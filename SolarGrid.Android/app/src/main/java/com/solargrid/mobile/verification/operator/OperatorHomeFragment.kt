/*
 * File: OperatorHomeFragment.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: Grid Operator Home tab. Greets the operator, lists the stations
 *              assigned to them and opens the QR scanner.
 */
package com.solargrid.mobile.verification.operator

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.solargrid.mobile.R
import com.solargrid.mobile.auth.LoginManager
import com.solargrid.mobile.databinding.FragmentOperatorHomeBinding
import kotlinx.coroutines.launch

class OperatorHomeFragment : Fragment() {

    private var _binding: FragmentOperatorHomeBinding? = null
    private val binding get() = _binding!!

    // Inflate the layout
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOperatorHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Pull to refresh and the Scan button, then load the greeting and stations
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.swipeOperatorHome.setColorSchemeResources(R.color.accent_gold)
        binding.swipeOperatorHome.setOnRefreshListener { load() }
        binding.btnScanQr.setOnClickListener {
            startActivity(Intent(requireContext(), ScanQrActivity::class.java))
        }
        load()
    }

    // Tabs are hidden, not destroyed, so refresh when the operator comes back
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && _binding != null) load()
    }

    // Saved name first so the header is never empty, then the stations from the API
    private fun load() {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = LoginManager.getInstance().restoreSession() ?: return@launch
            binding.tvOperatorGreeting.text =
                getString(R.string.operator_home_greeting, user.fullName.trim())

            binding.swipeOperatorHome.isRefreshing = true
            val result = OperatorManager.getInstance().getMyStations()
            binding.swipeOperatorHome.isRefreshing = false

            result
                .onSuccess { stations ->
                    val names = stations.mapNotNull { it.stationName?.takeIf(String::isNotBlank) }
                    binding.tvOperatorStations.text = if (names.isEmpty()) {
                        getString(R.string.operator_home_no_station)
                    } else {
                        names.joinToString(", ")
                    }
                    binding.tvOperatorHomeMessage.isVisible = false
                }
                .onFailure { error ->
                    binding.tvOperatorHomeMessage.text = getString(
                        R.string.operator_error_retry,
                        error.message ?: getString(R.string.operator_error_stations)
                    )
                    binding.tvOperatorHomeMessage.isVisible = true
                }
        }
    }

    // Drop the view reference so the old layout can be freed
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
