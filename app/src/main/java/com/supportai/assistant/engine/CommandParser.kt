package com.supportai.assistant.engine

import java.util.regex.Pattern

/**
 * Buyruq turlari
 */
sealed class AICommand {
    data class OpenYouTube(val searchQuery: String? = null) : AICommand()
    data class SeekVideo(val targetMinute: Int, val targetSecond: Int = 0) : AICommand()
    data class OpenTelegram(val chatTarget: String? = null, val messageText: String? = null) : AICommand()
    object OpenGallery : AICommand()
    object OpenSettings : AICommand()
    object GoHome : AICommand()
    object GoBack : AICommand()
    data class TypeText(val text: String) : AICommand()
    data class Unknown(val rawText: String) : AICommand()
}

/**
 * Ko'p tilli (O'zbek, Rus, Ingliz) buyruqlarni tahlil qilish (NLP Parser)
 */
object CommandParser {

    fun parse(rawText: String): AICommand {
        val clean = rawText.lowercase().trim()

        // 1. YouTube buyruqlari
        // Masalan: "youtubeni och", "youtube och", "open youtube", "открой ютуб", "youtube da qidir..."
        if (clean.contains("youtube") || clean.contains("yutub") || clean.contains("ютуб")) {
            // Minutga o'tkazish
            val seekResult = parseSeekMinute(clean)
            if (seekResult != null) {
                return seekResult
            }

            // Qidirish so'zini ajratib olish
            val query = extractYouTubeQuery(clean)
            return AICommand.OpenYouTube(query)
        }

        // 2. Videoni minutiga o'tkazish (umumiy)
        val seekResult = parseSeekMinute(clean)
        if (seekResult != null) {
            return seekResult
        }

        // 3. Telegram buyruqlari
        // Masalan: "telegramni och", "open telegram", "открой телеграм", "telegramda yoz..."
        if (clean.contains("telegram") || clean.contains("телеграм")) {
            val (chat, message) = extractTelegramDetails(clean)
            return AICommand.OpenTelegram(chat, message)
        }

        // 4. Galereya
        if (clean.contains("galereya") || clean.contains("gallery") || clean.contains("галере")) {
            return AICommand.OpenGallery
        }

        // 5. Sozlamalar
        if (clean.contains("sozlamalar") || clean.contains("settings") || clean.contains("настройки")) {
            return AICommand.OpenSettings
        }

        // 6. Tizim amallari
        if (clean.contains("bosh sahifa") || clean.contains("домой") || clean.contains("home")) {
            return AICommand.GoHome
        }
        if (clean.contains("orqaga") || clean.contains("назад") || clean.contains("back")) {
            return AICommand.GoBack
        }

        // 7. Matn yozish
        if (clean.startsWith("yoz ") || clean.startsWith("напиши ") || clean.startsWith("type ")) {
            val text = clean.substringAfter(" ")
            return AICommand.TypeText(text)
        }

        return AICommand.Unknown(rawText)
    }

    private fun parseSeekMinute(text: String): AICommand.SeekVideo? {
        // "5 minutga o'tkaz", "3 minutiga", "на 4 минуту", "jump to 2 minutes"
        val pattern = Pattern.compile("(\\d+)\\s*(minut|минут|minute)")
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            val minute = matcher.group(1)?.toIntOrNull() ?: 0
            return AICommand.SeekVideo(targetMinute = minute)
        }
        return null
    }

    private fun extractYouTubeQuery(text: String): String? {
        val triggers = listOf(
            "youtubeni och va", "youtube ochib", "open youtube and search", "открой ютуб и найди",
            "youtubeda qidir", "youtube da", "yutubda", "qidir"
        )
        for (trigger in triggers) {
            if (text.contains(trigger)) {
                val query = text.substringAfter(trigger)
                    .replace("haqida", "")
                    .replace("video qilib ber", "")
                    .replace("video qo'y", "")
                    .replace("videoni qo'y", "")
                    .replace("qo'y", "")
                    .replace("play", "")
                    .trim()
                if (query.isNotEmpty()) return query
            }
        }
        return null
    }

    private fun extractTelegramDetails(text: String): Pair<String?, String?> {
        // "telegramda Aliga salom deb yoz"
        var chat: String? = null
        var message: String? = null

        if (text.contains("ga ") && text.contains("yoz")) {
            val part = text.substringAfter("telegramda ").substringBefore("ga ")
            if (part.isNotBlank()) chat = part.trim()
            val msgPart = text.substringAfter("ga ").replace("yoz", "").replace("deb", "").trim()
            if (msgPart.isNotBlank()) message = msgPart
        }
        return Pair(chat, message)
    }
}
