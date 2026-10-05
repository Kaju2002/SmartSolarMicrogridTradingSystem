/*
 * File: ScanResultSheet.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: Bottom sheet after a QR scan, in three states. Verify: the booking the server
 *              found (station, time, NIC, name, kWh, cost) with Confirm transfer and Not now.
 *              Success: green with the completed booking. Error: red with the API's reason
 *              (invalid, already used, not approved, wrong day, offline).
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
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.launch

class ScanResultSheet : BottomSheetDialogFragment() {

    private var _binding: SheetScanResultBinding? = null
    private val binding get() = _binding!!

    private var completing = false

    // Inflate the layout
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetScanResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    // State and booking come in the arguments, so the sheet survives being recreated
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        val result = args.getString(ARG_RESULT)?.let { Gson().fromJson(it, ReservationResult::class.java) }

        when {
            result != null && args.getBoolean(ARG_VERIFIED) -> showVerified(result)
            result != null -> showSuccess(result)
            else -> showError(args.getString(ARG_ERROR) ?: getString(R.string.operator_error_scan))
        }
        binding.btnScanDismiss.setOnClickListener { dismiss() }
    }

    // Open fully so the buttons are visible without dragging
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

    // Booking found by the server: the operator checks it, then confirms or closes
    private fun showVerified(result: ReservationResult) {
        paintIcon(R.drawable.ic_qr_scan, R.color.booking_pending, R.color.booking_pending_bg)
        binding.tvResultTitle.setText(R.string.scan_verified_title)
        binding.tvResultMessage.setText(R.string.scan_verified_message)
        bindDetails(result, showName = true)

        binding.btnScanNext.setText(R.string.scan_confirm)
        binding.btnScanNext.setOnClickListener { confirm(result) }
        binding.btnScanDismiss.isVisible = true
    }

    // Green tick with station, slot time, NIC, kWh and cost of the booking just completed
    private fun showSuccess(result: ReservationResult) {
        paintIcon(R.drawable.ic_check, R.color.station_open, R.color.station_open_bg)
        binding.tvResultTitle.setText(R.string.scan_success_title)
        binding.tvResultMessage.setText(R.string.scan_success_message)
        bindDetails(result, showName = false)

        binding.btnScanNext.setText(R.string.scan_next)
        binding.btnScanNext.setOnClickListener { dismiss() }
        binding.btnScanDismiss.isVisible = false
    }

    // Red cross with the reason from the API or the connection check
    private fun showError(message: String) {
        paintIcon(R.drawable.ic_close, R.color.error_red, R.color.error_red_bg)
        binding.tvResultTitle.setText(R.string.scan_error_title)
        binding.tvResultMessage.text = message
        binding.layoutResultDetails.isVisible = false

        binding.btnScanNext.setText(R.string.scan_again)
        binding.btnScanNext.setOnClickListener { dismiss() }
        binding.btnScanDismiss.isVisible = false
    }

    // Finish the transfer; the API runs the same checks again in case anything changed
    private fun confirm(result: ReservationResult) {
        val qrCode = result.qrCode ?: requireArguments().getString(ARG_QR_CODE) ?: return
        if (completing) return
        setCompleting(true)

        viewLifecycleOwner.lifecycleScope.launch {
            OperatorManager.getInstance().scanQr(qrCode)
                .onSuccess { completed ->
                    setCompleting(false)
                    // Recreated sheet should show the finished state, not ask again
                    requireArguments().putString(ARG_RESULT, Gson().toJson(completed))
                    requireArguments().putBoolean(ARG_VERIFIED, false)
                    showSuccess(completed)
                }
                .onFailure { error ->
                    setCompleting(false)
                    requireArguments().remove(ARG_RESULT)
                    requireArguments().putString(ARG_ERROR, error.message ?: getString(R.string.operator_error_complete))
                    showError(error.message ?: getString(R.string.operator_error_complete))
                }
        }
    }

    // Lock the sheet while finishing so it can't be sent twice or closed half way
    private fun setCompleting(inProgress: Boolean) {
        completing = inProgress
        isCancelable = !inProgress
        binding.btnScanNext.isEnabled = !inProgress
        binding.btnScanDismiss.isEnabled = !inProgress
        if (inProgress) binding.btnScanNext.setText(R.string.scan_confirming)
    }

    // Station, status, time and fact tiles; the name tile only while verifying
    private fun bindDetails(result: ReservationResult, showName: Boolean) {
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
        val unknown = getString(R.string.station_value_unknown)
        binding.tvResultStation.text =
            result.stationName?.takeIf { it.isNotBlank() } ?: getString(R.string.station_unnamed)
        ReservationFormatter.bindStatus(binding.tvResultStatus, booking)
        binding.tvResultTime.text = ReservationFormatter.timeRange(requireContext(), booking)

        val kWh = result.requestedKWh ?: 0.0
        val hasKWh = kWh > 0
        bindInfo(binding.infoNic, R.drawable.ic_badge, R.string.scan_detail_nic,
            result.prosumerNic?.takeIf { it.isNotBlank() } ?: unknown)
        bindInfo(binding.infoName, R.drawable.ic_person, R.string.scan_detail_name,
            result.prosumerName?.takeIf { it.isNotBlank() } ?: unknown)
        binding.infoName.root.isVisible = showName
        binding.spaceName.isVisible = showName
        bindInfo(binding.infoEnergy, R.drawable.ic_bolt, R.string.booking_info_energy,
            if (hasKWh) getString(R.string.booking_kwh_value, ReservationFormatter.number(kWh))
            else getString(R.string.booking_kwh_unknown))
        bindInfo(binding.infoCost, R.drawable.ic_payments, R.string.booking_info_cost,
            if (hasKWh) getString(R.string.booking_cost_value, ReservationFormatter.number(result.estimatedCost ?: 0.0))
            else unknown)
        binding.layoutResultDetails.isVisible = true
    }

    // Icon and circle colours for each state
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
        private const val ARG_VERIFIED = "verified"
        private const val ARG_QR_CODE = "qr_code"
        private const val ARG_ERROR = "error"

        // Booking the server found for a QR; nothing is changed until Confirm transfer
        fun newVerified(result: ReservationResult, qrCode: String) = ScanResultSheet().apply {
            arguments = Bundle().apply {
                putString(ARG_RESULT, Gson().toJson(result))
                putBoolean(ARG_VERIFIED, true)
                putString(ARG_QR_CODE, qrCode)
            }
        }

        // Reason the scan failed
        fun newError(message: String) = ScanResultSheet().apply {
            arguments = Bundle().apply { putString(ARG_ERROR, message) }
        }
    }
}
