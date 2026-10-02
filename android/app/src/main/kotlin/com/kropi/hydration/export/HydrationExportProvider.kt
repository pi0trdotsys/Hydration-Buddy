package com.kropi.hydration.export

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.kropi.hydration.data.HydrationRepository
import kotlinx.coroutines.runBlocking

/**
 * Tylko-do-odczytu provider dzisiejszego nawodnienia i ostatnich dni.
 * Dostęp: `com.kropi.hydration.permission.READ_HYDRATION` (signature).
 */
class HydrationExportProvider : ContentProvider() {

    private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(HydrationExportContract.AUTHORITY, HydrationExportContract.PATH_TODAY, MATCH_TODAY)
        addURI(HydrationExportContract.AUTHORITY, HydrationExportContract.PATH_DAYS, MATCH_DAYS)
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        val match = matcher.match(uri)
        if (match == UriMatcher.NO_MATCH) return null
        val ctx = context ?: return null

        // current() domyka poprzedni dzień, więc po północy dostajemy już nowy, pusty dzień.
        val state = runBlocking { HydrationRepository(ctx).current() }
        val today = HydrationRepository.exportToday(state)
        val rows = when (match) {
            MATCH_TODAY -> listOf(HydrationExportRows.todayRow(today))
            else -> HydrationExportRows.daysRows(today, state.records)
        }

        val cursor = MatrixCursor(HydrationExportContract.COLUMNS, rows.size)
        rows.forEach { cursor.addRow(it.toCursorRow()) }
        cursor.setNotificationUri(ctx.contentResolver, uri)
        return cursor
    }

    override fun getType(uri: Uri): String? = when (matcher.match(uri)) {
        MATCH_TODAY -> "vnd.android.cursor.item/vnd.${HydrationExportContract.AUTHORITY}.day"
        MATCH_DAYS -> "vnd.android.cursor.dir/vnd.${HydrationExportContract.AUTHORITY}.day"
        else -> null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("Kropi export is read-only")

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("Kropi export is read-only")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("Kropi export is read-only")

    private companion object {
        const val MATCH_TODAY = 1
        const val MATCH_DAYS = 2
    }
}
