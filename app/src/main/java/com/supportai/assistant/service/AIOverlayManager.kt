package com.supportai.assistant.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.supportai.assistant.R
import com.supportai.assistant.utils.PermissionHelper

class AIOverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    fun showOverlay(initialText: String = "Eshitmoqdaman...") {
        if (!PermissionHelper.canDrawOverlays(context)) return

        if (overlayView != null) {
            updateText(initialText)
            return
        }

        val inflater = LayoutInflater.from(context)
        overlayView = inflater.inflate(R.layout.overlay_ai_bubble, null)

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 200
        }

        setupDragListener()
        updateText(initialText)

        try {
            windowManager.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupDragListener() {
        val root = overlayView?.findViewById<View>(R.id.overlay_root) ?: return
        root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams?.x ?: 0
                    initialY = layoutParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams?.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams?.y = initialY + (event.rawY - initialTouchY).toInt()
                    if (overlayView != null && layoutParams != null) {
                        windowManager.updateViewLayout(overlayView, layoutParams)
                    }
                    true
                }
                else -> false
            }
        }
    }

    fun updateText(message: String, autoHideSeconds: Long = 0) {
        mainHandler.post {
            overlayView?.findViewById<TextView>(R.id.tv_overlay_speech)?.text = message
        }

        if (autoHideSeconds > 0) {
            mainHandler.postDelayed({
                hideOverlay()
            }, autoHideSeconds * 1000)
        }
    }

    fun hideOverlay() {
        mainHandler.post {
            if (overlayView != null) {
                try {
                    windowManager.removeView(overlayView)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                overlayView = null
            }
        }
    }
}
