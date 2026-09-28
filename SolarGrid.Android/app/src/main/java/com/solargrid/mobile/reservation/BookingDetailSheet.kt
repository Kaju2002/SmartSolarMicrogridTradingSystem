/*
 * File: BookingDetailSheet.kt
 * Module: Reservation Management (Kajanthan)
 * Description: Bottom sheet for one booking. Shows time, kWh and cost, the QR code once a Grid
 *              Operator has approved it, and Change time / Cancel while the slot is more than
 *              12 hours away. The API checks the same rule again. The QR also works offline
 *              (drawn on the phone from the saved code); change and cancel need the API.
 */
package com.solargrid.mobile.reservation

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.solargrid.mobile.R
import com.solargrid.mobile.databinding.ItemStationInfoBinding
import com.solargrid.mobile.databinding.SheetBookingDetailBinding
import com.solargrid.mobile.reservation.models.Reservation
import kotlinx.coroutines.launch

class BookingDetailSheet : BottomSheetDialogFragment() {

    private var _binding: SheetBookingDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var booking: Reservation
    private var cancelling = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetBookingDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    // The booking comes in the arguments, so the sheet survives rotation without a reload
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val json = requireArguments().getString(ARG_BOOKING)
        booking = json?.let { Gson().fromJson(it, Reservation::class.java) } ?: run {
            dismiss()
            return
        }

        binding.btnCancelBooking.setOnClickListener { confirmCancel() }
        binding.btnChangeTime.setOnClickListener { changeTime() }
        showBooking()
    }

    // Open fully; the QR must be visible without dragging
    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showBooking() {
        val context = requireContext()
        binding.tvDetailStation.text =
            requireArguments().getString(ARG_STATION_NAME) ?: getString(R.string.station_unnamed)
        ReservationFormatter.bindStatus(binding.tvDetailStatus, booking)
        binding.tvDetailTime.text = ReservationFormatter.timeRange(context, booking)

        val kWh = booking.requestedKWh ?: 0.0
        val hasKWh = kWh > 0
        bindInfo(binding.infoEnergy, R.drawable.ic_bolt, R.string.booking_info_energy,
            if (hasKWh) getString(R.string.booking_kwh_value, ReservationFormatter.number(kWh))
            else getString(R.string.booking_kwh_unknown))
        bindInfo(binding.infoCost, R.drawable.ic_payments, R.string.booking_info_cost,
            if (hasKWh) getString(R.string.booking_cost_value, ReservationFormatter.number(booking.estimatedCost ?: 0.0))
            else getString(R.string.station_value_unknown))

        showQr()
        showNote()
        showActions()
    }

    // QR from the code the API made at approval; full brightness helps the operator's scanner
    private fun showQr() {
        val code = booking.qrCode
        val show = booking.status == Reservation.STATUS_APPROVED &&
            !code.isNullOrBlank() && !ReservationFormatter.isPast(booking)
        val bitmap = if (show) qrBitmap(code!!) else null

        binding.layoutQr.isVisible = bitmap != null
        if (bitmap != null) {
            binding.ivQr.setImageBitmap(bitmap)
            dialog?.window?.let { window ->
                window.attributes = window.attributes.apply {
                    screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                }
            }
        }
    }

    private fun qrBitmap(content: String): Bitmap? = try {
        BarcodeEncoder().encodeBitmap(
            content, BarcodeFormat.QR_CODE, QR_SIZE_PX, QR_SIZE_PX, mapOf(EncodeHintType.MARGIN to 1)
        )
    } catch (e: WriterException) {
        null
    }

    // One line about where the booking stands
    private fun showNote() {
        val note = when {
            booking.isLive() && ReservationFormatter.isPast(booking) -> R.string.booking_note_expired
            booking.status == Reservation.STATUS_PENDING -> R.string.booking_note_pending
            booking.status == Reservation.STATUS_COMPLETED -> R.string.booking_note_completed
            booking.status == Reservation.STATUS_CANCELLED -> R.string.booking_note_cancelled
            else -> null
        }
        binding.tvDetailNote.isVisible = note != null
        if (note != null) binding.tvDetailNote.setText(note)
    }

    // Buttons only while changes are open and the API can be reached; otherwise explain why
    private fun showActions() {
        val upcoming = booking.isLive() && !ReservationFormatter.isPast(booking)
        val canChange = ReservationFormatter.canChange(booking)
        val offline = requireArguments().getBoolean(ARG_OFFLINE)
        val deadline = ReservationFormatter.changeDeadline(booking)

        binding.tvDetailRule.isVisible = upcoming
        binding.tvDetailRule.text = when {
            canChange && offline -> getString(R.string.booking_offline_actions)
            canChange && deadline != null -> getString(
                R.string.booking_change_until, ReservationTime.dayLabel(deadline), ReservationTime.clock(deadline)
            )
            else -> getString(R.string.booking_change_closed)
        }
        binding.layoutActions.isVisible = upcoming && canChange && !offline
    }

    private fun bindInfo(tile: ItemStationInfoBinding, @DrawableRes icon: Int, @StringRes label: Int, value: String) {
        tile.ivInfoIcon.setImageResource(icon)
        tile.tvInfoLabel.setText(label)
        tile.tvInfoValue.text = value
    }

    // Ask first; cancelling can't be undone
    private fun confirmCancel() {
        if (cancelling) return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.booking_cancel_title)
            .setMessage(getString(R.string.booking_cancel_message,
                binding.tvDetailStation.text, binding.tvDetailTime.text))
            .setNegativeButton(R.string.booking_cancel_keep, null)
            .setPositiveButton(R.string.booking_cancel_confirm) { _, _ -> cancelBooking() }
            .show()
    }

    // On success the Bookings tab reloads; on a rule error (e.g. under 12 hours) show the API's message
    private fun cancelBooking() {
        val id = booking.id ?: return
        setCancelling(true)
        viewLifecycleOwner.lifecycleScope.launch {
            ReservationManager.getInstance().cancelReservation(id)
                .onSuccess {
                    sendResult(Bundle().apply { putString(KEY_MESSAGE, getString(R.string.booking_cancelled_done)) })
                    dismiss()
                }
                .onFailure { error ->
                    setCancelling(false)
                    Toast.makeText(requireContext(), error.message, Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun setCancelling(inProgress: Boolean) {
        cancelling = inProgress
        isCancelable = !inProgress
        binding.btnCancelBooking.isEnabled = !inProgress
        binding.btnChangeTime.isEnabled = !inProgress
        binding.btnCancelBooking.setText(if (inProgress) R.string.booking_cancelling else R.string.booking_cancel)
    }

    // The Bookings tab opens the booking screen in change mode
    private fun changeTime() {
        sendResult(Bundle().apply { putString(KEY_CHANGE_BOOKING, Gson().toJson(booking)) })
        dismiss()
    }

    private fun sendResult(result: Bundle) {
        parentFragmentManager.setFragmentResult(REQUEST_BOOKING, result)
    }

    companion object {
        const val TAG = "booking_detail"
        const val REQUEST_BOOKING = "booking_detail_result"
        const val KEY_MESSAGE = "message"
        const val KEY_CHANGE_BOOKING = "change_booking"
        private const val ARG_BOOKING = "booking"
        private const val ARG_STATION_NAME = "station_name"
        private const val ARG_OFFLINE = "offline"
        private const val QR_SIZE_PX = 600

        // offline = the booking came from the saved copy, so change/cancel are hidden
        fun newInstance(booking: Reservation, stationName: String?, offline: Boolean) = BookingDetailSheet().apply {
            arguments = Bundle().apply {
                putString(ARG_BOOKING, Gson().toJson(booking))
                putString(ARG_STATION_NAME, stationName)
                putBoolean(ARG_OFFLINE, offline)
            }
        }
    }
}
