package com.supportai.assistant.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.supportai.assistant.R
import com.supportai.assistant.engine.AICommand
import com.supportai.assistant.engine.AIVoiceEngine
import com.supportai.assistant.engine.CommandParser
import com.supportai.assistant.service.AIAccessibilityService
import com.supportai.assistant.service.AIBackgroundVoiceService
import com.supportai.assistant.utils.AnimatedLogoView
import com.supportai.assistant.utils.PermissionHelper

class HomeFragment : Fragment() {

    private lateinit var switchAiMaster: SwitchCompat
    private lateinit var tvAiStatus: TextView
    private lateinit var animatedLogoView: AnimatedLogoView
    private lateinit var tvTranscript: TextView
    private lateinit var tvAiResponse: TextView
    private lateinit var btnVoiceTest: AppCompatButton

    private var localVoiceEngine: AIVoiceEngine? = null

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val raw = intent?.getStringExtra(AIBackgroundVoiceService.EXTRA_RAW_TEXT)
            val response = intent?.getStringExtra(AIBackgroundVoiceService.EXTRA_RESPONSE_TEXT)
            if (raw != null) tvTranscript.text = raw
            if (response != null) tvAiResponse.text = response
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        switchAiMaster = root.findViewById(R.id.switch_ai_master)
        tvAiStatus = root.findViewById(R.id.tv_ai_status)
        animatedLogoView = root.findViewById(R.id.animated_logo_view)
        tvTranscript = root.findViewById(R.id.tv_transcript)
        tvAiResponse = root.findViewById(R.id.tv_ai_response)
        btnVoiceTest = root.findViewById(R.id.btn_voice_test)

        setupAiMasterSwitch()
        setupVoiceTest()
        setupQuickShortcuts(root)

        return root
    }

    private fun setupAiMasterSwitch() {
        switchAiMaster.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Ruxsatlarni tekshirish
                val context = requireContext()
                if (!PermissionHelper.hasRecordAudioPermission(context)) {
                    Toast.makeText(context, "Avval mikrofonga ruxsat bering!", Toast.LENGTH_SHORT).show()
                    switchAiMaster.isChecked = false
                    return@setOnCheckedChangeListener
                }

                tvAiStatus.text = getString(R.string.ai_status_active)
                tvAiStatus.setTextColor(requireContext().getColor(R.color.neon_green))
                animatedLogoView.setActivePulse(true)
                startBackgroundService()
            } else {
                tvAiStatus.text = getString(R.string.ai_status_inactive)
                tvAiStatus.setTextColor(requireContext().getColor(R.color.neon_red))
                animatedLogoView.setActivePulse(false)
                stopBackgroundService()
            }
        }
    }

    private fun startBackgroundService() {
        val intent = Intent(requireContext(), AIBackgroundVoiceService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
    }

    private fun stopBackgroundService() {
        val intent = Intent(requireContext(), AIBackgroundVoiceService::class.java)
        requireContext().stopService(intent)
    }

    private fun setupVoiceTest() {
        localVoiceEngine = AIVoiceEngine(
            context = requireContext(),
            onResult = { text ->
                tvTranscript.text = text
                val command = CommandParser.parse(text)
                executeParsedCommand(command, text)
            },
            onError = { error ->
                tvTranscript.text = "Xatolik: $error"
            },
            onReady = {
                tvTranscript.text = getString(R.string.listening_text)
            }
        )

        btnVoiceTest.setOnClickListener {
            localVoiceEngine?.startListening()
        }
    }

    private fun executeParsedCommand(command: AICommand, rawText: String) {
        when (command) {
            is AICommand.OpenYouTube -> {
                val resp = getString(R.string.ai_voice_ready_youtube)
                tvAiResponse.text = resp
                localVoiceEngine?.speak(resp)
                AIAccessibilityService.instance?.automateYouTube(command.searchQuery) {}
            }
            is AICommand.SeekVideo -> {
                val resp = getString(R.string.ai_voice_seeking)
                tvAiResponse.text = resp
                localVoiceEngine?.speak(resp)
                val percent = (command.targetMinute.toFloat() / 15f).coerceIn(0.1f, 0.9f)
                AIAccessibilityService.instance?.seekVideoProgress(percent)
            }
            is AICommand.OpenTelegram -> {
                val resp = getString(R.string.ai_voice_ready_telegram)
                tvAiResponse.text = resp
                localVoiceEngine?.speak(resp)
                AIAccessibilityService.instance?.automateTelegram(command.chatTarget, command.messageText) {}
            }
            is AICommand.OpenGallery -> {
                val resp = getString(R.string.ai_voice_ready_gallery)
                tvAiResponse.text = resp
                localVoiceEngine?.speak(resp)
                AIAccessibilityService.instance?.automateGallery {}
            }
            is AICommand.OpenSettings -> {
                val resp = getString(R.string.ai_voice_ready_settings)
                tvAiResponse.text = resp
                localVoiceEngine?.speak(resp)
                AIAccessibilityService.instance?.automateSettings {}
            }
            else -> {
                val resp = "Eshitdim: $rawText"
                tvAiResponse.text = resp
                localVoiceEngine?.speak(resp)
            }
        }
    }

    private fun setupQuickShortcuts(root: View) {
        root.findViewById<View>(R.id.btn_test_youtube).setOnClickListener {
            tvTranscript.text = "YouTubeni och va O'zbekiston haqida video qo'y"
            executeParsedCommand(AICommand.OpenYouTube("O'zbekiston"), "YouTube")
        }

        root.findViewById<View>(R.id.btn_test_telegram).setOnClickListener {
            tvTranscript.text = "Telegramda xabar yozish"
            executeParsedCommand(AICommand.OpenTelegram(), "Telegram")
        }

        root.findViewById<View>(R.id.btn_test_gallery).setOnClickListener {
            tvTranscript.text = "Galereyani och"
            executeParsedCommand(AICommand.OpenGallery, "Galereya")
        }

        root.findViewById<View>(R.id.btn_test_settings).setOnClickListener {
            tvTranscript.text = "Sozlamalarni och"
            executeParsedCommand(AICommand.OpenSettings, "Sozlamalar")
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(AIBackgroundVoiceService.ACTION_BROADCAST_COMMAND)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            requireContext().registerReceiver(updateReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            requireContext().unregisterReceiver(updateReceiver)
        } catch (_: Exception) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        localVoiceEngine?.destroy()
    }
}
