package com.example.data.local

import androidx.room.*
import com.example.data.model.Project
import com.example.data.model.CaptionItem
import com.example.data.model.BrandKit
import com.example.data.model.ExportItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Int): Project?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Update
    suspend fun updateProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)
}

@Dao
interface CaptionDao {
    @Query("SELECT * FROM captions WHERE projectId = :projectId ORDER BY startTimeMs ASC")
    fun getCaptionsForProject(projectId: Int): Flow<List<CaptionItem>>

    @Query("SELECT * FROM captions WHERE id = :id LIMIT 1")
    suspend fun getCaptionById(id: Int): CaptionItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCaption(caption: CaptionItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCaptions(captions: List<CaptionItem>)

    @Update
    suspend fun updateCaption(caption: CaptionItem)

    @Delete
    suspend fun deleteCaption(caption: CaptionItem)

    @Query("DELETE FROM captions WHERE projectId = :projectId")
    suspend fun deleteCaptionsForProject(projectId: Int)
}

@Dao
interface BrandKitDao {
    @Query("SELECT * FROM brand_kits WHERE id = 1 LIMIT 1")
    fun getBrandKit(): Flow<BrandKit?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBrandKit(brandKit: BrandKit)
}

@Dao
interface ExportDao {
    @Query("SELECT * FROM export_history ORDER BY timestamp DESC")
    fun getAllExports(): Flow<List<ExportItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExport(export: ExportItem): Long

    @Update
    suspend fun updateExport(export: ExportItem)
}
