package com.example.data.repository

import com.example.data.local.ProjectDao
import com.example.data.local.CaptionDao
import com.example.data.local.BrandKitDao
import com.example.data.local.ExportDao
import com.example.data.model.Project
import com.example.data.model.CaptionItem
import com.example.data.model.BrandKit
import com.example.data.model.ExportItem
import kotlinx.coroutines.flow.Flow

class CaptionRepository(
    private val projectDao: ProjectDao,
    private val captionDao: CaptionDao,
    private val brandKitDao: BrandKitDao,
    private val exportDao: ExportDao
) {
    val allProjects: Flow<List<Project>> = projectDao.getAllProjects()
    val allExports: Flow<List<ExportItem>> = exportDao.getAllExports()
    val brandKit: Flow<BrandKit?> = brandKitDao.getBrandKit()

    suspend fun getProject(id: Int): Project? = projectDao.getProjectById(id)

    suspend fun createProject(project: Project): Int {
        return projectDao.insertProject(project).toInt()
    }

    suspend fun updateProject(project: Project) {
        projectDao.updateProject(project)
    }

    suspend fun deleteProject(project: Project) {
        captionDao.deleteCaptionsForProject(project.id)
        projectDao.deleteProject(project)
    }

    fun getCaptions(projectId: Int): Flow<List<CaptionItem>> {
        return captionDao.getCaptionsForProject(projectId)
    }

    suspend fun insertCaption(caption: CaptionItem) {
        captionDao.insertCaption(caption)
    }

    suspend fun insertCaptions(captions: List<CaptionItem>) {
        captionDao.insertCaptions(captions)
    }

    suspend fun updateCaption(caption: CaptionItem) {
        captionDao.updateCaption(caption)
    }

    suspend fun deleteCaption(caption: CaptionItem) {
        captionDao.deleteCaption(caption)
    }

    suspend fun saveBrandKit(brandKit: BrandKit) {
        brandKitDao.saveBrandKit(brandKit)
    }

    suspend fun addExport(export: ExportItem): Int {
        return exportDao.insertExport(export).toInt()
    }

    suspend fun updateExport(export: ExportItem) {
        exportDao.updateExport(export)
    }
}
