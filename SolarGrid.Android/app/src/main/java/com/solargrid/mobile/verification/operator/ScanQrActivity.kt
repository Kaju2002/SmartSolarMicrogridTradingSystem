/*
 * File: ScanQrActivity.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: Grid Operator QR scanner. Reads a prosumer's booking QR with the camera
 *              (or a typed code), sends it to the API to complete the energy transfer and
 *              shows the result sheet. Asks for camera access and offers a flash toggle.
 */
package com.solargrid.mobile.verification.operator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.EditorInfo
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.solargrid.mobile.R
import com.solargrid.mobile.databinding.ActivityScanQrBinding
import com.solargrid.mobile.databinding.DialogManualQrBinding
import kotlinx.coroutines.launch

class ScanQrActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanQrBinding

    private var verifying = false
    private var torchOn = false
    // True after "don't ask again"; the button then opens the app settings
    private var cameraBlocked = false
    private var manualDialog: AlertDialog? = null
    // Result that arrived while the screen was in the background
    private var pendingSheet: ScanResultSheet? = null

    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startScanning()
            } else {
                showPermissionNeeded(
                    blocked = !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
                )
            }
        }

    // One code per scan; the camera waits until the result sheet is closed
    private val scanCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult) {
            val code = result.text
            if (!code.isNullOrBlank()) verify(code)
        }

        override fun possibleResultPoints(resultPoints: List<ResultPoint>) = Unit
    }

    // Camera setup, buttons and the first permission check
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityScanQrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        setupScanner()
        setupButtons()
        supportFragmentManager.setFragmentResultListener(ScanResultSheet.REQUEST_SCAN_NEXT, this) { _, _ ->
            if (canScan()) resumeCamera()
        }

        when {
            hasCameraPermission() -> startScanning()
            savedInstanceState == null -> cameraPermission.launch(Manifest.permission.CAMERA)
            else -> showPermissionNeeded(blocked = false)
        }
    }

    // Back from the settings screen with access granted, or back from another app
    override fun onResume() {
        super.onResume()
        if (!hasCameraPermission()) return
        if (binding.layoutPermission.isVisible) {
            startScanning()
        } else if (canScan() && !isResultShowing()) {
            resumeCamera()
        }
    }

    // A result that came back while in the background is shown now
    override fun onPostResume() {
        super.onPostResume()
        pendingSheet?.let { sheet ->
            pendingSheet = null
            sheet.show(supportFragmentManager, ScanResultSheet.TAG)
        }
    }

    // Free the camera for other apps
    override fun onPause() {
        super.onPause()
        binding.barcodeView.pause()
    }

    // Top bar below the status bar, bottom panel above the navigation bar
    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.layoutScanTop.updatePadding(top = bars.top)
            binding.layoutScanBottom.updatePadding(bottom = bars.bottom + resources.getDimensionPixelSize(R.dimen.space_md))
            binding.layoutPermission.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
    }

    // QR codes only, no library status text, flash button only when the phone has one
    private fun setupScanner() {
        binding.barcodeView.barcodeView.decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
        binding.barcodeView.setStatusText("")
        binding.barcodeView.setTorchListener(object : DecoratedBarcodeView.TorchListener {
            override fun onTorchOn() = updateTorch(true)
            override fun onTorchOff() = updateTorch(false)
        })
        binding.btnTorch.isVisible = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }

    // Back, flash, manual entry and the permission buttons
    private fun setupButtons() {
        binding.btnScanBack.setOnClickListener { finish() }
        binding.btnPermissionBack.setOnClickListener { finish() }
        binding.btnTorch.setOnClickListener {
            if (torchOn) binding.barcodeView.setTorchOff() else binding.barcodeView.setTorchOn()
        }
        binding.btnManualEntry.setOnClickListener { showManualEntry() }
        binding.btnPermissionManual.setOnClickListener { showManualEntry() }
        binding.btnAllowCamera.setOnClickListener {
            if (cameraBlocked) openAppSettings() else cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    // Flash icon and its spoken label follow the real torch state
    private fun updateTorch(on: Boolean) {
        torchOn = on
        binding.btnTorch.alpha = if (on) 1f else FLASH_OFF_ALPHA
        binding.btnTorch.contentDescription = getString(if (on) R.string.scan_torch_off else R.string.scan_torch_on)
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    // Nothing else on screen needs the operator's attention
    private fun canScan(): Boolean =
        hasCameraPermission() && !verifying && pendingSheet == null && manualDialog?.isShowing != true

    private fun isResultShowing(): Boolean =
        supportFragmentManager.findFragmentByTag(ScanResultSheet.TAG) != null

    // Camera view with its top and bottom panels
    private fun startScanning() {
        binding.layoutPermission.isVisible = false
        binding.layoutScanTop.isVisible = true
        binding.layoutScanBottom.isVisible = true
        updateTorch(torchOn)
        if (canScan() && !isResultShowing()) resumeCamera()
    }

    private fun resumeCamera() {
        binding.barcodeView.resume()
        binding.barcodeView.decodeSingle(scanCallback)
    }

    // Explain why the camera is needed; after "don't ask again" the button opens settings
    private fun showPermissionNeeded(blocked: Boolean) {
        cameraBlocked = blocked
        binding.layoutPermission.isVisible = true
        binding.layoutScanTop.isVisible = false
        binding.layoutScanBottom.isVisible = false
        binding.btnAllowCamera.setText(
            if (blocked) R.string.scan_permission_settings else R.string.scan_permission_allow
        )
    }

    private fun openAppSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        )
    }

    // Typed code for a damaged screen or a camera that can't focus
    private fun showManualEntry() {
        if (verifying || manualDialog?.isShowing == true) return
        binding.barcodeView.pause()

        val input = DialogManualQrBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.scan_manual_title)
            .setView(input.root)
            .setNegativeButton(R.string.scan_manual_cancel, null)
            .setPositiveButton(R.string.scan_manual_verify, null)
            .setOnDismissListener {
                manualDialog = null
                if (canScan() && !isResultShowing()) resumeCamera()
            }
            .show()
        manualDialog = dialog

        // Set after show() so an empty field keeps the dialog open
        val submit = {
            val code = input.etManualCode.text?.toString()?.trim().orEmpty()
            if (code.isEmpty()) {
                input.tilManualCode.error = getString(R.string.scan_manual_empty)
            } else {
                verify(code)
                dialog.dismiss()
            }
        }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { submit() }
        input.etManualCode.doAfterTextChanged { input.tilManualCode.error = null }
        input.etManualCode.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                true
            } else {
                false
            }
        }
        input.etManualCode.requestFocus()
    }

    // Send the code to the API once; the sheet shows the completed booking or the reason
    private fun verify(code: String) {
        if (verifying) return
        verifying = true
        binding.barcodeView.pause()
        binding.layoutVerifying.isVisible = true

        lifecycleScope.launch {
            val result = OperatorManager.getInstance().scanQr(code)
            verifying = false
            binding.layoutVerifying.isVisible = false

            val sheet = result.fold(
                onSuccess = { ScanResultSheet.newSuccess(it) },
                onFailure = { ScanResultSheet.newError(it.message ?: getString(R.string.operator_error_scan)) }
            )
            if (supportFragmentManager.isStateSaved) {
                pendingSheet = sheet
            } else {
                sheet.show(supportFragmentManager, ScanResultSheet.TAG)
            }
        }
    }

    companion object {
        private const val FLASH_OFF_ALPHA = 0.6f
    }
}
