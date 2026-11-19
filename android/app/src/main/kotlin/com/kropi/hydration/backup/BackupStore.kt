package com.kropi.hydration.backup

import com.kropi.hydration.R
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.kropi.hydration.backup.BackupRules.Dir
import com.kropi.hydration.data.HydrationRepository
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

/**
 * Pliki kopii, do których użytkownik naprawdę może dojść (jak w Szpili):
 *
 * ```
 * Download/Kropi/Kopie/                 kopie zapisane ręcznie  (kropi-kopia-2026-11-16-2136.json)
 * Download/Kropi/Kopie/Automatyczne/    kopia dzienna           (kropi-auto-2026-11-16.json), ostatnie 14
 * ```
 *
 * Android 10+ pisze przez MediaStore (bez żadnych uprawnień; ta instalacja widzi,
 * nadpisuje i kasuje tylko swoje pliki). Starsze wersje używają katalogu
 * zewnętrznego aplikacji z tymi samymi podfolderami.
 *
 * Uwaga: po odinstalowaniu i ponownej instalacji nowa instalacja NIE widzi plików
 * poprzedniej (inny właściciel w MediaStore) — dlatego Ustawienia mają też
 * „Przywróć z pliku…" przez systemowy wybór pliku.
 */
object BackupStore {
    private const val PREFS = "kropi_backup"
    private const val KEY_LAST_AUTO = "last_auto_backup"
    private const val KEY_AUTO_ENABLED = "auto_enabled"

    /** Kopia widoczna na liście w Ustawieniach. */
    data class Entry(val id: String, val name: String, val auto: Boolean, val size: Long, val modifiedMs: Long)

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun autoEnabled(c: Context): Boolean = prefs(c).getBoolean(KEY_AUTO_ENABLED, true)

    fun setAutoEnabled(c: Context, enabled: Boolean) {
        prefs(c).edit().putBoolean(KEY_AUTO_ENABLED, enabled).apply()
    }

    /** Zapisuje kopię ręczną i zwraca czytelne miejsce zapisu. */
    suspend fun saveManual(c: Context, repo: HydrationRepository): String {
        val json = BackupFormat.encode(repo.snapshot())
        return write(c, Dir.MANUAL, BackupRules.manualName(LocalDateTime.now()), json)
    }

    /** Raz na dobę (przy dowolnym ticku workera): kropi-auto-<data>.json, potem przycięcie do 14. */
    suspend fun autoBackupIfDue(c: Context, repo: HydrationRepository) {
        if (!autoEnabled(c)) return
        val today = LocalDate.now()
        if (prefs(c).getString(KEY_LAST_AUTO, "") == today.toString()) return
        runCatching {
            write(c, Dir.AUTO, BackupRules.autoName(today), BackupFormat.encode(repo.snapshot()))
            prefs(c).edit().putString(KEY_LAST_AUTO, today.toString()).apply()
            val names = list(c).filter { it.auto }.map { it.name }
            val drop = BackupRules.autoToPrune(names)
            list(c).filter { it.auto && it.name in drop }.forEach { delete(c, it.id) }
        }
    }

    private fun write(c: Context, dir: Dir, name: String, text: String): String {
        val data = text.toByteArray(Charsets.UTF_8)
        val shown = "${c.getString(R.string.backup_downloads_folder)}/${BackupRules.ROOT}/${dir.path}/$name"
        if (Build.VERSION.SDK_INT >= 29) {
            val cr = c.contentResolver
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val rel = BackupRules.relPath(dir)
            val uri = findOwn(c, collection, name, rel) ?: cr.insert(
                collection,
                ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                    put(MediaStore.Downloads.RELATIVE_PATH, rel)
                },
            ) ?: error("MediaStore insert failed")
            cr.openOutputStream(uri, "wt").use { out ->
                checkNotNull(out) { "No output stream" }.write(data)
            }
            return shown
        }
        val f = File(legacyDir(c, dir), name)
        f.writeBytes(data)
        return f.absolutePath
    }

    private fun legacyDir(c: Context, dir: Dir): File {
        val f = File(File(c.getExternalFilesDir(null), BackupRules.ROOT), dir.path)
        check(f.exists() || f.mkdirs()) { "Cannot create $f" }
        return f
    }

    /** Plik o tej nazwie, który utworzyła TA instalacja (cudzych MediaStore nie pokaże ani nie pozwoli zapisać). */
    private fun findOwn(c: Context, collection: Uri, name: String, rel: String): Uri? {
        val sel = "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=?"
        return runCatching {
            c.contentResolver.query(collection, arrayOf(MediaStore.Downloads._ID), sel, arrayOf(name, rel), null).use { cur ->
                if (cur != null && cur.moveToFirst()) ContentUris.withAppendedId(collection, cur.getLong(0)) else null
            }
        }.getOrNull()
    }

    /** Własne kopie, od najnowszej: ręczne i dzienne. */
    fun list(c: Context): List<Entry> {
        val out = mutableListOf<Entry>()
        if (Build.VERSION.SDK_INT >= 29) {
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val cols = arrayOf(
                MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME, MediaStore.Downloads.RELATIVE_PATH,
                MediaStore.Downloads.SIZE, MediaStore.Downloads.DATE_MODIFIED,
            )
            val autoPath = BackupRules.relPath(Dir.AUTO)
            runCatching {
                c.contentResolver.query(
                    collection, cols, "${MediaStore.Downloads.RELATIVE_PATH} IN (?, ?)",
                    arrayOf(BackupRules.relPath(Dir.MANUAL), autoPath), null,
                ).use { cur ->
                    while (cur != null && cur.moveToNext()) {
                        val name = cur.getString(1) ?: continue
                        if (!name.lowercase(Locale.ROOT).endsWith(".json")) continue
                        out += Entry(
                            id = ContentUris.withAppendedId(collection, cur.getLong(0)).toString(),
                            name = name,
                            auto = autoPath == cur.getString(2),
                            size = cur.getLong(3),
                            modifiedMs = cur.getLong(4) * 1000L,
                        )
                    }
                }
            }
        } else {
            for (d in listOf(Dir.MANUAL, Dir.AUTO)) {
                legacyDir(c, d).listFiles()?.filter { it.isFile && it.name.lowercase(Locale.ROOT).endsWith(".json") }?.forEach {
                    out += Entry(Uri.fromFile(it).toString(), it.name, d == Dir.AUTO, it.length(), it.lastModified())
                }
            }
        }
        return out.sortedByDescending { it.modifiedMs }
    }

    /** Tylko id, które sami wylistowaliśmy. */
    fun read(c: Context, id: String): String {
        require(list(c).any { it.id == id }) { "Unknown backup" }
        return readUri(c, Uri.parse(id))
    }

    /** Treść wskazanego przez użytkownika pliku (wybór systemowy) — .json albo .csv. */
    fun readUri(c: Context, uri: Uri): String {
        val stream = if (uri.scheme == "file") File(uri.path!!).inputStream() else c.contentResolver.openInputStream(uri)
        return checkNotNull(stream) { "No input stream" }.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    fun delete(c: Context, id: String): Boolean = runCatching {
        val uri = Uri.parse(id)
        if (uri.scheme == "file") File(uri.path!!).delete() else c.contentResolver.delete(uri, null, null) > 0
    }.getOrDefault(false)
}
