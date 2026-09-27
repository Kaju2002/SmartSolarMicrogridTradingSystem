/*
 * File: ProfileFragment.kt
 * Module: Identity and Access (Vithusha)
 * Description: Profile tab. Shows the logged-in user and handles logout.
 */
package com.solargrid.mobile.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.solargrid.mobile.R
import com.solargrid.mobile.core.utils.initialsOf
import com.solargrid.mobile.databinding.FragmentProfileBinding
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // Inflate the layout
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Load the user and hook up logout
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnLogout.setOnClickListener { logout() }
        loadUser()
    }

    // Name, initials and NIC from the saved session
    private fun loadUser() {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = LoginManager.getInstance().restoreSession() ?: return@launch
            binding.tvAvatar.text = initialsOf(user.fullName)
            binding.tvProfileName.text = user.fullName
            binding.tvProfileNic.isVisible = !user.nic.isNullOrBlank()
            binding.tvProfileNic.text = getString(R.string.profile_nic, user.nic.orEmpty())
        }
    }

    // Clear the session, then login becomes the only screen
    private fun logout() {
        binding.btnLogout.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            LoginManager.getInstance().logout()
            val intent = Intent(requireContext(), LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
    }

    // Drop the view reference so the old layout can be freed
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
