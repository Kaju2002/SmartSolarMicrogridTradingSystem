/*
 * File: OperatorBookingSheet.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: Bottom sheet for one booking at the operator's station. Shows the prosumer NIC,
 *              time, kWh and cost, and an Approve button while the booking is Pending and its
 *              hour hasn't passed. The API checks the same rules and makes the QR code.
 */
package com.solargrid.mobile.verification.operator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.solargrid.mobile.R
import com.solargrid.mobile.databinding.ItemStationInfoBinding
import com.solargrid.mobile.databinding.SheetOperatorBookingBinding
import com.solargrid.mobile.reservation.ReservationFormatter
import com.solargrid.mobile.reservation.models.Reservation
import com.solargrid.mobile.verification.operator.models.OperatorBooking
import kotlinx.coroutines.launch

class OperatorBookingSheet : BottomSheetDialogFragment() {

    private var _binding: SheetOperatorBookingBinding? = null
    private val binding get() = _binding!!

    private lateinit var booking: OperatorBooking
    private var approving = false

    // Inflate the layout
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetOperatorBookingBinding.inflate(inflater, container, false)
        return binding.root
    }

    // The booking comes in the arguments, so the sheet survives rotation without a reload
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        booking = requireArguments().getString(ARG_BOOKING)
            ?.let { Gson().fromJson(it, OperatorBooking::class.java) } ?: run {
            dismiss()
            return
        }
        binding.btnSheetApprove.setOnClickListener { approve() }
        showBooking()
    }

    // Open fully so the Approve button is visible without dragging
    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    // Drop the view reference so the old layout can be freed
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Header, fact tiles, note and the button
    private fun showBooking() {
        val context = requireContext()
        val reservation = booking.toReservation()
        val unknown = getString(R.string.station_value_unknown)

        binding.tvSheetStation.text =
            booking.stationName?.takeIf { it.isNotBlank() } ?: getString(R.string.station_unnamed)
        ReservationFormatter.bindStatus(binding.tvSheetStatus, reservation)
        binding.tvSheetTime.text = ReservationFormatter.timeRange(context, reservation)

        val kWh = booking.requestedKWh ?: 0.0
        val hasKWh = kWh > 0
        bindInfo(binding.infoSheetNic, R.drawable.ic_badge, R.string.scan_detail_nic,
            booking.prosumerNic?.takeIf { it.isNotBlank() } ?: unknown)
        bindInfo(binding.infoSheetEnergy, R.drawable.ic_bolt, R.string.booking_info_energy,
            if (hasKWh) getString(R.string.booking_kwh_value, ReservationFormatter.number(kWh))
            else getString(R.string.booking_kwh_unknown))
        bindInfo(binding.infoSheetCost, R.drawable.ic_payments, R.string.booking_info_cost,
            if (hasKWh) getString(R.string.booking_cost_value, ReservationFormatter.number(booking.estimatedCost ?: 0.0))
            else unknown)

        binding.tvSheetNote.setText(noteFor(reservation))
        binding.btnSheetApprove.isVisible = OperatorBookingFormatter.canApprove(booking)
    }

    // One line about what the operator can do with this booking
    @StringRes
    private fun noteFor(reservation: Reservation): Int = when {
        reservation.isLive() && ReservationFormatter.isPast(reservation) -> R.string.operator_note_expired
        reservation.status == Reservation.STATUS_PENDING -> R.string.operator_note_pending
        reservation.status == Reservation.STATUS_APPROVED -> R.string.operator_note_approved
        reservation.status == Reservation.STATUS_COMPLETED -> R.string.operator_note_completed
        else -> R.string.operator_note_cancelled
    }

    // On success the list reloads; a rule error (e.g. slot passed) stays in the sheet
    private fun approve() {
        val id = booking.id ?: return
        if (approving) return
        setApproving(true)
        binding.tvSheetError.isVisible = false

        viewLifecycleOwner.lifecycleScope.launch {
            OperatorManager.getInstance().approve(id)
                .onSuccess {
                    parentFragmentManager.setFragmentResult(REQUEST_APPROVED, Bundle.EMPTY)
                    dismiss()
                }
                .onFailure { error ->
                    setApproving(false)
                    binding.tvSheetError.text = error.message ?: getString(R.string.operator_error_approve)
                    binding.tvSheetError.isVisible = true
                }
        }
    }

    // Lock the sheet while the call runs so it can't be approved twice
    private fun setApproving(inProgress: Boolean) {
        approving = inProgress
        isCancelable = !inProgress
        binding.btnSheetApprove.isEnabled = !inProgress
        binding.btnSheetApprove.setText(if (inProgress) R.string.operator_approving else R.string.operator_approve)
    }

    // Icon, label and value of one fact tile
    private fun bindInfo(tile: ItemStationInfoBinding, @DrawableRes icon: Int, @StringRes label: Int, value: String) {
        tile.ivInfoIcon.setImageResource(icon)
        tile.tvInfoLabel.setText(label)
        tile.tvInfoValue.text = value
    }

    companion object {
        const val TAG = "operator_booking"
        const val REQUEST_APPROVED = "operator_booking_approved"
        private const val ARG_BOOKING = "booking"

        fun newInstance(booking: OperatorBooking) = OperatorBookingSheet().apply {
            arguments = Bundle().apply { putString(ARG_BOOKING, Gson().toJson(booking)) }
        }
    }
}
