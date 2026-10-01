package com.supportai.assistant.ui

import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import com.supportai.assistant.R

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val logoCard = findViewById<CardView>(R.id.card_welcome_logo)
        val btnGetStarted = findViewById<AppCompatButton>(R.id.btn_get_started)

        // Professional kirish animatsiyasi
        logoCard.scaleX = 0.5f
        logoCard.scaleY = 0.5f
        logoCard.alpha = 0f

        logoCard.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(1000)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        btnGetStarted.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
