package com.dailyflow.app.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val attachmentsDir: File by lazy {
        File(context.filesDir, "note_attachments").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    /**
     * Копирует файл из URI в приватное хранилище приложения
     * @param sourceUri URI исходного файла
     * @param noteId ID заметки, к которой прикрепляется файл
     * @return Имя файла в хранилище или null в случае ошибки
     */
    suspend fun copyFileToStorage(sourceUri: Uri, noteId: String): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
            if (inputStream == null) {
                return null
            }

            // Получаем имя файла из URI
            val fileName = getFileName(sourceUri) ?: "attachment_${System.currentTimeMillis()}"
            val sanitizedFileName = sanitizeFileName(fileName)
            
            // Создаем уникальное имя файла: noteId_originalFileName
            // Это позволяет легко извлечь оригинальное имя при отображении
            val storedFileName = "${noteId}_${sanitizedFileName}"
            val outputFile = File(attachmentsDir, storedFileName)

            inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }

            storedFileName
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Получает URI файла из хранилища
     * @param fileName Имя файла в хранилище
     * @return File объект или null если файл не найден
     */
    fun getFile(fileName: String): File? {
        val file = File(attachmentsDir, fileName)
        return if (file.exists()) file else null
    }

    /**
     * Получает URI файла из хранилища для использования в Intent
     * @param fileName Имя файла в хранилище
     * @return Uri или null если файл не найден
     */
    fun getFileUri(fileName: String): Uri? {
        val file = getFile(fileName) ?: return null
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Удаляет файл из хранилища
     * @param fileName Имя файла в хранилище
     * @return true если файл был удален, false если не найден или ошибка
     */
    fun deleteFile(fileName: String): Boolean {
        val file = File(attachmentsDir, fileName)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    /**
     * Удаляет все файлы, связанные с заметкой
     * @param noteId ID заметки
     */
    fun deleteNoteFiles(noteId: String) {
        attachmentsDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("${noteId}_")) {
                file.delete()
            }
        }
    }

    /**
     * Получает имя файла из URI
     */
    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null
        
        // Пытаемся получить имя из ContentResolver
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }
        
        // Если не получилось, пытаемся из пути
        if (fileName.isNullOrBlank()) {
            val path = uri.path
            if (path != null) {
                fileName = path.substringAfterLast('/')
            }
        }
        
        return fileName
    }

    /**
     * Очищает имя файла от недопустимых символов
     */
    private fun sanitizeFileName(fileName: String): String {
        return fileName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }

    /**
     * Получает расширение файла
     */
    private fun getFileExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot >= 0 && lastDot < fileName.length - 1) {
            fileName.substring(lastDot)
        } else {
            ""
        }
    }

    /**
     * Получает размер директории с файлами
     */
    fun getStorageSize(): Long {
        var size = 0L
        attachmentsDir.listFiles()?.forEach { file ->
            size += file.length()
        }
        return size
    }
}

