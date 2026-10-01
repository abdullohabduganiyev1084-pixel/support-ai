package com.supportai.assistant.ui

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import com.supportai.assistant.R
import com.supportai.assistant.engine.GeminiAIEngine
import java.util.Locale

class SettingsFragment : Fragment() {

    private lateinit var etApiKey: EditText
    private lateinit var btnSaveApiKey: AppCompatButton
    private lateinit var btnTestSpeech: AppCompatButton
    private lateinit var rgLanguages: RadioGroup
    private var textToSpeech: TextToSpeech? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings, container, false)

        etApiKey = root.findViewById(R.id.et_gemini_api_key)
        btnSaveApiKey = root.findViewById(R.id.btn_save_api_key)
        btnTestSpeech = root.findViewById(R.id.btn_test_speech)
        rgLanguages = root.findViewById(R.id.rg_languages)

        // Mavjud API kalitni ko'rsatish
        val currentKey = GeminiAIEngine.getApiKey(requireContext())
        if (currentKey.isNotBlank()) {
            etApiKey.setText(currentKey)
        }

        btnSaveApiKey.setOnClickListener {
            val key = etApiKey.text.toString().trim()
            GeminiAIEngine.saveApiKey(requireContext(), key)
            Toast.makeText(requireContext(), "Gemini API kaliti saqlandi! AI faollashdi.", Toast.LENGTH_SHORT).show()
        }

        // Erkak ovozi TTS
        textToSpeech = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale("uz", "UZ")
                textToSpeech?.setPitch(0.82f) // Erkak tembri
                textToSpeech?.setSpeechRate(0.98f)
            }
        }

        btnTestSpeech.setOnClickListener {
            val speech = "Assalomu alaykum! Men Support AI erkak ovozli yordamchisiman. Sizga qanday yordam beray?"
            textToSpeech?.speak(speech, TextToSpeech.QUEUE_FLUSH, null, "TEST_MALE_VOICE")
            Toast.makeText(requireContext(), "Erkak ovozi yangramoqda...", Toast.LENGTH_SHORT).show()
        }

        rgLanguages.setOnCheckedChangeListener { _, checkedId ->
            val lang = when (checkedId) {
                R.id.rb_lang_uz -> "O'zbek tili"
                R.id.rb_lang_ru -> "Русский язык"
                R.id.rb_lang_en -> "English"
                else -> "O'zbek tili"
            }
            Toast.makeText(requireContext(), "Tanlangan til: $lang", Toast.LENGTH_SHORT).show()
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
