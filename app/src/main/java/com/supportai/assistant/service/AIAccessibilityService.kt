package com.supportai.assistant.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AIAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "AIAccessibility"
        var instance: AIAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Support AI Accessibility Service connected successfully")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Ekrandagi o'zgarishlar (zarurat tug'ilganda kuzatish uchun)
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    /**
     * Ilovani ochish
     */
    fun launchApp(packageName: String): Boolean {
        return try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch $packageName", e)
            false
        }
    }

    /**
     * Matn bo'yicha elementni topish va bosish
     */
    fun clickByText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val nodes = rootNode.findAccessibilityNodeInfosByText(text)
        for (node in nodes) {
            if (performClickAction(node)) {
                return true
            }
        }
        return false
    }

    /**
     * ViewId bo'yicha elementni topish va bosish
     */
    fun clickByViewId(viewId: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
        for (node in nodes) {
            if (performClickAction(node)) {
                return true
            }
        }
        return false
    }

    private fun performClickAction(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        // Agar ota-elementi bosiladigan bo'lsa
        val parent = node.parent
        return if (parent != null) {
            val clicked = performClickAction(parent)
            parent.recycle()
            clicked
        } else {
            false
        }
    }

    /**
     * Hozirgi fokuslangan maydonga yoki qidiruv satriga matn yozish
     */
    fun inputText(text: String, viewId: String? = null): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val targetNode: AccessibilityNodeInfo? = if (viewId != null) {
            rootNode.findAccessibilityNodeInfosByViewId(viewId).firstOrNull()
        } else {
            rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        }

        if (targetNode != null) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val result = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            targetNode.recycle()
            return result
        }
        return false
    }

    /**
     * Koordinata bo'yicha ekranga teginish (Click Gesture)
     */
    fun clickCoordinates(x: Float, y: Float) {
        val path = Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()
        dispatchGesture(gesture, null, null)
    }

    /**
     * Videoni ma'lum bir minutga o'tkazish (Gorizontal surish / Progress bar bosish)
     */
    fun seekVideoProgress(percent: Float) {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val screenHeight = displayMetrics.heightPixels.toFloat()

        // Odatda video player ekranning yuqori yoki o'rta qismida bo'ladi
        val playerY = screenHeight * 0.35f
        val startX = screenWidth * 0.1f
        val targetX = screenWidth * percent.coerceIn(0.1f, 0.9f)

        val path = Path().apply {
            moveTo(startX, playerY)
            lineTo(targetX, playerY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()

        dispatchGesture(gesture, null, null)
    }

    /**
     * YouTube da qidirish va videoni ochish avtomatizatsiyasi
     */
    fun automateYouTube(searchQuery: String?, onComplete: () -> Unit) {
        launchApp("com.google.android.youtube")

        mainHandler.postDelayed({
            // Qidiruv tugmasini topish va bosish
            clickByViewId("com.google.android.youtube:id/menu_item_0")
            clickByText("Search")

            mainHandler.postDelayed({
                if (searchQuery != null) {
                    // Qidiruv so'zini yozish
                    inputText(searchQuery, "com.google.android.youtube:id/search_edit_text")

                    mainHandler.postDelayed({
                        // Birinchi videoga bosish
                        clickCoordinates(resources.displayMetrics.widthPixels / 2f, 600f)
                        onComplete()
                    }, 1200)
                } else {
                    onComplete()
                }
            }, 800)
        }, 1500)
    }

    /**
     * Telegramda xabar yozish avtomatizatsiyasi
     */
    fun automateTelegram(chatTarget: String?, messageText: String?, onComplete: () -> Unit) {
        launchApp("org.telegram.messenger")

        mainHandler.postDelayed({
            if (chatTarget != null) {
                // Qidiruv tugmasini bosish
                clickByViewId("org.telegram.messenger:id/search_button")
                mainHandler.postDelayed({
                    inputText(chatTarget)
                    mainHandler.postDelayed({
                        // Birinchi kontaktni tanlash
                        clickCoordinates(resources.displayMetrics.widthPixels / 2f, 400f)
                        if (messageText != null) {
                            mainHandler.postDelayed({
                                inputText(messageText)
                                // Yuborish tugmasi
                                clickByViewId("org.telegram.messenger:id/send_button")
                                onComplete()
                            }, 1000)
                        } else {
                            onComplete()
                        }
                    }, 1000)
                }, 800)
            } else {
                onComplete()
            }
        }, 1500)
    }

    /**
     * Galereyani ochish
     */
    fun automateGallery(onComplete: () -> Unit) {
        // Universal Gallery Intent
        val intent = Intent(Intent.ACTION_VIEW).apply {
            type = "image/*"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            launchApp("com.google.android.apps.photos")
        }
        mainHandler.postDelayed({ onComplete() }, 1000)
    }

    /**
     * Sozlamalarni ochish
     */
    fun automateSettings(onComplete: () -> Unit) {
        val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        mainHandler.postDelayed({ onComplete() }, 1000)
    }
}
