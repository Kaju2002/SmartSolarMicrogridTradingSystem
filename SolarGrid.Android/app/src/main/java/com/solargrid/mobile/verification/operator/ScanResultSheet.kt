/*
 * File: ScanResultSheet.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: Bottom sheet after a QR scan. Green with the completed booking's details,
 *              or red with the API's reason (invalid, already used, not approved, offline).
 *              Closing it tells the scanner to look for the next code.
 */
package com.solargrid.mobile.verification.operator

import android.content.DialogInterface
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.solargrid.mobile.R
import com.solargrid.mobile.databinding.ItemStationInfoBinding
import com.solargrid.mobile.databinding.SheetScanResultBinding
import com.solargrid.mobile.reservation.ReservationFormatter
import com.solargrid.mobile.reservation.models.Reservation
import com.solargrid.mobile.reservation.models.ReservationResult

class ScanResultSheet : BottomSheetDialogFragment() {

    private var _binding: SheetScanResultBinding? = null
    private val binding get() = _binding!!

    // Inflate the layout
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetScanResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Result comes in the arguments, so the sheet survives being recreated
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val result = requireArguments().getString(ARG_RESULT)
            ?.let { Gson().fromJson(it, ReservationResult::class.java) }

        if (result != null) {
            showSuccess(result)
        } else {
            showError(requireArguments().getString(ARG_ERROR) ?: getString(R.string.operator_error_scan))
        }
        binding.btnScanNext.setOnClickListener { dismiss() }
    }

    // Open fully so the button is visible without dragging
    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    // Button, swipe or Back: the scanner starts looking again
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        parentFragmentManager.setFragmentResult(REQUEST_SCAN_NEXT, Bundle.EMPTY)
    }

    // Drop the view reference so the old layout can be freed
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Green tick with station, slot time, NIC, kWh and cost of the booking just completed
    private fun showSuccess(result: ReservationResult) {
        paintIcon(R.drawable.ic_check, R.color.station_open, R.color.station_open_bg)
        binding.tvResultTitle.setText(R.string.scan_success_title)
        binding.tvResultMessage.setText(R.string.scan_success_message)
        binding.btnScanNext.setText(R.string.scan_next)

        val booking = Reservation(
            id = result.reservationId,
            stationId = null,
            reservationDateTime = result.reservationDateTime,
            requestedKWh = result.requestedKWh,
            estimatedCost = result.estimatedCost,
            status = result.status,
            qrCode = null,
            createdAt = null
        )
        binding.tvResultStation.text =
            result.stationName?.takeIf { it.isNotBlank() } ?: getString(R.string.station_unnamed)
        ReservationFormatter.bindStatus(binding.tvResultStatus, booking)
        binding.tvResultTime.text = ReservationFormatter.timeRange(requireContext(), booking)

        val kWh = result.requestedKWh ?: 0.0
        val hasKWh = kWh > 0
        bindInfo(binding.infoNic, R.drawable.ic_badge, R.string.scan_detail_nic,
            result.prosumerNic?.takeIf { it.isNotBlank() } ?: getString(R.string.station_value_unknown))
        bindInfo(binding.infoEnergy, R.drawable.ic_bolt, R.string.booking_info_energy,
            if (hasKWh) getString(R.string.booking_kwh_value, ReservationFormatter.number(kWh))
            else getString(R.string.booking_kwh_unknown))
        bindInfo(binding.infoCost, R.drawable.ic_payments, R.string.booking_info_cost,
            if (hasKWh) getString(R.string.booking_cost_value, ReservationFormatter.number(result.estimatedCost ?: 0.0))
            else getString(R.string.station_value_unknown))
        binding.layoutResultDetails.isVisible = true
    }

    // Red cross with the reason from the API or the connection check
    private fun showError(message: String) {
        paintIcon(R.drawable.ic_close, R.color.error_red, R.color.error_red_bg)
        binding.tvResultTitle.setText(R.string.scan_error_title)
        binding.tvResultMessage.text = message
        binding.btnScanNext.setText(R.string.scan_again)
        binding.layoutResultDetails.isVisible = false
    }

    // Icon and circle colours for success or failure
    private fun paintIcon(@DrawableRes icon: Int, @ColorRes tint: Int, @ColorRes background: Int) {
        val context = requireContext()
        binding.ivResultIcon.setImageResource(icon)
        binding.ivResultIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, tint))
        binding.ivResultIcon.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, background))
    }

    // Icon, label and value of one fact tile
    private fun bindInfo(tile: ItemStationInfoBinding, @DrawableRes icon: Int, @StringRes label: Int, value: String) {
        tile.ivInfoIcon.setImageResource(icon)
        tile.tvInfoLabel.setText(label)
        tile.tvInfoValue.text = value
    }

    companion object {
        const val TAG = "scan_result"
        const val REQUEST_SCAN_NEXT = "scan_next"
        private const val ARG_RESULT = "result"
        private const val ARG_ERROR = "error"

        // Completed booking returned by the API
        fun newSuccess(result: ReservationResult) = ScanResultSheet().apply {
            arguments = Bundle().apply { putString(ARG_RESULT, Gson().toJson(result)) }
        }

        // Reason the scan failed
        fun newError(message: String) = ScanResultSheet().apply {
            arguments = Bundle().apply { putString(ARG_ERROR, message) }
        }
    }
}
