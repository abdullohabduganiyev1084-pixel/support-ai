package com.supportai.assistant.ui

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.supportai.assistant.R
import java.util.Locale

class SettingsFragment : Fragment() {

    private lateinit var rgLanguages: RadioGroup
    private lateinit var switchWakeWord: SwitchCompat
    private lateinit var btnTestSpeech: AppCompatButton
    private var textToSpeech: TextToSpeech? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings, container, false)

        rgLanguages = root.findViewById(R.id.rg_languages)
        switchWakeWord = root.findViewById(R.id.switch_wake_word)
        btnTestSpeech = root.findViewById(R.id.btn_test_speech)

        textToSpeech = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale("uz", "UZ")
            }
        }

        btnTestSpeech.setOnClickListener {
            val speechSample = "Assalomu alaykum! Men Support AI yordamchisiman. Sizga qanday yordam bera olaman?"
            textToSpeech?.speak(speechSample, TextToSpeech.QUEUE_FLUSH, null, "TEST_SPEECH")
            Toast.makeText(requireContext(), "Ovoz yangramoqda...", Toast.LENGTH_SHORT).show()
        }

        rgLanguages.setOnCheckedChangeListener { _, checkedId ->
            val langName = when (checkedId) {
                R.id.rb_lang_uz -> "O'zbek tili"
                R.id.rb_lang_ru -> "Русский язык"
                R.id.rb_lang_en -> "English"
                else -> "O'zbek tili"
            }
            Toast.makeText(requireContext(), "Tanish tili: $langName o'rnatildi", Toast.LENGTH_SHORT).show()
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
