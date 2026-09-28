/*
 * File: ProfileFragment.kt
 * Module: Identity and Access (Vithusha)
 * Description: Profile tab. Shows the logged-in user's details from the API,
 *              opens the edit screen, and handles logout and account deactivation.
 */
package com.solargrid.mobile.auth

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.solargrid.mobile.R
import com.solargrid.mobile.auth.models.UserProfile
import com.solargrid.mobile.core.utils.initialsOf
import com.solargrid.mobile.databinding.FragmentProfileBinding
import com.solargrid.mobile.databinding.ItemProfileActionBinding
import com.solargrid.mobile.databinding.ItemProfileRowBinding
import com.solargrid.mobile.reservation.ReservationTime
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private var profile: UserProfile? = null

    // Edit screen reports back with the API's message; reload so the card shows the new values
    private val editLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val message = result.data?.getStringExtra(EditProfileActivity.EXTRA_MESSAGE)
                showSnackbar(message?.takeIf { it.isNotBlank() } ?: getString(R.string.profile_saved))
                loadProfile()
            }
        }

    // Inflate the layout
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Set up the rows and taps, then load the user
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindRow(binding.rowName, R.string.full_name_label, editable = true)
        bindRow(binding.rowPhone, R.string.phone_label, editable = true)
        bindRow(binding.rowEmail, R.string.email_label, editable = true)
        bindRow(binding.rowNic, R.string.nic_label, editable = false)
        bindRow(binding.rowMemberSince, R.string.profile_member_since, editable = false)
        bindAction(binding.rowLogout, R.drawable.ic_logout, R.string.profile_logout, R.color.text_primary)
        bindAction(binding.rowDeactivate, R.drawable.ic_block, R.string.profile_deactivate, R.color.error_red)
        showDetails(null)

        binding.btnAvatarEdit.setOnClickListener { openEdit() }
        binding.tvEditDetails.setOnClickListener { openEdit() }
        binding.rowLogout.root.setOnClickListener { logout() }
        binding.rowDeactivate.root.setOnClickListener { confirmDeactivate() }
        loadProfile()
    }

    // Tabs are hidden, not destroyed, so refresh when the user comes back (e.g. after a web edit)
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && _binding != null) loadProfile()
    }

    // Label never changes; editable rows get an arrow and open the edit screen
    private fun bindRow(row: ItemProfileRowBinding, @StringRes label: Int, editable: Boolean) {
        row.tvRowLabel.setText(label)
        row.ivRowChevron.visibility = if (editable) View.VISIBLE else View.INVISIBLE
        if (editable) {
            row.root.setOnClickListener { openEdit() }
        } else {
            row.root.isClickable = false
            row.root.background = null
        }
    }

    // Icon, title and colour of an account row
    private fun bindAction(
        row: ItemProfileActionBinding,
        @DrawableRes icon: Int,
        @StringRes title: Int,
        @ColorRes color: Int
    ) {
        val tint = ContextCompat.getColor(requireContext(), color)
        row.ivActionIcon.setImageResource(icon)
        row.ivActionIcon.imageTintList = ColorStateList.valueOf(tint)
        row.tvActionTitle.setText(title)
        row.tvActionTitle.setTextColor(tint)
    }

    // Saved name and NIC first so the screen is never empty, then the full details from the API
    private fun loadProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = LoginManager.getInstance().restoreSession()
            if (user == null) {
                openLogin()
                return@launch
            }
            if (profile == null) showUser(user.fullName, user.nic)

            binding.pbProfile.isVisible = true
            val result = ProfileManager.getInstance().getProfile()
            binding.pbProfile.isVisible = false

            result
                .onSuccess { loaded ->
                    profile = loaded
                    showUser(loaded.fullName ?: user.fullName, loaded.nic ?: user.nic)
                    showDetails(loaded)
                    binding.tvProfileMessage.isVisible = false
                }
                .onFailure { error ->
                    binding.tvProfileMessage.text = getString(
                        R.string.profile_load_failed,
                        error.message ?: getString(R.string.profile_error_load)
                    )
                    binding.tvProfileMessage.isVisible = true
                }
        }
    }

    // Header (avatar, name) and the saved name/NIC rows, so offline still shows something
    private fun showUser(fullName: String, nic: String?) {
        val empty = getString(R.string.profile_value_missing)
        binding.tvAvatar.text = initialsOf(fullName)
        binding.tvProfileName.text = fullName
        binding.rowName.tvRowValue.text = fullName.ifBlank { empty }
        binding.rowNic.tvRowValue.text = nic?.takeIf { it.isNotBlank() } ?: empty
    }

    // Phone, e-mail, member since and the status pill; dashes until the API answers
    private fun showDetails(details: UserProfile?) {
        val empty = getString(R.string.profile_value_missing)
        binding.rowPhone.tvRowValue.text = details?.phoneNumber?.takeIf { it.isNotBlank() } ?: empty
        binding.rowEmail.tvRowValue.text = details?.email?.takeIf { it.isNotBlank() } ?: empty
        binding.rowMemberSince.tvRowValue.text =
            ReservationTime.parseUtc(details?.createdAt)?.let { memberSince(it) } ?: empty
        bindStatus(details?.status)
    }

    // "20 Sep 2026" in Sri Lanka time
    private fun memberSince(millis: Long): String =
        SimpleDateFormat("d MMM yyyy", Locale.getDefault())
            .apply { timeZone = ReservationTime.SRI_LANKA }
            .format(Date(millis))

    // Green for Active, grey for anything else
    private fun bindStatus(status: String?) {
        val pill = binding.tvProfileStatus
        if (status.isNullOrBlank()) {
            pill.isVisible = false
            return
        }
        val (label, text, background) = when (status) {
            STATUS_ACTIVE -> Triple(getString(R.string.profile_status_active), R.color.station_open, R.color.station_open_bg)
            STATUS_PENDING -> Triple(getString(R.string.profile_status_pending), R.color.booking_pending, R.color.booking_pending_bg)
            else -> Triple(status, R.color.station_closed, R.color.station_closed_bg)
        }
        pill.text = label
        pill.setTextColor(ContextCompat.getColor(requireContext(), text))
        pill.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), background))
        pill.isVisible = true
    }

    // Edit screen starts with the current values; needs them from the API first
    private fun openEdit() {
        val current = profile
        if (current == null) {
            showSnackbar(getString(R.string.profile_edit_unavailable))
            return
        }
        editLauncher.launch(
            EditProfileActivity.newIntent(
                requireContext(),
                fullName = current.fullName.orEmpty(),
                email = current.email.orEmpty(),
                phoneNumber = current.phoneNumber.orEmpty()
            )
        )
    }

    // Ask first: after this the user cannot sign in until Backoffice reactivates the account
    private fun confirmDeactivate() {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.profile_deactivate_title)
            .setMessage(R.string.profile_deactivate_message)
            .setNegativeButton(R.string.profile_deactivate_keep, null)
            .setPositiveButton(R.string.profile_deactivate_confirm) { _, _ -> deactivate() }
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            ?.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red))
    }

    // On success the phone is already logged out, so go straight to login
    private fun deactivate() {
        setBusy(true)
        viewLifecycleOwner.lifecycleScope.launch {
            ProfileManager.getInstance().deactivateAccount()
                .onSuccess {
                    Toast.makeText(requireContext().applicationContext,
                        R.string.profile_deactivated, Toast.LENGTH_LONG).show()
                    openLogin()
                }
                .onFailure { error ->
                    setBusy(false)
                    showSnackbar(error.message ?: getString(R.string.profile_error_deactivate))
                }
        }
    }

    // Clear the session, then login becomes the only screen
    private fun logout() {
        setBusy(true)
        viewLifecycleOwner.lifecycleScope.launch {
            LoginManager.getInstance().logout()
            openLogin()
        }
    }

    // Lock the taps while a request is running
    private fun setBusy(busy: Boolean) {
        listOf(
            binding.btnAvatarEdit, binding.tvEditDetails,
            binding.rowName.root, binding.rowPhone.root, binding.rowEmail.root,
            binding.rowLogout.root, binding.rowDeactivate.root
        ).forEach { it.isEnabled = !busy }
    }

    // Login becomes the only screen, so Back cannot return here
    private fun openLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    // Above the bottom navigation, not on top of it
    private fun showSnackbar(message: String) {
        val view = _binding?.root ?: return
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
            .apply { activity?.findViewById<View>(R.id.bottomNav)?.let { anchorView = it } }
            .show()
    }

    // Drop the view reference so the old layout can be freed
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val STATUS_ACTIVE = "Active"
        private const val STATUS_PENDING = "PendingApproval"
    }
}
