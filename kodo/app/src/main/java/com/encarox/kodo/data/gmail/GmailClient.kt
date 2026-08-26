package com.encarox.kodo.data.gmail

import android.util.Base64
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.MessagePart
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper over the Gmail REST API for reading recent verification emails.
 * All work is on-device; nothing is uploaded anywhere.
 */
@Singleton
class GmailClient @Inject constructor(
    private val authManager: GmailAuthManager
) {
    data class GmailMessage(
        val id: String,
        val from: String,
        val subject: String,
        val body: String,
        val receivedAt: Long
    )

    // Query narrows to likely verification mail from the last day.
    private val query =
        "newer_than:1d (code OR verification OR verify OR OTP OR passcode OR " +
            "\"security code\" OR kod OR doğrulama OR dogrulama OR onay OR şifre)"

    private fun service(email: String): Gmail {
        val credential: GoogleAccountCredential = authManager.credentialFor(email)
        return Gmail.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Kodo").build()
    }

    /** Returns recent verification-looking messages for [email]. */
    fun fetchRecent(email: String, maxResults: Long = 15): List<GmailMessage> {
        val gmail = service(email)
        val list = gmail.users().messages().list("me")
            .setQ(query)
            .setMaxResults(maxResults)
            .execute()

        val ids = list.messages?.map { it.id } ?: return emptyList()
        return ids.mapNotNull { id ->
            runCatching { fetchOne(gmail, id) }.getOrNull()
        }
    }

    private fun fetchOne(gmail: Gmail, id: String): GmailMessage {
        val msg = gmail.users().messages().get("me", id).setFormat("full").execute()
        val headers = msg.payload?.headers.orEmpty()
        val from = headers.firstOrNull { it.name.equals("From", true) }?.value.orEmpty()
        val subject = headers.firstOrNull { it.name.equals("Subject", true) }?.value.orEmpty()
        val body = extractPlainText(msg.payload) ?: msg.snippet.orEmpty()
        val receivedAt = msg.internalDate ?: System.currentTimeMillis()
        return GmailMessage(
            id = id,
            from = cleanFrom(from),
            subject = subject,
            body = "$subject\n$body",
            receivedAt = receivedAt
        )
    }

    /** Depth-first search for a text/plain part, decoding base64url bodies. */
    private fun extractPlainText(part: MessagePart?): String? {
        if (part == null) return null
        if (part.mimeType == "text/plain") {
            part.body?.data?.let { return decode(it) }
        }
        part.parts?.forEach { child ->
            extractPlainText(child)?.let { return it }
        }
        // Fallback to any html part, stripped.
        if (part.mimeType == "text/html") {
            part.body?.data?.let { return stripHtml(decode(it)) }
        }
        return null
    }

    private fun decode(data: String): String =
        String(Base64.decode(data, Base64.URL_SAFE or Base64.NO_WRAP))

    private fun stripHtml(html: String): String =
        html.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()

    private fun cleanFrom(from: String): String {
        // "Google <no-reply@google.com>" -> "Google"
        val name = from.substringBefore("<").trim().trim('"')
        return name.ifBlank { from.substringAfter("<").substringBefore(">").ifBlank { from } }
    }
}
