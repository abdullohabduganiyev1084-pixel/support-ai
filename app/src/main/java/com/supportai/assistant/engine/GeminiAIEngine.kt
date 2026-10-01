package com.supportai.assistant.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GeminiAIEngine {

    private const val TAG = "GeminiAIEngine"
    private const val PREFS_NAME = "support_ai_prefs"
    private const val KEY_API_KEY = "gemini_api_key"

    fun getApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_API_KEY, "") ?: ""
    }

    fun saveApiKey(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_API_KEY, key.trim()).apply()
    }

    /**
     * Foydalanuvchi aytgan gapni Gemini AI ga yuborib, JSON harakat va o'zbekcha javob olish
     */
    suspend fun processVoiceCommand(context: Context, userSpeech: String): Pair<AICommand, String> {
        val apiKey = getApiKey(context)

        // Agar API key kiritilmagan bo'lsa, lokal tezkor tahlilchidan foydalanish
        if (apiKey.isBlank()) {
            val localCmd = CommandParser.parse(userSpeech)
            val localResp = getLocalResponse(localCmd)
            return Pair(localCmd, localResp)
        }

        return withContext(Dispatchers.IO) {
            try {
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.doOutput = true
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                val systemPrompt = """
                Siz Android telefonini to'liq boshqaruvchi 'Support AI' ovozli yordamchisisiz.
                Foydalanuvchi o'zbek, rus yoki ingliz tilida telefonini boshqarishni so'raydi.
                Qat'iy faqat quyidagi JSON formatida javob bering, ortiqcha belgi va markdown yozmang:
                {
                  "action": "youtube" | "seek_video" | "telegram" | "gallery" | "settings" | "launch_app" | "system",
                  "target": "ochiladigan ilova yoki chat nomi",
                  "query": "qidiruv matni yoki minut soni",
                  "speech": "O'zbek tilidagi erkak ovozi uchun qisqa, jiddiy va tabiiy javob (masalan: 'YouTubeni ochdim, qaysi videoni qo'yaylik?')"
                }
                """.trimIndent()

                val requestJson = JSONObject().apply {
                    val contents = org.json.JSONArray().apply {
                        val part = JSONObject().apply {
                            val parts = org.json.JSONArray().apply {
                                put(JSONObject().put("text", "$systemPrompt\nFoydalanuvchi buyrug'i: $userSpeech"))
                            }
                            put("parts", parts)
                        }
                        put(part)
                    }
                    put("contents", contents)
                }

                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(requestJson.toString())
                    writer.flush()
                }

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val jsonResp = JSONObject(responseStr)
                    val candidates = jsonResp.optJSONArray("candidates")
                    val rawText = candidates?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text") ?: ""

                    parseGeminiJson(rawText, userSpeech)
                } else {
                    Log.e(TAG, "Gemini API error code: $responseCode")
                    val fallback = CommandParser.parse(userSpeech)
                    Pair(fallback, getLocalResponse(fallback))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini request failed", e)
                val fallback = CommandParser.parse(userSpeech)
                Pair(fallback, getLocalResponse(fallback))
            }
        }
    }

    private fun parseGeminiJson(rawJson: String, originalSpeech: String): Pair<AICommand, String> {
        return try {
            val clean = rawJson.replace("```json", "").replace("```", "").trim()
            val obj = JSONObject(clean)
            val action = obj.optString("action")
            val target = obj.optString("target")
            val query = obj.optString("query")
            val speech = obj.optString("speech", "Bajarildi.")

            val command = when (action) {
                "youtube" -> AICommand.OpenYouTube(query.ifBlank { null })
                "seek_video" -> AICommand.SeekVideo(query.toIntOrNull() ?: 1)
                "telegram" -> AICommand.OpenTelegram(target.ifBlank { null }, query.ifBlank { null })
                "gallery" -> AICommand.OpenGallery
                "settings" -> AICommand.OpenSettings
                "launch_app" -> AICommand.OpenCustomApp(target)
                else -> CommandParser.parse(originalSpeech)
            }
            Pair(command, speech)
        } catch (_: Exception) {
            val fallback = CommandParser.parse(originalSpeech)
            Pair(fallback, getLocalResponse(fallback))
        }
    }

    private fun getLocalResponse(cmd: AICommand): String {
        return when (cmd) {
            is AICommand.OpenYouTube -> "YouTube ochildi. Qaysi videoga o'tkizay?"
            is AICommand.SeekVideo -> "Videoni ko'rsatilgan daqiqasiga o'tkazdim."
            is AICommand.OpenTelegram -> "Telegram ochildi. Qaysi chatga xabar yozamiz?"
            is AICommand.OpenGallery -> "Galereya ochildi. Qaysi faylni ko'ramiz?"
            is AICommand.OpenSettings -> "Telefon sozlamalari ochildi."
            is AICommand.OpenCustomApp -> "${cmd.appName} ilovasi ochildi."
            else -> "Tushundim, buyruq bajarilmoqda."
        }
    }
}
