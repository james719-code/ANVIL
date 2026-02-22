package com.james.anvil.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Utility to help migrate data from old package versions or backups.
 */
object DatabaseMigrationHelper {
    private const val TAG = "DatabaseMigrationHelper"
    private const val DATABASE_NAME = "anvil_database"
    
    // Potential old package names based on project history
    private val OLD_PACKAGES = listOf(
        "com.bdbshs.crest.dev",
        "com.bdbshs.crest",
        "com.james.anvil.dev"
    )

    /**
     * Attempts to find a database from an old package and copy it to the current one.
     * Note: This only works if both apps are debuggable or shared UID (unlikely for different packages),
     * OR if the user manually moved the file to a reachable location.
     * 
     * However, for development purposes, we can try to look in common paths if the device allows.
     */
    fun tryMigrateFromOldPackage(context: Context): Boolean {
        val currentDbPath = context.getDatabasePath(DATABASE_NAME)
        
        for (oldPkg in OLD_PACKAGES) {
            // This is a long shot due to Android permissions, but useful for dev/rooted/specific setups
            val oldDbFile = File("/data/data/$oldPkg/databases/$DATABASE_NAME")
            if (oldDbFile.exists()) {
                try {
                    copyFile(oldDbFile, currentDbPath)
                    // Also copy the WAL/SHM files if they exist
                    copyFile(File(oldDbFile.path + "-wal"), File(currentDbPath.path + "-wal"))
                    copyFile(File(oldDbFile.path + "-shm"), File(currentDbPath.path + "-shm"))
                    
                    Log.d(TAG, "Successfully migrated database from $oldPkg")
                    return true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to copy database from $oldPkg", e)
                }
            }
        }
        return false
    }

    private fun copyFile(source: File, destination: File) {
        if (!source.exists()) return
        
        source.inputStream().use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
    
    /**
     * Checks if there's a database file in a specific external location (e.g. Downloads) 
     * that we can import.
     */
    fun importFromExternal(context: Context, sourcePath: String): Boolean {
        val currentDbPath = context.getDatabasePath(DATABASE_NAME)
        val sourceFile = File(sourcePath)
        
        return try {
            if (sourceFile.exists()) {
                // Close database connections before this!
                copyFile(sourceFile, currentDbPath)
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }
}
