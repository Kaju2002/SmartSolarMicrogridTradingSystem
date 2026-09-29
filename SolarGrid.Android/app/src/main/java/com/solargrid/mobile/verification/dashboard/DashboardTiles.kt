/*
 * File: DashboardTiles.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: Pending / Approved / Completed count tiles, shared by the dashboard
 *              screen and the Home card so both look the same.
 */
package com.solargrid.mobile.verification.dashboard

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.solargrid.mobile.R
import com.solargrid.mobile.databinding.ItemDashboardCountBinding
import com.solargrid.mobile.verification.dashboard.models.DashboardSummary

object DashboardTiles {

    // Same colours as the booking status pills; numbers show a dash until loaded
    fun setup(
        pending: ItemDashboardCountBinding,
        approved: ItemDashboardCountBinding,
        completed: ItemDashboardCountBinding
    ) {
        paint(pending, R.string.dashboard_count_pending, R.color.booking_pending, R.color.booking_pending_bg)
        paint(approved, R.string.dashboard_count_approved, R.color.station_open, R.color.station_open_bg)
        paint(completed, R.string.dashboard_count_completed, R.color.booking_completed, R.color.booking_completed_bg)
        show(pending, approved, completed, null)
    }

    // Counts from the API, or dashes when there are none yet
    fun show(
        pending: ItemDashboardCountBinding,
        approved: ItemDashboardCountBinding,
        completed: ItemDashboardCountBinding,
        summary: DashboardSummary?
    ) {
        setCount(pending, summary?.pendingCount)
        setCount(approved, summary?.approvedCount)
        setCount(completed, summary?.completedCount)
    }

    private fun setCount(tile: ItemDashboardCountBinding, count: Int?) {
        tile.tvCountValue.text = count?.toString()
            ?: tile.root.context.getString(R.string.dashboard_count_unknown)
    }

    private fun paint(
        tile: ItemDashboardCountBinding,
        @StringRes label: Int,
        @ColorRes text: Int,
        @ColorRes background: Int
    ) {
        val context = tile.root.context
        tile.root.setCardBackgroundColor(ContextCompat.getColor(context, background))
        tile.tvCountValue.setTextColor(ContextCompat.getColor(context, text))
        tile.tvCountLabel.setText(label)
    }
}
