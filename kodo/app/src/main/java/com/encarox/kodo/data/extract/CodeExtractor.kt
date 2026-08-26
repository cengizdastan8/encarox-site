package com.encarox.kodo.data.extract

/**
 * Heuristic verification-code extractor.
 *
 * Kodo scans SMS and email text for one-time passwords / verification codes.
 * The goal is high precision: we would rather miss an odd code than surface a
 * random number (an order id, a price, a phone number) as if it were an OTP.
 *
 * Strategy:
 *  1. Require the message to look like a verification message (keyword gate),
 *     unless it is an extremely OTP-shaped short message.
 *  2. Find candidate tokens (4-8 digits, optionally with one dash/space group).
 *  3. Score candidates by proximity to code keywords and reject numbers that
 *     are clearly something else (years, phone numbers, money).
 */
object CodeExtractor {

    /** Keywords, EN + TR, that signal a verification context. */
    private val CONTEXT_KEYWORDS = listOf(
        // English
        "code", "verification", "verify", "otp", "one-time", "one time",
        "passcode", "pin", "security code", "confirm", "authentication",
        "auth code", "login code", "sign-in", "sign in", "2fa", "two-factor",
        // Turkish
        "kod", "doğrulama", "dogrulama", "onay", "onay kodu", "şifre", "sifre",
        "tek kullanımlık", "tek kullanimlik", "güvenlik kodu", "guvenlik kodu",
        "giriş kodu", "giris kodu", "aktivasyon", "parola"
    )

    /** Words that, next to a number, mean it is NOT an OTP. */
    private val NEGATIVE_KEYWORDS = listOf(
        "order", "sipariş", "siparis", "invoice", "fatura", "amount", "tutar",
        "\$", "€", "₺", "tl", "usd", "eur", "tracking", "kargo", "takip",
        "reference no", "referans"
    )

    // 4-8 digit codes, or grouped like 123-456 / 123 456.
    private val CANDIDATE_REGEX =
        Regex("""(?<![\d.,])(\d{3}[ -]\d{3}|\d{4,8})(?![\d.,])""")

    // A well-known service -> normalized brand name.
    private val KNOWN_SERVICES = mapOf(
        "instagram" to "Instagram", "facebook" to "Facebook", "whatsapp" to "WhatsApp",
        "google" to "Google", "gmail" to "Google", "apple" to "Apple", "icloud" to "Apple",
        "microsoft" to "Microsoft", "paypal" to "PayPal", "amazon" to "Amazon",
        "netflix" to "Netflix", "twitter" to "X", "telegram" to "Telegram",
        "discord" to "Discord", "linkedin" to "LinkedIn", "tiktok" to "TikTok",
        "binance" to "Binance", "coinbase" to "Coinbase", "steam" to "Steam",
        "github" to "GitHub", "spotify" to "Spotify", "uber" to "Uber",
        "trendyol" to "Trendyol", "getir" to "Getir", "hepsiburada" to "Hepsiburada",
        "papara" to "Papara", "ininal" to "ininal", "garanti" to "Garanti BBVA",
        "akbank" to "Akbank", "yapı kredi" to "Yapı Kredi", "ziraat" to "Ziraat",
        "iş bankası" to "İş Bankası", "enpara" to "Enpara"
    )

    data class Result(
        val code: String,
        val service: String?,
        val confidence: Int // 0..100
    )

    /**
     * Extract the most likely verification code from [text].
     * Returns null when nothing looks like a code.
     */
    fun extract(text: String, senderHint: String? = null): Result? {
        if (text.isBlank()) return null
        val lower = text.lowercase()

        val hasContext = CONTEXT_KEYWORDS.any { lower.contains(it) }
        val candidates = CANDIDATE_REGEX.findAll(text).map { it.value }.toList()
        if (candidates.isEmpty()) return null

        // If there is no verification context at all, only accept when the whole
        // message is basically just a short code (avoids false positives).
        if (!hasContext) {
            val digitsOnly = text.filter { it.isDigit() }
            val looksLikeBareCode = candidates.size == 1 &&
                digitsOnly.length in 4..8 &&
                text.trim().length <= 24
            if (!looksLikeBareCode) return null
        }

        var best: Pair<String, Int>? = null
        for (raw in candidates) {
            val normalized = raw.replace(" ", "").replace("-", "")
            if (normalized.length !in 4..8) continue
            if (isLikelyNotCode(normalized)) continue

            var score = 40
            val idx = text.indexOf(raw)
            val window = windowAround(lower, idx, raw.length, radius = 25)

            if (CONTEXT_KEYWORDS.any { window.contains(it) }) score += 40
            else if (hasContext) score += 15
            if (NEGATIVE_KEYWORDS.any { window.contains(it) }) score -= 45
            if (raw.contains(' ') || raw.contains('-')) score += 10 // grouped codes are usually OTPs
            if (normalized.length == 6) score += 10 // most common OTP length
            if (normalized.length in 4..8) score += 5

            score = score.coerceIn(0, 100)
            if (score > 0 && (best == null || score > best!!.second)) {
                best = normalized to score
            }
        }

        val chosen = best ?: return null
        if (chosen.second < 45) return null
        return Result(
            code = chosen.first,
            service = detectService(lower, senderHint),
            confidence = chosen.second
        )
    }

    private fun isLikelyNotCode(digits: String): Boolean {
        // 4-digit years (19xx / 20xx) are usually not codes.
        if (digits.length == 4) {
            val n = digits.toIntOrNull() ?: return false
            if (n in 1900..2099) return true
        }
        // All identical digits like 0000 / 111111 are unlikely real OTPs.
        if (digits.toSet().size == 1) return true
        // Phone-number-length runs.
        if (digits.length > 8) return true
        return false
    }

    private fun windowAround(text: String, index: Int, length: Int, radius: Int): String {
        if (index < 0) return text
        val start = (index - radius).coerceAtLeast(0)
        val end = (index + length + radius).coerceAtMost(text.length)
        return text.substring(start, end)
    }

    fun detectService(lowerText: String, senderHint: String?): String? {
        val hint = senderHint?.lowercase().orEmpty()
        for ((key, brand) in KNOWN_SERVICES) {
            if (lowerText.contains(key) || hint.contains(key)) return brand
        }
        // Fall back to a capitalized sender hint if it looks like a name.
        if (senderHint != null && senderHint.any { it.isLetter() } && senderHint.length <= 20) {
            return senderHint
        }
        return null
    }
}
