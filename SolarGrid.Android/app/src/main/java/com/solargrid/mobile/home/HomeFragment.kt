/*
 * File: HomeFragment.kt
 * Module: Team
 * Description: Home tab. Illustrated hero with a greeting, feature cards,
 *              "Your SolarGrid" shortcuts and a booking tip. All content is static;
 *              taps move to the other tabs or open a short info sheet.
 */
package com.solargrid.mobile.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.solargrid.mobile.R
import com.solargrid.mobile.auth.LoginManager
import com.solargrid.mobile.core.utils.initialsOf
import com.solargrid.mobile.databinding.FragmentHomeBinding
import com.solargrid.mobile.databinding.ItemHomeCardBinding
import com.solargrid.mobile.databinding.ItemHomeRowBinding
import com.solargrid.mobile.databinding.SheetHomeInfoBinding
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Inflate the layout
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Fill the static content, hook up taps, then load the user's name
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyWindowInsets()
        setupStatusScrim()
        setupContent()
        setupListeners()
        binding.tvGreeting.text = getString(greetingRes())
        loadUser()
    }

    // Tabs are hidden, not destroyed, so pick up a name changed on the Profile tab
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && _binding != null) loadUser()
    }

    // The hero runs behind the status bar, so push only its text down
    private fun applyWindowInsets() {
        val basePadding = resources.getDimensionPixelSize(R.dimen.space_md)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.layoutHeroText.updatePadding(top = basePadding + bars.top)
            binding.viewStatusScrim.updateLayoutParams { height = bars.top }
            insets
        }
        // Added after the first insets pass, so ask again
        ViewCompat.requestApplyInsets(binding.root)
    }

    // Fade the status bar strip in as the white sheet scrolls up to it
    private fun setupStatusScrim() {
        val fadeLength = resources.getDimensionPixelSize(R.dimen.home_scrim_fade)
        binding.scrollHome.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
                val sheetReachesTop = binding.layoutSheet.top - binding.viewStatusScrim.height
                val progress = (scrollY - (sheetReachesTop - fadeLength)).toFloat() / fadeLength
                binding.viewStatusScrim.alpha = progress.coerceIn(0f, 1f)
            }
        )
    }

    // Images, tints and texts for the cards and rows
    private fun setupContent() {
        bindCard(binding.cardFind, R.color.home_card_teal, R.drawable.img_home_find,
            R.string.home_card_find_title, R.string.home_card_find_subtitle)
        bindCard(binding.cardBook, R.color.home_card_gold, R.drawable.img_home_book,
            R.string.home_card_book_title, R.string.home_card_book_subtitle)
        bindCard(binding.cardQr, R.color.home_card_blue, R.drawable.img_home_qr,
            R.string.home_card_qr_title, R.string.home_card_qr_subtitle)

        bindRow(binding.rowBookings, R.drawable.ic_calendar_outlined,
            R.string.home_row_bookings, R.string.home_row_bookings_sub)
        bindRow(binding.rowHow, R.drawable.ic_help,
            R.string.home_row_how, R.string.home_row_how_sub)
        bindRow(binding.rowWhy, R.drawable.ic_sunny,
            R.string.home_row_why, R.string.home_row_why_sub)
        bindRow(binding.rowProfile, R.drawable.ic_person_outlined,
            R.string.home_row_profile, R.string.home_row_profile_sub)
    }

    // Where each tap goes. Booking starts from a station; QR codes live on approved bookings.
    private fun setupListeners() {
        binding.tvAvatar.setOnClickListener { openTab(R.id.nav_profile) }

        binding.cardFind.root.setOnClickListener { openTab(R.id.nav_stations) }
        binding.cardBook.root.setOnClickListener { openTab(R.id.nav_stations) }
        binding.cardQr.root.setOnClickListener { openTab(R.id.nav_bookings) }

        binding.rowBookings.root.setOnClickListener { openTab(R.id.nav_bookings) }
        binding.rowHow.root.setOnClickListener { showInfo(R.string.home_row_how, R.string.home_how_body) }
        binding.rowWhy.root.setOnClickListener { showInfo(R.string.home_row_why, R.string.home_why_body) }
        binding.rowProfile.root.setOnClickListener { openTab(R.id.nav_profile) }

        binding.tvTipLink.setOnClickListener { showInfo(R.string.home_row_how, R.string.home_how_body) }
    }

    // Greeting with the full name (Sri Lankan names often start with an initial) and the avatar
    private fun loadUser() {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = LoginManager.getInstance().restoreSession() ?: return@launch
            val name = user.fullName.trim()
            if (name.isNotEmpty()) {
                binding.tvGreeting.text =
                    getString(R.string.home_greeting_with_name, getString(greetingRes()), name)
            }
            binding.tvAvatar.text = initialsOf(name)
        }
    }

    // Morning before 12, afternoon before 5 pm, evening after
    @StringRes
    private fun greetingRes(): Int {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> R.string.home_greeting_morning
            hour < 17 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        }
    }

    // Fill one feature card
    private fun bindCard(
        card: ItemHomeCardBinding,
        @ColorRes tint: Int,
        @DrawableRes image: Int,
        @StringRes title: Int,
        @StringRes subtitle: Int
    ) {
        card.root.setCardBackgroundColor(ContextCompat.getColor(requireContext(), tint))
        card.ivCardImage.setImageResource(image)
        card.tvCardTitle.setText(title)
        card.tvCardSubtitle.setText(subtitle)
    }

    // Fill one shortcut row
    private fun bindRow(
        row: ItemHomeRowBinding,
        @DrawableRes icon: Int,
        @StringRes title: Int,
        @StringRes subtitle: Int
    ) {
        row.ivRowIcon.setImageResource(icon)
        row.tvRowTitle.setText(title)
        row.tvRowSubtitle.setText(subtitle)
    }

    // Switch the bottom nav to another tab
    private fun openTab(itemId: Int) {
        (activity as? ProsumerMainActivity)?.openTab(itemId)
    }

    // Short explanation in a bottom sheet
    private fun showInfo(@StringRes title: Int, @StringRes body: Int) {
        val dialog = BottomSheetDialog(requireContext())
        val sheet = SheetHomeInfoBinding.inflate(layoutInflater)
        sheet.tvSheetTitle.setText(title)
        sheet.tvSheetBody.setText(body)
        dialog.setContentView(sheet.root)
        dialog.show()
    }

    // Drop the view reference so the old layout can be freed
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
