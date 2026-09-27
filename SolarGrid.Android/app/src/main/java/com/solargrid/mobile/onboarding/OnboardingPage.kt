/*
 * File: OnboardingPage.kt
 * Module: Onboarding (shared)
 * Description: Content of one onboarding page (photo, title, body).
 */
package com.solargrid.mobile.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class OnboardingPage(
    @param:DrawableRes val imageRes: Int,
    @param:StringRes val titleRes: Int,
    @param:StringRes val bodyRes: Int
)
