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
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.supportai.assistant.R
import com.supportai.assistant.engine.AICommand
import com.supportai.assistant.engine.AIVoiceEngine
import com.supportai.assistant.engine.GeminiAIEngine
import com.supportai.assistant.service.AIAccessibilityService
import com.supportai.assistant.service.AIBackgroundVoiceService
import com.supportai.assistant.utils.PermissionHelper
import com.supportai.assistant.utils.SiriWaveOrbView
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var switchAiMaster: SwitchCompat
    private lateinit var tvAiStatus: TextView
    private lateinit var siriOrbView: SiriWaveOrbView
    private lateinit var tvTranscript: TextView
    private lateinit var tvAiResponse: TextView
    private lateinit var cardMicCenter: View

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
        siriOrbView = root.findViewById(R.id.siri_orb_view)
        tvTranscript = root.findViewById(R.id.tv_transcript)
        tvAiResponse = root.findViewById(R.id.tv_ai_response)
        cardMicCenter = root.findViewById(R.id.card_mic_center)

        setupAiMasterSwitch()
        setupSiriVoiceEngine()

        // Markaziy Siri shariga bosganda ovozli buyruq olish
        cardMicCenter.setOnClickListener {
            siriOrbView.setListeningState(true)
            localVoiceEngine?.startListening()
        }

        return root
    }

    private fun setupAiMasterSwitch() {
        switchAiMaster.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val context = requireContext()
                if (!PermissionHelper.hasRecordAudioPermission(context)) {
                    Toast.makeText(context, "Avval mikrofonga ruxsat bering!", Toast.LENGTH_SHORT).show()
                    switchAiMaster.isChecked = false
                    return@setOnCheckedChangeListener
                }

                tvAiStatus.text = "AI Faol (Erkak Ovozi)"
                tvAiStatus.setTextColor(requireContext().getColor(R.color.neon_green))
                startBackgroundService()
            } else {
                tvAiStatus.text = "AI O'chirilgan"
                tvAiStatus.setTextColor(requireContext().getColor(R.color.neon_red))
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

    private fun setupSiriVoiceEngine() {
        localVoiceEngine = AIVoiceEngine(
            context = requireContext(),
            onResult = { recognizedText ->
                siriOrbView.setListeningState(false)
                tvTranscript.text = recognizedText

                // Gemini AI orqali tahlil qilish va bajarish
                lifecycleScope.launch {
                    tvAiResponse.text = "Gemini AI tahlil qilmoqda..."
                    val (command, responseSpeech) = GeminiAIEngine.processVoiceCommand(requireContext(), recognizedText)

                    tvAiResponse.text = responseSpeech
                    localVoiceEngine?.speak(responseSpeech)

                    executeCommand(command)
                }
            },
            onError = { error ->
                siriOrbView.setListeningState(false)
                tvTranscript.text = "Ovoz qabul qilinmadi: $error"
            },
            onReady = {
                siriOrbView.setListeningState(true)
                tvTranscript.text = "Eshitmoqdaman... Gapiring..."
            }
        )
    }

    private fun executeCommand(command: AICommand) {
        when (command) {
            is AICommand.OpenYouTube -> {
                AIAccessibilityService.instance?.automateYouTube(command.searchQuery) {}
            }
            is AICommand.SeekVideo -> {
                val percent = (command.targetMinute.toFloat() / 15f).coerceIn(0.1f, 0.9f)
                AIAccessibilityService.instance?.seekVideoProgress(percent)
            }
            is AICommand.OpenTelegram -> {
                AIAccessibilityService.instance?.automateTelegram(command.chatTarget, command.messageText) {}
            }
            is AICommand.OpenGallery -> {
                AIAccessibilityService.instance?.automateGallery {}
            }
            is AICommand.OpenSettings -> {
                AIAccessibilityService.instance?.automateSettings {}
            }
            is AICommand.OpenCustomApp -> {
                AIAccessibilityService.instance?.launchAppByName(command.appName)
            }
            is AICommand.GoHome -> {
                AIAccessibilityService.instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
            }
            is AICommand.GoBack -> {
                AIAccessibilityService.instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            }
            is AICommand.TypeText -> {
                AIAccessibilityService.instance?.inputText(command.text)
            }
            else -> {}
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
