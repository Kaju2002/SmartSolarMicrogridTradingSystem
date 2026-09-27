/*
 * File: StationsFragment.kt
 * Module: Station Management (Gabilan)
 * Description: Stations tab. Finds nearby stations (Colombo when the phone's location is not
 *              available) and shows them as map pins with a swipeable card per station.
 */
package com.solargrid.mobile.station

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.solargrid.mobile.R
import com.solargrid.mobile.core.managers.DeviceLocationManager
import com.solargrid.mobile.databinding.FragmentStationsBinding
import com.solargrid.mobile.station.models.Station
import kotlinx.coroutines.launch

class StationsFragment : Fragment(R.layout.fragment_stations) {

    private var _binding: FragmentStationsBinding? = null
    private val binding get() = _binding!!

    private val locationManager = DeviceLocationManager.getInstance()

    // True once the user said no in the permission dialog
    private var permissionDenied = false

    // Reload when the user comes back from the settings screen
    private var reloadOnResume = false

    // Loaded stations and where we searched from (null when we fell back to Colombo)
    private var stations: List<Station> = emptyList()
    private var userLocation: LatLng? = null
    private var selectedIndex = RecyclerView.NO_POSITION

    // Map objects live only while the view exists
    private var googleMap: GoogleMap? = null
    private val markers = mutableListOf<Marker>()
    private var pinIcon: BitmapDescriptor? = null
    private var selectedPinIcon: BitmapDescriptor? = null

    private val cardAdapter = StationCardAdapter { position ->
        selectStation(position, scrollCards = true)
        openDetail(position)
    }
    private val snapHelper = PagerSnapHelper()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            permissionDenied = result.values.none { it }
            loadStations()
        }

    // Bind views, then ask for location the first time the tab opens
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStationsBinding.bind(view)
        setupListeners()
        setupCards()
        setupMap()
        listenForBooking()

        if (locationManager.hasLocationPermission() || savedInstanceState != null) {
            loadStations()
        } else {
            permissionLauncher.launch(DeviceLocationManager.LOCATION_PERMISSIONS)
        }
    }

    // Pick up a permission or location change made in Settings
    override fun onResume() {
        super.onResume()
        if (reloadOnResume) {
            reloadOnResume = false
            loadStations()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        snapHelper.attachToRecyclerView(null)
        markers.clear()
        googleMap = null
        pinIcon = null
        selectedPinIcon = null
        _binding = null
    }

    private fun setupListeners() {
        binding.btnUseLocation.setOnClickListener { onUseLocationClicked() }
        binding.btnRetry.setOnClickListener { loadStations() }
    }

    // Horizontal cards that snap one at a time; the snapped card becomes the selected station
    private fun setupCards() {
        binding.rvStations.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvStations.adapter = cardAdapter
        snapHelper.attachToRecyclerView(binding.rvStations)

        binding.rvStations.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) return
                val layoutManager = recyclerView.layoutManager ?: return
                val snapped = snapHelper.findSnapView(layoutManager) ?: return
                selectStation(layoutManager.getPosition(snapped), scrollCards = false)
            }
        })
    }

    // Clean map style, no Google buttons; tapping a pin selects its station
    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapStations) as SupportMapFragment
        mapFragment.getMapAsync { map ->
            if (_binding == null) return@getMapAsync
            googleMap = map
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style))
            map.uiSettings.isMapToolbarEnabled = false
            map.uiSettings.isCompassEnabled = false
            map.uiSettings.isMyLocationButtonEnabled = false
            // First tap selects a pin, a second tap on the same pin opens its details
            map.setOnMarkerClickListener { marker ->
                (marker.tag as? Int)?.let { index ->
                    if (index == selectedIndex) openDetail(index) else selectStation(index, scrollCards = true)
                }
                true
            }
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(StationManager.CITY_LATITUDE, StationManager.CITY_LONGITUDE), CITY_ZOOM
                )
            )
            pinIcon = pinFrom(R.drawable.ic_map_pin)
            selectedPinIcon = pinFrom(R.drawable.ic_map_pin_selected)
            renderMap()
        }
    }

    // "Book a slot" in the detail sheet
    private fun listenForBooking() {
        childFragmentManager.setFragmentResultListener(
            StationDetailSheet.REQUEST_BOOK_SLOT, viewLifecycleOwner
        ) { _, result ->
            result.getString(StationDetailSheet.KEY_STATION_ID)?.let { openBooking(it) }
        }
    }

    // Booking screen is added with the Reservation module
    @Suppress("UNUSED_PARAMETER")
    private fun openBooking(stationId: String) {
        Toast.makeText(requireContext(), R.string.station_book_coming, Toast.LENGTH_SHORT).show()
    }

    // Detail sheet for one station; ignores double taps while it is already open
    private fun openDetail(index: Int) {
        val stationId = stations.getOrNull(index)?.stationId ?: return
        if (childFragmentManager.findFragmentByTag(StationDetailSheet.TAG) != null) return
        StationDetailSheet.newInstance(stationId).show(childFragmentManager, StationDetailSheet.TAG)
    }

    // Search around the phone, or around Colombo with a wider radius when that fails
    private fun loadStations() {
        showLoading()
        viewLifecycleOwner.lifecycleScope.launch {
            val myLocation = locationManager.getCurrentLocation()
            val center = myLocation ?: LatLng(StationManager.CITY_LATITUDE, StationManager.CITY_LONGITUDE)
            val radiusKm = if (myLocation != null) StationManager.DEFAULT_RADIUS_KM else StationManager.CITY_RADIUS_KM

            StationManager.getInstance()
                .getNearbyStations(center.latitude, center.longitude, radiusKm)
                .onSuccess { showStations(it, myLocation, radiusKm.toInt()) }
                .onFailure { showError(it.message ?: getString(R.string.station_error_load)) }
        }
    }

    // Ask again, or send the user to Settings when Android won't show the dialog any more
    private fun onUseLocationClicked() {
        val canAskAgain = DeviceLocationManager.LOCATION_PERMISSIONS.any {
            shouldShowRequestPermissionRationale(it)
        }
        when {
            locationManager.hasLocationPermission() -> openSettings(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            !permissionDenied || canAskAgain ->
                permissionLauncher.launch(DeviceLocationManager.LOCATION_PERMISSIONS)
            else -> openSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        }
    }

    // Open a Settings screen and reload when we're back
    private fun openSettings(action: String) {
        val intent = Intent(action)
        if (action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
            intent.data = Uri.fromParts("package", requireContext().packageName, null)
        }
        reloadOnResume = true
        startActivity(intent)
    }

    private fun showLoading() {
        binding.cardSearchArea.isVisible = true
        binding.tvSearchArea.setText(R.string.stations_finding_location)
        binding.tvSearchAreaHint.isVisible = false
        binding.btnUseLocation.isVisible = false
        binding.rvStations.isVisible = false
        binding.progressStations.isVisible = true
        binding.layoutStationsEmpty.isVisible = false
    }

    // Update the search card, then show pins and cards (or the empty state)
    private fun showStations(found: List<Station>, myLocation: LatLng?, radiusKm: Int) {
        val nearMe = myLocation != null
        stations = found
        userLocation = myLocation
        selectedIndex = if (found.isEmpty()) RecyclerView.NO_POSITION else 0
        binding.progressStations.isVisible = false

        binding.tvSearchArea.text = if (nearMe) {
            resources.getQuantityString(R.plurals.stations_count_near_you, found.size, found.size, radiusKm)
        } else {
            resources.getQuantityString(R.plurals.stations_count_near_city, found.size, found.size)
        }
        binding.tvSearchAreaHint.isVisible = !nearMe
        binding.btnUseLocation.isVisible = !nearMe
        if (!nearMe) {
            binding.tvSearchAreaHint.setText(
                if (locationManager.hasLocationPermission()) R.string.stations_location_unknown
                else R.string.stations_location_off
            )
        }

        binding.layoutStationsEmpty.isVisible = found.isEmpty()
        binding.btnRetry.isVisible = false
        binding.tvStationsEmpty.text = if (nearMe) {
            getString(R.string.stations_empty_near_you, radiusKm)
        } else {
            getString(R.string.stations_empty_near_city)
        }

        binding.rvStations.isVisible = found.isNotEmpty()
        cardAdapter.submit(found, selectedIndex)
        binding.rvStations.scrollToPosition(0)
        renderMap()
    }

    // Load failed: hide the search card and offer a retry
    private fun showError(message: String) {
        stations = emptyList()
        binding.cardSearchArea.isVisible = false
        binding.progressStations.isVisible = false
        binding.rvStations.isVisible = false
        binding.layoutStationsEmpty.isVisible = true
        binding.tvStationsEmpty.text = message
        binding.btnRetry.isVisible = true
    }

    // Redraw the pins and fit the camera; waits until both the map and the stations are ready
    private fun renderMap() {
        val map = googleMap ?: return
        markers.forEach { it.remove() }
        markers.clear()

        stations.forEachIndexed { index, station ->
            val selected = index == selectedIndex
            val marker = map.addMarker(
                MarkerOptions()
                    .position(station.position())
                    .title(station.stationName)
                    .icon(if (selected) selectedPinIcon else pinIcon)
                    .zIndex(if (selected) 1f else 0f)
            )
            marker?.tag = index
            marker?.let { markers += it }
        }
        showMyLocationDot(map)

        // Cards cover the bottom of the map, so keep pins and the Google logo above them
        binding.rvStations.post {
            if (_binding == null) return@post
            val cardsHeight = if (binding.rvStations.isVisible) binding.rvStations.height else 0
            map.setPadding(0, 0, 0, cardsHeight)
            fitCamera(map)
        }
    }

    // Show every station (and the user) on screen
    private fun fitCamera(map: GoogleMap) {
        val points = stations.map { it.position() } + listOfNotNull(userLocation)
        when (points.size) {
            0 -> return
            1 -> map.animateCamera(CameraUpdateFactory.newLatLngZoom(points[0], SINGLE_STATION_ZOOM))
            else -> {
                val bounds = LatLngBounds.builder().apply { points.forEach { include(it) } }.build()
                val padding = resources.getDimensionPixelSize(R.dimen.screen_padding) * 2
                val width = binding.layoutMap.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
                val height = binding.layoutMap.height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding))
            }
        }
    }

    // Blue dot for the user, only when location is allowed
    @SuppressLint("MissingPermission")
    private fun showMyLocationDot(map: GoogleMap) {
        try {
            map.isMyLocationEnabled = locationManager.hasLocationPermission()
        } catch (e: SecurityException) {
            map.isMyLocationEnabled = false
        }
    }

    // Highlight one station on both the map and the cards
    private fun selectStation(index: Int, scrollCards: Boolean) {
        if (index !in stations.indices) return
        val previous = selectedIndex
        selectedIndex = index
        cardAdapter.setSelected(index)

        markers.getOrNull(previous)?.apply {
            setIcon(pinIcon)
            zIndex = 0f
        }
        markers.getOrNull(index)?.apply {
            setIcon(selectedPinIcon)
            zIndex = 1f
        }
        if (previous != index) {
            googleMap?.animateCamera(CameraUpdateFactory.newLatLng(stations[index].position()))
        }
        if (scrollCards) scrollCardsTo(index)
    }

    // Smooth scroll so the card lines up with the carousel padding, same place the snap puts it
    private fun scrollCardsTo(index: Int) {
        val scroller = object : LinearSmoothScroller(requireContext()) {
            override fun getHorizontalSnapPreference(): Int = SNAP_TO_START
        }
        scroller.targetPosition = index
        binding.rvStations.layoutManager?.startSmoothScroll(scroller)
    }

    // Vector pin drawn into a bitmap for the map
    private fun pinFrom(drawableRes: Int): BitmapDescriptor? {
        val drawable = ContextCompat.getDrawable(requireContext(), drawableRes) ?: return null
        val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.setBounds(0, 0, bitmap.width, bitmap.height)
        drawable.draw(Canvas(bitmap))
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // StationManager already dropped stations without a position
    private fun Station.position() = LatLng(latitude ?: 0.0, longitude ?: 0.0)

    companion object {
        private const val CITY_ZOOM = 11f
        private const val SINGLE_STATION_ZOOM = 14f
    }
}
