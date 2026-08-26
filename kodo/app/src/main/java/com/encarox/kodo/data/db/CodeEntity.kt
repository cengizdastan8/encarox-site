package com.encarox.kodo.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.encarox.kodo.data.model.CodeItem
import com.encarox.kodo.data.model.CodeSource

@Entity(
    tableName = "codes",
    indices = [Index(value = ["dedupeKey"], unique = true), Index(value = ["receivedAt"])]
)
data class CodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val source: String,
    val sender: String,
    val account: String?,
    val service: String?,
    val rawMessage: String,
    val receivedAt: Long,
    val isRead: Boolean = false,
    val isArchived: Boolean = false,
    /** Stable key used to avoid inserting the same code twice. */
    val dedupeKey: String
) {
    fun toModel() = CodeItem(
        id = id,
        code = code,
        source = CodeSource.fromName(source),
        sender = sender,
        account = account,
        service = service,
        rawMessage = rawMessage,
        receivedAt = receivedAt,
        isRead = isRead,
        isArchived = isArchived
    )

    companion object {
        fun from(item: CodeItem, dedupeKey: String) = CodeEntity(
            id = item.id,
            code = item.code,
            source = item.source.name,
            sender = item.sender,
            account = item.account,
            service = item.service,
            rawMessage = item.rawMessage,
            receivedAt = item.receivedAt,
            isRead = item.isRead,
            isArchived = item.isArchived,
            dedupeKey = dedupeKey
        )
    }
}
