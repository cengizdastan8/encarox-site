package com.encarox.kodo.data.repository

import com.encarox.kodo.data.db.CodeDao
import com.encarox.kodo.data.db.CodeEntity
import com.encarox.kodo.data.extract.CodeExtractor
import com.encarox.kodo.data.model.CodeItem
import com.encarox.kodo.data.model.CodeSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for captured codes. Handles extraction, de-duplication
 * and persistence so callers (SMS receiver, Gmail worker) stay thin.
 */
@Singleton
class CodeRepository @Inject constructor(
    private val dao: CodeDao
) {
    val activeCodes: Flow<List<CodeItem>> =
        dao.observeActive().map { list -> list.map { it.toModel() } }

    val archivedCodes: Flow<List<CodeItem>> =
        dao.observeArchived().map { list -> list.map { it.toModel() } }

    val unreadCount: Flow<Int> = dao.observeUnreadCount()

    /**
     * Try to extract a code from a raw message and store it.
     * Returns the stored [CodeItem], or null if no code was found or it was a duplicate.
     */
    suspend fun ingest(
        rawMessage: String,
        source: CodeSource,
        sender: String,
        account: String? = null,
        receivedAt: Long = System.currentTimeMillis(),
        externalId: String? = null
    ): CodeItem? {
        val result = CodeExtractor.extract(rawMessage, senderHint = sender) ?: return null
        val dedupeKey = dedupeKey(source, result.code, externalId ?: sender, receivedAt)

        if (dao.findByDedupeKey(dedupeKey) != null) return null

        val item = CodeItem(
            code = result.code,
            source = source,
            sender = sender,
            account = account,
            service = result.service,
            rawMessage = rawMessage,
            receivedAt = receivedAt
        )
        val id = dao.insertIgnoring(CodeEntity.from(item, dedupeKey))
        if (id <= 0) return null
        return item.copy(id = id)
    }

    suspend fun markRead(id: Long) = dao.markRead(id)
    suspend fun markAllRead() = dao.markAllRead()
    suspend fun archive(id: Long) = dao.archive(id)
    suspend fun delete(id: Long) = dao.delete(id)

    /** Retention cleanup — drop codes older than [days]. */
    suspend fun purgeOlderThan(days: Int) {
        if (days <= 0) return
        val cutoff = System.currentTimeMillis() - days.toLong() * 24 * 60 * 60 * 1000
        dao.deleteOlderThan(cutoff)
    }

    private fun dedupeKey(source: CodeSource, code: String, ref: String, receivedAt: Long): String {
        // For Gmail we get a stable message id in [ref]; for SMS we bucket by
        // minute so identical resend within the same minute counts as one.
        val bucket = if (source == CodeSource.GMAIL) ref else "${ref}:${receivedAt / 60000}"
        return sha1("${source.name}|$code|$bucket")
    }

    private fun sha1(input: String): String =
        MessageDigest.getInstance("SHA-1")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
