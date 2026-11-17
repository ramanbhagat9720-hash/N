package com.example.data.ai

import com.example.domain.model.SupportedLanguage

object LanguageDetector {

    fun detectLanguage(
        utterance: String,
        preferredLanguages: List<SupportedLanguage> = emptyList(),
        fallback: SupportedLanguage = SupportedLanguage.HINDI
    ): SupportedLanguage {
        val trimmed = utterance.trim()
        if (trimmed.isEmpty()) return fallback

        // 1. Script-based detection (highest confidence)
        for (ch in trimmed) {
            when (ch.code) {
                in 0x0A00..0x0A7F -> return SupportedLanguage.PUNJABI // Gurmukhi
                in 0x0980..0x09FF -> return SupportedLanguage.BENGALI
                in 0x0A80..0x0AFF -> return SupportedLanguage.GUJARATI
                in 0x0B80..0x0BFF -> return SupportedLanguage.TAMIL
                in 0x0C00..0x0C7F -> return SupportedLanguage.TELUGU
                in 0x0C80..0x0CFF -> return SupportedLanguage.KANNADA
                in 0x0D00..0x0D7F -> return SupportedLanguage.MALAYALAM
                in 0x0600..0x06FF -> {
                    val lower = trimmed.lowercase()
                    return if (lower.contains("کٲش") || lower.contains("چُھس")) {
                        SupportedLanguage.KASHMIRI
                    } else {
                        SupportedLanguage.URDU
                    }
                }
                in 0x0900..0x097F -> {
                    // Devanagari: distinguish Marathi vs Hindi by characteristic words
                    val marathiMarkers = listOf("नमस्कार", "माझं", "आहे", "नाही", "तुमची", "मला", "बोलायचं")
                    if (marathiMarkers.any { trimmed.contains(it) }) {
                        return SupportedLanguage.MARATHI
                    }
                    return SupportedLanguage.HINDI
                }
            }
        }

        // 2. Romanized / Mixed language keyword detection
        val tokens = trimmed.lowercase()
            .replace(Regex("[^a-z\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .toSet()

        val punjabiKeywords = setOf(
            "sat", "sri", "akal", "mainu", "tuhanu", "tuhadi", "haanji",
            "gal", "karni", "si", "paji", "veere", "kiddan", "kithe", "nal", "da", "di"
        )
        if (tokens.intersect(punjabiKeywords).size >= 2 || tokens.contains("sat") && tokens.contains("akal")) {
            return SupportedLanguage.PUNJABI
        }

        val urduKeywords = setOf(
            "assalam", "alaikum", "walikum", "janab", "dastiyab", "tashrif", "shukriya", "khuda", "hafiz"
        )
        if (tokens.intersect(urduKeywords).isNotEmpty()) {
            return SupportedLanguage.URDU
        }

        val kashmiriKeywords = setOf("chhus", "chhi", "woni", "kashur", "aadaab", "tuhi")
        if (tokens.intersect(kashmiriKeywords).isNotEmpty()) {
            return SupportedLanguage.KASHMIRI
        }

        val bengaliKeywords = setOf("nomoshkar", "kemon", "achen", "ami", "apnake", "ekhon", "kotha")
        if (tokens.intersect(bengaliKeywords).size >= 2) {
            return SupportedLanguage.BENGALI
        }

        val gujaratiKeywords = setOf("kem", "cho", "maja", "hu", "tamari", "shaku", "chhu")
        if (tokens.intersect(gujaratiKeywords).size >= 2) {
            return SupportedLanguage.GUJARATI
        }

        val marathiKeywords = setOf("namaskar", "kasa", "aahe", "tumhi", "mala", "bolaycha", "majha")
        if (tokens.intersect(marathiKeywords).size >= 2) {
            return SupportedLanguage.MARATHI
        }

        val tamilKeywords = setOf("vanakkam", "epdi", "irukinga", "naan", "pesanum", "nandri")
        if (tokens.intersect(tamilKeywords).isNotEmpty()) {
            return SupportedLanguage.TAMIL
        }

        val teluguKeywords = setOf("namaskaram", "ela", "unnaru", "nenu", "matladali", "dhanyavadalu")
        if (tokens.intersect(teluguKeywords).isNotEmpty()) {
            return SupportedLanguage.TELUGU
        }

        val kannadaKeywords = setOf("namaskara", "hegiddira", "naanu", "matadabeku", "dhanyavada")
        if (tokens.intersect(kannadaKeywords).isNotEmpty()) {
            return SupportedLanguage.KANNADA
        }

        val malayalamKeywords = setOf("sugham", "njan", "samsarikkan", "nanni")
        if (tokens.intersect(malayalamKeywords).isNotEmpty()) {
            return SupportedLanguage.MALAYALAM
        }

        val hindiKeywords = setOf(
            "mujhe", "aap", "aapko", "aapka", "kya", "hai", "hain", "hoon", "main",
            "baat", "karni", "thi", "tha", "namaste", "ji", "nahi", "kar", "karna",
            "bata", "kaam", "bhejo", "samjhao", "bol", "bolo", "abhi", "office", "ghar", "se"
        )
        val hindiHits = tokens.intersect(hindiKeywords).size
        if (hindiHits >= 2 || (hindiHits >= 1 && preferredLanguages.contains(SupportedLanguage.HINDI))) {
            return SupportedLanguage.HINDI
        }

        return SupportedLanguage.ENGLISH
    }
}
