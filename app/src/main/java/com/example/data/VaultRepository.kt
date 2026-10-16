package com.example.data

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.data.room.AlbumEntity
import com.example.data.room.VaultItemEntity
import com.example.data.room.VaultRoomDatabase
import com.example.security.VaultEncryption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class VaultRepository(private val context: Context) {

    private val db = VaultRoomDatabase.getDatabase(context)
    private val dao = db.vaultDao()

    private val vaultDir: File = File(context.filesDir, "vault_encrypted").apply { if (!exists()) mkdirs() }
    private val tempDir: File = File(context.cacheDir, "vault_temp").apply { if (!exists()) mkdirs() }

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val _items = MutableStateFlow<List<VaultItem>>(emptyList())
    val items: StateFlow<List<VaultItem>> = _items.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    init {
        // Observe Room DB entities reactively
        repositoryScope.launch {
            try {
                dao.getAllVaultItems().collect { entities ->
                    _items.value = entities.map { it.toVaultItem() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        repositoryScope.launch {
            try {
                if (dao.getAlbumCount() == 0) {
                    val defaultAlbums = listOf(
                        AlbumEntity("main", "المعرض الرئيسي", "photo_library", 0),
                        AlbumEntity("private_docs", "وثائق خاصة", "folder_special", 0),
                        AlbumEntity("personal_videos", "فيديوهات شخصية", "video_library", 0)
                    )
                    defaultAlbums.forEach { dao.insertAlbum(it) }
                }

                dao.getAllAlbums().collect { albumEntities ->
                    _albums.value = albumEntities.map { Album(it.id, it.name, it.icon, it.itemCount) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Imports a media URI, encrypts bytes using AES-256, and saves metadata into Room Database.
     */
    suspend fun importMediaUri(
        uri: Uri,
        mediaType: MediaType,
        albumName: String = "المعرض الرئيسي",
        userPin: String? = null
    ): VaultItem? = withContext(Dispatchers.IO) {
        val itemId = UUID.randomUUID().toString()
        val encFileName = "enc_$itemId.dat"
        val destEncryptedFile = File(vaultDir, encFileName)

        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            // AES-256 Stream Encryption
            val passcode = if (!userPin.isNullOrEmpty()) userPin else "6789"
            val success = VaultEncryption.encryptStreamToFile(context, inputStream, destEncryptedFile, passcode)
            inputStream.close()

            if (!success) return@withContext null

            val mimeType = context.contentResolver.getType(uri) ?: if (mediaType == MediaType.PHOTO) "image/jpeg" else "video/mp4"
            val fileSize = destEncryptedFile.length()

            val newItem = VaultItem(
                id = itemId,
                title = "محمي_${System.currentTimeMillis() % 10000}",
                encryptedFileName = encFileName,
                mimeType = mimeType,
                mediaType = mediaType,
                albumName = albumName,
                fileSize = fileSize,
                dateAdded = System.currentTimeMillis()
            )

            // Insert into Room SQLite Database
            dao.insertVaultItem(VaultItemEntity.fromVaultItem(newItem))
            updateAlbumCounts()
            newItem
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun importRawFile(
        file: File,
        mediaType: MediaType,
        albumName: String = "المعرض الرئيسي",
        userPin: String? = null
    ): VaultItem? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        val itemId = UUID.randomUUID().toString()
        val encFileName = "enc_$itemId.dat"
        val destEncryptedFile = File(vaultDir, encFileName)

        try {
            val inputStream = file.inputStream()
            val passcode = if (!userPin.isNullOrEmpty()) userPin else "6789"
            val success = VaultEncryption.encryptStreamToFile(context, inputStream, destEncryptedFile, passcode)
            inputStream.close()
            file.delete()

            if (!success) return@withContext null

            val mimeType = if (mediaType == MediaType.PHOTO) "image/jpeg" else "video/mp4"
            val newItem = VaultItem(
                id = itemId,
                title = "كاميرا_${System.currentTimeMillis() % 10000}",
                encryptedFileName = encFileName,
                mimeType = mimeType,
                mediaType = mediaType,
                albumName = albumName,
                fileSize = destEncryptedFile.length(),
                dateAdded = System.currentTimeMillis()
            )

            // Insert into Room
            dao.insertVaultItem(VaultItemEntity.fromVaultItem(newItem))
            updateAlbumCounts()
            newItem
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getDecryptedTempFile(item: VaultItem, userPin: String? = null): File? = withContext(Dispatchers.IO) {
        val encFile = File(vaultDir, item.encryptedFileName)
        if (!encFile.exists()) return@withContext null

        val ext = if (item.mediaType == MediaType.PHOTO) ".jpg" else ".mp4"
        val tempFile = File(tempDir, "temp_${item.id}$ext")

        if (tempFile.exists() && tempFile.length() > 0) {
            return@withContext tempFile
        }

        val passcode = if (!userPin.isNullOrEmpty()) userPin else "6789"
        val success = VaultEncryption.decryptToTempFile(context, encFile, tempFile, passcode)
        if (success) tempFile else null
    }

    suspend fun exportToPublicDownloads(item: VaultItem, userPin: String? = null): Boolean = withContext(Dispatchers.IO) {
        val encFile = File(vaultDir, item.encryptedFileName)
        if (!encFile.exists()) return@withContext false

        val ext = if (item.mediaType == MediaType.PHOTO) ".jpg" else ".mp4"
        val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val exportFile = File(publicDir, "Vault_Exported_${item.title}$ext")

        try {
            FileOutputStream(exportFile).use { fos ->
                val passcode = if (!userPin.isNullOrEmpty()) userPin else "6789"
                val success = VaultEncryption.decryptToOutputStream(context, encFile, fos, passcode)
                if (success) true else false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun toggleFavorite(itemId: String) {
        repositoryScope.launch {
            val entity = dao.getVaultItemById(itemId)
            if (entity != null) {
                dao.updateVaultItem(entity.copy(isFavorite = !entity.isFavorite))
            }
        }
    }

    fun moveToTrash(itemId: String) {
        repositoryScope.launch {
            val entity = dao.getVaultItemById(itemId)
            if (entity != null) {
                dao.updateVaultItem(entity.copy(isTrash = true))
                updateAlbumCounts()
            }
        }
    }

    fun restoreFromTrash(itemId: String) {
        repositoryScope.launch {
            val entity = dao.getVaultItemById(itemId)
            if (entity != null) {
                dao.updateVaultItem(entity.copy(isTrash = false))
                updateAlbumCounts()
            }
        }
    }

    fun permanentlyDelete(itemId: String) {
        repositoryScope.launch {
            val entity = dao.getVaultItemById(itemId)
            if (entity != null) {
                val encFile = File(vaultDir, entity.encryptedFileName)
                if (encFile.exists()) encFile.delete()

                val ext = if (entity.mediaType == "PHOTO") ".jpg" else ".mp4"
                val tempFile = File(tempDir, "temp_${entity.id}$ext")
                if (tempFile.exists()) tempFile.delete()

                dao.deleteVaultItemById(itemId)
                updateAlbumCounts()
            }
        }
    }

    fun createAlbum(name: String, icon: String = "folder") {
        if (name.isBlank()) return
        repositoryScope.launch {
            val newAlbum = AlbumEntity(UUID.randomUUID().toString(), name.trim(), icon, 0)
            dao.insertAlbum(newAlbum)
        }
    }

    fun moveToAlbum(itemId: String, albumName: String) {
        repositoryScope.launch {
            val entity = dao.getVaultItemById(itemId)
            if (entity != null) {
                dao.updateVaultItem(entity.copy(albumName = albumName))
                updateAlbumCounts()
            }
        }
    }

    private suspend fun updateAlbumCounts() {
        _albums.value.forEach { album ->
            val count = _items.value.count { !it.isTrash && it.albumName == album.name }
            dao.insertAlbum(AlbumEntity(album.id, album.name, album.icon, count))
        }
    }

    fun clearTempCache() {
        try {
            tempDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addVaultItemDirect(item: VaultItem) = withContext(Dispatchers.IO) {
        dao.insertVaultItem(VaultItemEntity.fromVaultItem(item))
        updateAlbumCounts()
    }

    suspend fun deleteVaultItem(itemId: String) = withContext(Dispatchers.IO) {
        permanentlyDelete(itemId)
    }
}
