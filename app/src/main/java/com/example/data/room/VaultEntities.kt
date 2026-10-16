package com.example.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.MediaType
import com.example.data.VaultItem

@Entity(tableName = "vault_media")
data class VaultItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val encryptedFileName: String,
    val mimeType: String,
    val mediaType: String, // "PHOTO", "VIDEO", "DOCUMENT", "CONTACT", "LOCKED_APP"
    val albumName: String = "المعرض الرئيسي",
    val isFavorite: Boolean = false,
    val isTrash: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val fileSize: Long = 0L,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val isDecoy: Boolean = false,
    val extraData: String = ""
) {
    fun toVaultItem(): VaultItem {
        val type = when (mediaType) {
            "VIDEO" -> MediaType.VIDEO
            "DOCUMENT" -> MediaType.DOCUMENT
            "CONTACT" -> MediaType.CONTACT
            "LOCKED_APP" -> MediaType.LOCKED_APP
            else -> MediaType.PHOTO
        }
        return VaultItem(
            id = id,
            title = title,
            encryptedFileName = encryptedFileName,
            mimeType = mimeType,
            mediaType = type,
            albumName = albumName,
            isFavorite = isFavorite,
            isTrash = isTrash,
            dateAdded = dateAdded,
            fileSize = fileSize,
            durationMs = durationMs,
            width = width,
            height = height,
            isDecoy = isDecoy,
            extraData = extraData
        )
    }

    companion object {
        fun fromVaultItem(item: VaultItem): VaultItemEntity {
            return VaultItemEntity(
                id = item.id,
                title = item.title,
                encryptedFileName = item.encryptedFileName,
                mimeType = item.mimeType,
                mediaType = item.mediaType.name,
                albumName = item.albumName,
                isFavorite = item.isFavorite,
                isTrash = item.isTrash,
                dateAdded = item.dateAdded,
                fileSize = item.fileSize,
                durationMs = item.durationMs,
                width = item.width,
                height = item.height,
                isDecoy = item.isDecoy,
                extraData = item.extraData
            )
        }
    }
}

@Entity(tableName = "vault_albums")
data class AlbumEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String = "folder",
    val itemCount: Int = 0
)
