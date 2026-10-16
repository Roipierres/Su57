package com.example.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {

    @Query("SELECT * FROM vault_media ORDER BY dateAdded DESC")
    fun getAllVaultItems(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_media WHERE isTrash = 0 ORDER BY dateAdded DESC")
    fun getActiveVaultItems(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_media WHERE isFavorite = 1 AND isTrash = 0 ORDER BY dateAdded DESC")
    fun getFavoriteVaultItems(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_media WHERE id = :id LIMIT 1")
    suspend fun getVaultItemById(id: String): VaultItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(item: VaultItemEntity)

    @Update
    suspend fun updateVaultItem(item: VaultItemEntity)

    @Query("DELETE FROM vault_media WHERE id = :id")
    suspend fun deleteVaultItemById(id: String)

    // ALBUMS QUERIES
    @Query("SELECT * FROM vault_albums ORDER BY name ASC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumEntity)

    @Query("DELETE FROM vault_albums WHERE id = :id")
    suspend fun deleteAlbumById(id: String)
}
