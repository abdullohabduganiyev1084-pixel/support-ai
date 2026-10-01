package com.supportai.assistant.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.supportai.assistant.R
import com.supportai.assistant.utils.PermissionHelper

class PermissionsFragment : Fragment() {

    private lateinit var tvStatusAccessibility: TextView
    private lateinit var tvStatusOverlay: TextView
    private lateinit var tvStatusMic: TextView
    private lateinit var tvStatusBattery: TextView

    private val requestMicLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        refreshPermissionStates()
    }

    private val requestNotificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        refreshPermissionStates()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_permissions, container, false)

        tvStatusAccessibility = root.findViewById(R.id.tv_status_accessibility)
        tvStatusOverlay = root.findViewById(R.id.tv_status_overlay)
        tvStatusMic = root.findViewById(R.id.tv_status_mic)
        tvStatusBattery = root.findViewById(R.id.tv_status_battery)

        // 1. Accessibility tugmasi
        root.findViewById<AppCompatButton>(R.id.btn_perm_accessibility).setOnClickListener {
            PermissionHelper.openAccessibilitySettings(requireContext())
        }

        // 2. Overlay tugmasi
        root.findViewById<AppCompatButton>(R.id.btn_perm_overlay).setOnClickListener {
            PermissionHelper.openOverlaySettings(requireContext())
        }

        // 3. Mikrofon tugmasi
        root.findViewById<AppCompatButton>(R.id.btn_perm_mic).setOnClickListener {
            if (!PermissionHelper.hasRecordAudioPermission(requireContext())) {
                requestMicLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        // 4. Batareya cheklovi tugmasi
        root.findViewById<AppCompatButton>(R.id.btn_perm_battery).setOnClickListener {
            PermissionHelper.requestIgnoreBatteryOptimizations(requireContext())
        }

        return root
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStates()
    }

    private fun refreshPermissionStates() {
        val context = context ?: return

        // Accessibility
        val isA11y = PermissionHelper.isAccessibilityServiceEnabled(context)
        updateStatusBadge(tvStatusAccessibility, isA11y)

        // Overlay
        val isOverlay = PermissionHelper.canDrawOverlays(context)
        updateStatusBadge(tvStatusOverlay, isOverlay)

        // Mic
        val isMic = PermissionHelper.hasRecordAudioPermission(context)
        updateStatusBadge(tvStatusMic, isMic)

        // Battery
        val isBattery = PermissionHelper.isIgnoringBatteryOptimizations(context)
        updateStatusBadge(tvStatusBattery, isBattery)
    }

    private fun updateStatusBadge(textView: TextView, granted: Boolean) {
        if (granted) {
            textView.text = "✓ Berilgan"
            textView.setTextColor(requireContext().getColor(R.color.neon_green))
        } else {
            textView.text = "✗ Berilmagan"
            textView.setTextColor(requireContext().getColor(R.color.neon_red))
        }
    }
}
