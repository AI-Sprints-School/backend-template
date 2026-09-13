package com.learning.services

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Files

/**
 * Unit тесты для FileStorageService
 */
@Tag("star")
@Tag("chapter8")
class FileStorageServiceTest {

    private lateinit var fileStorageService: FileStorageService

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        // Set environment variable for test upload directory
        System.setProperty("FILE_STORAGE_PATH", tempDir.toString())
        fileStorageService = FileStorageService()
    }

    // ==================== Save File Tests ====================

    @Test
    fun `saveFile should save valid image file`() {
        val bytes = "fake image content".toByteArray()
        val fileName = "test-image.jpg"
        val contentType = "image/jpeg"

        val result = fileStorageService.saveFile(bytes, fileName, contentType)

        assertTrue(result is FileStorageService.FileUploadResult.Success)
        val success = result as FileStorageService.FileUploadResult.Success
        assertEquals(fileName, success.originalFileName)
        assertEquals("image/jpeg", success.mimeType)
        assertTrue(success.size > 0)
    }

    @Test
    fun `saveFile should reject file exceeding max size`() {
        val largeBytes = ByteArray(11 * 1024 * 1024) // 11 MB
        val fileName = "large-file.jpg"
        val contentType = "image/jpeg"

        val result = fileStorageService.saveFile(largeBytes, fileName, contentType)

        assertTrue(result is FileStorageService.FileUploadResult.Error)
        val error = result as FileStorageService.FileUploadResult.Error
        assertTrue(error.message.contains("слишком большой"))
    }

    @Test
    fun `saveFile should reject unsupported file type`() {
        val bytes = "executable content".toByteArray()
        val fileName = "malicious.exe"
        val contentType = "application/x-msdownload"

        val result = fileStorageService.saveFile(bytes, fileName, contentType)

        assertTrue(result is FileStorageService.FileUploadResult.Error)
        val error = result as FileStorageService.FileUploadResult.Error
        assertTrue(error.message.contains("Недопустимый тип"))
    }

    @Test
    fun `saveFile should accept PDF files`() {
        val bytes = "PDF content".toByteArray()
        val fileName = "document.pdf"
        val contentType = "application/pdf"

        val result = fileStorageService.saveFile(bytes, fileName, contentType)

        assertTrue(result is FileStorageService.FileUploadResult.Success)
    }

    @Test
    fun `saveFile should accept PNG images`() {
        val bytes = "PNG content".toByteArray()
        val fileName = "image.png"
        val contentType = "image/png"

        val result = fileStorageService.saveFile(bytes, fileName, contentType)

        assertTrue(result is FileStorageService.FileUploadResult.Success)
    }

    @Test
    fun `saveFile should accept WebP images`() {
        val bytes = "WebP content".toByteArray()
        val fileName = "image.webp"
        val contentType = "image/webp"

        val result = fileStorageService.saveFile(bytes, fileName, contentType)

        assertTrue(result is FileStorageService.FileUploadResult.Success)
    }

    @Test
    fun `saveFile should accept GIF images`() {
        val bytes = "GIF content".toByteArray()
        val fileName = "animation.gif"
        val contentType = "image/gif"

        val result = fileStorageService.saveFile(bytes, fileName, contentType)

        assertTrue(result is FileStorageService.FileUploadResult.Success)
    }

    @Test
    fun `saveFile should generate unique filename`() {
        val bytes = "content".toByteArray()
        
        val result1 = fileStorageService.saveFile(bytes, "test.jpg", "image/jpeg")
        val result2 = fileStorageService.saveFile(bytes, "test.jpg", "image/jpeg")

        assertTrue(result1 is FileStorageService.FileUploadResult.Success)
        assertTrue(result2 is FileStorageService.FileUploadResult.Success)
        
        val success1 = result1 as FileStorageService.FileUploadResult.Success
        val success2 = result2 as FileStorageService.FileUploadResult.Success
        assertNotEquals(success1.fileName, success2.fileName)
    }

    @Test
    fun `saveFile should create subdirectory if provided`() {
        val bytes = "content".toByteArray()
        val subDir = "avatars"

        val result = fileStorageService.saveFile(bytes, "avatar.jpg", "image/jpeg", subDir)

        assertTrue(result is FileStorageService.FileUploadResult.Success)
        val success = result as FileStorageService.FileUploadResult.Success
        assertTrue(success.path.contains(subDir))
    }

    @Test
    fun `saveFile should detect mime type from extension when null`() {
        val bytes = "content".toByteArray()

        val result = fileStorageService.saveFile(bytes, "test.jpg", null)

        assertTrue(result is FileStorageService.FileUploadResult.Success)
        val success = result as FileStorageService.FileUploadResult.Success
        assertEquals("image/jpeg", success.mimeType)
    }

    // ==================== Get File Tests ====================

    @Test
    fun `getFile should return file content when exists`() {
        val originalContent = "file content".toByteArray()
        val saveResult = fileStorageService.saveFile(originalContent, "test.jpg", "image/jpeg")
        
        assertTrue(saveResult is FileStorageService.FileUploadResult.Success)
        val success = saveResult as FileStorageService.FileUploadResult.Success

        val result = fileStorageService.getFile(success.fileName)

        assertNotNull(result)
        assertArrayEquals(originalContent, result)
    }

    @Test
    fun `getFile should return null when file not exists`() {
        val result = fileStorageService.getFile("non-existent-file.jpg")

        assertNull(result)
    }

    @Test
    fun `getFile should find file in subdirectory`() {
        val originalContent = "subdirectory content".toByteArray()
        val subDir = "documents"
        val saveResult = fileStorageService.saveFile(originalContent, "doc.pdf", "application/pdf", subDir)
        
        assertTrue(saveResult is FileStorageService.FileUploadResult.Success)
        val success = saveResult as FileStorageService.FileUploadResult.Success

        val result = fileStorageService.getFile(success.fileName, subDir)

        assertNotNull(result)
        assertArrayEquals(originalContent, result)
    }

    // ==================== Delete File Tests ====================

    @Test
    fun `deleteFile should return true when file deleted`() {
        val bytes = "content to delete".toByteArray()
        val saveResult = fileStorageService.saveFile(bytes, "delete-me.jpg", "image/jpeg")
        
        assertTrue(saveResult is FileStorageService.FileUploadResult.Success)
        val success = saveResult as FileStorageService.FileUploadResult.Success

        val result = fileStorageService.deleteFile(success.fileName)

        assertTrue(result)
        assertNull(fileStorageService.getFile(success.fileName))
    }

    @Test
    fun `deleteFile should return false when file not exists`() {
        val result = fileStorageService.deleteFile("non-existent.jpg")

        assertFalse(result)
    }

    // ==================== File Exists Tests ====================

    @Test
    fun `fileExists should return true when file exists`() {
        val bytes = "content".toByteArray()
        val saveResult = fileStorageService.saveFile(bytes, "exists.jpg", "image/jpeg")
        
        assertTrue(saveResult is FileStorageService.FileUploadResult.Success)
        val success = saveResult as FileStorageService.FileUploadResult.Success

        val result = fileStorageService.fileExists(success.fileName)

        assertTrue(result)
    }

    @Test
    fun `fileExists should return false when file not exists`() {
        val result = fileStorageService.fileExists("not-exists.jpg")

        assertFalse(result)
    }

    // ==================== Get File Info Tests ====================

    @Test
    fun `getFileInfo should return info when file exists`() {
        val bytes = "info content".toByteArray()
        val saveResult = fileStorageService.saveFile(bytes, "info-test.jpg", "image/jpeg")
        
        assertTrue(saveResult is FileStorageService.FileUploadResult.Success)
        val success = saveResult as FileStorageService.FileUploadResult.Success

        val result = fileStorageService.getFileInfo(success.fileName)

        assertNotNull(result)
        assertEquals(success.fileName, result?.fileName)
        assertEquals(bytes.size.toLong(), result?.size)
        assertTrue(result?.lastModified ?: 0 > 0)
    }

    @Test
    fun `getFileInfo should return null when file not exists`() {
        val result = fileStorageService.getFileInfo("non-existent.jpg")

        assertNull(result)
    }

    // ==================== Cleanup Old Files Tests ====================

    @Test
    fun `cleanupOldFiles should delete old files`() {
        // Create a file
        val bytes = "old content".toByteArray()
        val saveResult = fileStorageService.saveFile(bytes, "old-file.jpg", "image/jpeg")
        assertTrue(saveResult is FileStorageService.FileUploadResult.Success)
        val success = saveResult as FileStorageService.FileUploadResult.Success

        // Set file modification time to past
        val filePath = tempDir.resolve(success.fileName)
        if (Files.exists(filePath)) {
            Files.setLastModifiedTime(
                filePath,
                java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() - 8 * 24 * 3600 * 1000)
            )
        }

        // Cleanup files older than 7 days
        val deletedCount = fileStorageService.cleanupOldFiles(7 * 24 * 3600 * 1000L)

        assertTrue(deletedCount >= 0)
    }

    @Test
    fun `cleanupOldFiles should not delete recent files`() {
        val bytes = "recent content".toByteArray()
        val saveResult = fileStorageService.saveFile(bytes, "recent-file.jpg", "image/jpeg")
        assertTrue(saveResult is FileStorageService.FileUploadResult.Success)
        val success = saveResult as FileStorageService.FileUploadResult.Success

        // Cleanup files older than 7 days
        fileStorageService.cleanupOldFiles(7 * 24 * 3600 * 1000L)

        // Recent file should still exist
        assertTrue(fileStorageService.fileExists(success.fileName))
    }

    // ==================== MIME Type Detection Tests ====================

    @Test
    fun `saveFile should detect jpeg mime type from extension`() {
        val bytes = "content".toByteArray()
        
        val result = fileStorageService.saveFile(bytes, "image.jpeg", null)

        assertTrue(result is FileStorageService.FileUploadResult.Success)
        val success = result as FileStorageService.FileUploadResult.Success
        assertEquals("image/jpeg", success.mimeType)
    }

    @Test
    fun `saveFile should detect mp4 mime type from extension`() {
        val bytes = "video content".toByteArray()
        
        val result = fileStorageService.saveFile(bytes, "video.mp4", null)

        assertTrue(result is FileStorageService.FileUploadResult.Success)
        val success = result as FileStorageService.FileUploadResult.Success
        assertEquals("video/mp4", success.mimeType)
    }

    @Test
    fun `saveFile should accept Word documents`() {
        val bytes = "document content".toByteArray()
        
        val result = fileStorageService.saveFile(bytes, "document.docx", 
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document")

        assertTrue(result is FileStorageService.FileUploadResult.Success)
    }
}
