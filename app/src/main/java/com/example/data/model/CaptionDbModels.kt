package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val videoDuration: Int, // in seconds
    val videoSizeMb: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val sourceLanguage: String = "English",
    val viralScore: Int = 78,
    val engagementScore: Int = 82,
    val generatedTitle: String = "",
    val generatedHashtags: String = ""
)

@Entity(tableName = "captions")
data class CaptionItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val styleCategory: String = "Neon",
    val textColor: String = "#FFFFFF",
    val fontSize: Int = 24,
    val fontFamilyName: String = "Inter",
    val glowColor: String = "#FF007F",
    val hasGlow: Boolean = true,
    val shadowColor: String = "#000000",
    val outlineColor: String = "#000000",
    val outlineWidth: Int = 4,
    val bgStyle: String = "None", // None, Fully Rounded, Semi-Transparent, Outlined Rectangle
    val bgHexColor: String = "#000000",
    val bgOpacity: Float = 0.5f,
    val xOffset: Float = 0f, // Center relative offset
    val yOffset: Float = 120f, // vertical baseline offset standard
    val scale: Float = 1.0f,
    val rotation: Float = 0f,
    val speakerId: String = "Speaker A"
)

@Entity(tableName = "brand_kits")
data class BrandKit(
    @PrimaryKey val id: Int = 1, // Single active brand kit
    val primaryColorHex: String = "#FF007F",
    val secondaryColorHex: String = "#39FF14",
    val accentColorHex: String = "#00F0FF",
    val watermarkText: String = "CaptionX AI",
    val watermarkEnabled: Boolean = false,
    val defaultFont: String = "Inter",
    val defaultStyleCategory: String = "Neon"
)

@Entity(tableName = "export_history")
data class ExportItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val projectTitle: String,
    val format: String, // MP4, MOV, WEBM
    val resolution: String, // 1080P, 4K
    val timestamp: Long = System.currentTimeMillis(),
    val fileSizeMb: Double,
    val status: String // "Completed", "In Queue", "Processing"
)
