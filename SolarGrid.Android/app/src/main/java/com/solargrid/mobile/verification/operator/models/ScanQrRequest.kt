/*
 * File: ScanQrRequest.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: Body for POST api/verification/scan-qr. Matches VerifyQrDto in SolarGrid.API.
 */
package com.solargrid.mobile.verification.operator.models

import com.google.gson.annotations.SerializedName

data class ScanQrRequest(
    @SerializedName("qrCode") val qrCode: String
)
