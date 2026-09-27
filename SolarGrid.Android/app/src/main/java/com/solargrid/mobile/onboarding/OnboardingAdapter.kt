/*
 * File: OnboardingAdapter.kt
 * Module: Onboarding (shared)
 * Description: Fills each ViewPager2 page (photo, texts, dots) and reports arrow taps.
 */
package com.solargrid.mobile.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.solargrid.mobile.R
import com.solargrid.mobile.databinding.ItemOnboardingPageBinding

class OnboardingAdapter(
    private val pages: List<OnboardingPage>,
    private val onNextClick: (position: Int) -> Unit
) : RecyclerView.Adapter<OnboardingAdapter.PageViewHolder>() {

    // Navigation bar height, so the card sits above it
    private var bottomInset = 0

    class PageViewHolder(val binding: ItemOnboardingPageBinding) :
        RecyclerView.ViewHolder(binding.root)

    // Inflate one page
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemOnboardingPageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PageViewHolder(binding)
    }

    // Put the page content, dots and arrow action on screen
    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val page = pages[position]
        val binding = holder.binding
        val res = binding.root.resources

        binding.ivPhoto.setImageResource(page.imageRes)
        binding.tvTitle.setText(page.titleRes)
        binding.tvBody.setText(page.bodyRes)

        val baseMargin = res.getDimensionPixelSize(R.dimen.onboarding_card_margin)
        binding.cardContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = baseMargin + bottomInset
        }

        showDots(binding, position)

        val isLast = position == pages.lastIndex
        binding.btnNext.contentDescription = res.getString(
            if (isLast) R.string.onboarding_get_started else R.string.onboarding_next
        )
        binding.btnNext.setOnClickListener { onNextClick(holder.bindingAdapterPosition) }
    }

    override fun getItemCount(): Int = pages.size

    // Called from the activity once the window insets are known
    fun setBottomInset(inset: Int) {
        if (inset == bottomInset) return
        bottomInset = inset
        notifyItemRangeChanged(0, itemCount)
    }

    // Current page dot is a wide gold pill, the others are small grey dots.
    // Extra dots hide if there are ever fewer than three pages.
    private fun showDots(binding: ItemOnboardingPageBinding, position: Int) {
        val res = binding.root.resources
        val dotSize = res.getDimensionPixelSize(R.dimen.onboarding_dot_size)
        val activeWidth = res.getDimensionPixelSize(R.dimen.onboarding_dot_active_width)

        listOf<View>(binding.dot1, binding.dot2, binding.dot3).forEachIndexed { index, dot ->
            val active = index == position
            dot.isVisible = index < pages.size
            dot.setBackgroundResource(if (active) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
            dot.updateLayoutParams { width = if (active) activeWidth else dotSize }
        }
    }
}
