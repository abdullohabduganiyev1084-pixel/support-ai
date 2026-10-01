package com.supportai.assistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.supportai.assistant.R
import com.supportai.assistant.engine.AICommand
import com.supportai.assistant.engine.AIVoiceEngine
import com.supportai.assistant.engine.CommandParser
import com.supportai.assistant.ui.MainActivity

class AIBackgroundVoiceService : Service() {

    private lateinit var voiceEngine: AIVoiceEngine
    private lateinit var overlayManager: AIOverlayManager
    private var isListeningLoopActive = true

    companion object {
        const val CHANNEL_ID = "SupportAI_Voice_Channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_BROADCAST_COMMAND = "com.supportai.assistant.COMMAND_UPDATE"
        const val EXTRA_RAW_TEXT = "extra_raw_text"
        const val EXTRA_RESPONSE_TEXT = "extra_response_text"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Support AI eshitmoqda..."))

        overlayManager = AIOverlayManager(this)

        voiceEngine = AIVoiceEngine(
            context = this,
            onResult = { recognizedText ->
                handleVoiceInput(recognizedText)
            },
            onError = { _ ->
                // Ovoz tugaganda yoki xato bo'lganda orqa fonda qayta tinglashni davom ettirish
                if (isListeningLoopActive) {
                    voiceEngine.startListening()
                }
            }
        )

        // Tinglashni boshlash
        voiceEngine.startListening()
    }

    private fun handleVoiceInput(text: String) {
        val clean = text.lowercase().trim()

        // Hotword tekshirish: "Hey AI" yoki "Salom AI"
        val isHotword = clean.contains("hey ai") || clean.contains("salom ai") || clean.contains("ai")

        // Buyruqni tahlil qilish
        val command = CommandParser.parse(clean)

        // Floating overlay ko'rsatish
        overlayManager.showOverlay("Siz: $text")

        when (command) {
            is AICommand.OpenYouTube -> {
                val response = getString(R.string.ai_voice_ready_youtube)
                AIAccessibilityService.instance?.automateYouTube(command.searchQuery) {
                    overlayManager.updateText(response, autoHideSeconds = 6)
                    voiceEngine.speak(response)
                    broadcastUpdate(text, response)
                } ?: run {
                    overlayManager.updateText(response, autoHideSeconds = 6)
                    voiceEngine.speak(response)
                    broadcastUpdate(text, response)
                }
            }

            is AICommand.SeekVideo -> {
                val response = getString(R.string.ai_voice_seeking)
                // 10 minutlik videoda daqiqasiga qarab foiz hisoblash
                val percent = (command.targetMinute.toFloat() / 15f).coerceIn(0.1f, 0.9f)
                AIAccessibilityService.instance?.seekVideoProgress(percent)
                overlayManager.updateText(response, autoHideSeconds = 4)
                voiceEngine.speak(response)
                broadcastUpdate(text, response)
            }

            is AICommand.OpenTelegram -> {
                val response = getString(R.string.ai_voice_ready_telegram)
                AIAccessibilityService.instance?.automateTelegram(command.chatTarget, command.messageText) {
                    overlayManager.updateText(response, autoHideSeconds = 6)
                    voiceEngine.speak(response)
                    broadcastUpdate(text, response)
                } ?: run {
                    overlayManager.updateText(response, autoHideSeconds = 6)
                    voiceEngine.speak(response)
                    broadcastUpdate(text, response)
                }
            }

            is AICommand.OpenGallery -> {
                val response = getString(R.string.ai_voice_ready_gallery)
                AIAccessibilityService.instance?.automateGallery {
                    overlayManager.updateText(response, autoHideSeconds = 5)
                    voiceEngine.speak(response)
                    broadcastUpdate(text, response)
                }
            }

            is AICommand.OpenSettings -> {
                val response = getString(R.string.ai_voice_ready_settings)
                AIAccessibilityService.instance?.automateSettings {
                    overlayManager.updateText(response, autoHideSeconds = 5)
                    voiceEngine.speak(response)
                    broadcastUpdate(text, response)
                }
            }

            is AICommand.GoHome -> {
                AIAccessibilityService.instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
                overlayManager.hideOverlay()
            }

            is AICommand.GoBack -> {
                AIAccessibilityService.instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
                overlayManager.hideOverlay()
            }

            is AICommand.TypeText -> {
                AIAccessibilityService.instance?.inputText(command.text)
                overlayManager.updateText("Yozildi: ${command.text}", autoHideSeconds = 3)
            }

            is AICommand.Unknown -> {
                if (isHotword) {
                    val response = "Eshitmoqdaman! Nima yordam bera olaman?"
                    overlayManager.updateText(response, autoHideSeconds = 4)
                    voiceEngine.speak(response)
                    broadcastUpdate(text, response)
                }
            }
        }

        // Qayta eshitish uchun pauzadan so'ng tinglashni davom ettirish
        if (isListeningLoopActive) {
            voiceEngine.startListening()
        }
    }

    private fun broadcastUpdate(rawText: String, responseText: String) {
        val intent = Intent(ACTION_BROADCAST_COMMAND).apply {
            putExtra(EXTRA_RAW_TEXT, rawText)
            putExtra(EXTRA_RESPONSE_TEXT, responseText)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun buildNotification(status: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isListeningLoopActive = false
        voiceEngine.destroy()
        overlayManager.hideOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
