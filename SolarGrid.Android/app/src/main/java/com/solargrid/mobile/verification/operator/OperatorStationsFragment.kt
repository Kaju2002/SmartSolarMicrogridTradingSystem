/*
 * File: OperatorStationsFragment.kt
 * Module: Verification and Dashboard (Aaron)
 * Description: Grid Operator Stations tab: stations assigned to the operator.
 */
package com.solargrid.mobile.verification.operator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.solargrid.mobile.databinding.FragmentOperatorStationsBinding

class OperatorStationsFragment : Fragment() {

    private var _binding: FragmentOperatorStationsBinding? = null
    private val binding get() = _binding!!

    // Inflate the layout
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOperatorStationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Drop the view reference so the old layout can be freed
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
