package com.kmb.downloader.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: Long): DownloadItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(item: DownloadItem): Long

    @Update
    suspend fun updateDownload(item: DownloadItem)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownload(id: Long)

    @Query("DELETE FROM downloads")
    suspend fun clearDownloads()
}

@Dao
interface SavedLinkDao {
    @Query("SELECT * FROM saved_links ORDER BY detectedAt DESC")
    fun getAllSaved(): Flow<List<SavedLink>>

    @Query("SELECT * FROM saved_links WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): SavedLink?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(link: SavedLink): Long

    @Update
    suspend fun update(link: SavedLink)

    @Query("DELETE FROM saved_links WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM saved_links")
    suspend fun clearAll()
}
