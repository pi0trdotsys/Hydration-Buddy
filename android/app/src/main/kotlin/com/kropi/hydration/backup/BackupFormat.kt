package com.kropi.hydration.backup

import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Format kopii zapasowej Kropi: jeden plik JSON z całą zawartością DataStore
 * (łyki, historia, profil godzinowy, ustawienia) — tak samo jak Szpila trzyma
 * jeden plik z pełną kopią. Czysta logika, bez Androida, testowana na JVM.
 *
 * ```
 * { "app": "kropi", "format": 1, "createdAt": 1790000000000, "appVersion": "1.7.0",
 *   "prefs": { "goal": {"t": "int", "v": 2500}, "intakes": {"t": "string", "v": "7:20:250"} } }
 * ```
 */
object BackupFormat {
    const val APP = "kropi"
    const val VERSION = 1

    enum class Type(val id: String) {
        INT("int"), LONG("long"), FLOAT("float"), STRING("string"), BOOL("bool");

        companion object {
            fun of(id: String?): Type? = entries.firstOrNull { it.id == id }
        }
    }

    data class Entry(val name: String, val type: Type, val value: Any)

    data class Snapshot(val createdAtMs: Long, val appVersion: String, val entries: List<Entry>)

    /** Wpis z wartości DataStore albo null dla typu, którego kopia nie obsługuje. */
    fun entryOf(name: String, value: Any): Entry? = when (value) {
        is Int -> Entry(name, Type.INT, value)
        is Long -> Entry(name, Type.LONG, value)
        is Float -> Entry(name, Type.FLOAT, value)
        is String -> Entry(name, Type.STRING, value)
        is Boolean -> Entry(name, Type.BOOL, value)
        else -> null
    }

    fun encode(s: Snapshot): String {
        val prefs = JSONObject()
        s.entries.sortedBy { it.name }.forEach { e ->
            prefs.put(e.name, JSONObject().put("t", e.type.id).put("v", e.value))
        }
        return JSONObject()
            .put("app", APP)
            .put("format", VERSION)
            .put("createdAt", s.createdAtMs)
            .put("appVersion", s.appVersion)
            .put("prefs", prefs)
            .toString(2)
    }

    /** null, gdy to nie jest kopia Kropi, jest z nowszego formatu albo ma uszkodzony wpis. */
    fun decode(json: String): Snapshot? = runCatching {
        val root = JSONObject(json)
        if (root.optString("app") != APP) return null
        if (root.optInt("format", 0) !in 1..VERSION) return null
        val prefs = root.getJSONObject("prefs")
        val entries = buildList {
            for (name in prefs.keys()) {
                val o = prefs.getJSONObject(name)
                val type = Type.of(o.optString("t")) ?: return null
                val value: Any = when (type) {
                    Type.INT -> o.getInt("v")
                    Type.LONG -> o.getLong("v")
                    Type.FLOAT -> o.getDouble("v").toFloat()
                    Type.STRING -> o.getString("v")
                    Type.BOOL -> o.getBoolean("v")
                }
                add(Entry(name, type, value))
            }
        }
        if (entries.isEmpty()) return null
        Snapshot(root.optLong("createdAt", 0L), root.optString("appVersion", ""), entries)
    }.getOrNull()
}

/** Nazwy plików i foldery — te same zasady co w BackupStore Szpili, pod nazwą Kropi. */
object BackupRules {
    const val ROOT = "Kropi"

    /** Wygląda jak Environment.DIRECTORY_DOWNLOADS (stała tutaj, żeby reguły działały na JVM). */
    const val DOWNLOADS = "Download"

    /** Ile dziennych kopii trzymamy. Kopie zapisane ręcznie nie są nigdy kasowane automatycznie. */
    const val AUTO_KEEP = 14

    enum class Dir(val path: String) {
        MANUAL("Kopie"), AUTO("Kopie/Automatyczne"), EXPORT("Eksport"),
    }

    private val AUTO_NAME = Regex("^kropi-auto-(\\d{4}-\\d{2}-\\d{2})\\.json$")
    private val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm", Locale.US)

    /** „Download/Kropi/Kopie/" — RELATIVE_PATH w MediaStore (zawsze kończy się „/"). */
    fun relPath(dir: Dir): String = "$DOWNLOADS/$ROOT/${dir.path}/"

    fun manualName(at: LocalDateTime): String = "kropi-kopia-${at.format(STAMP)}.json"

    fun autoName(date: LocalDate): String = "kropi-auto-$date.json"

    /**
     * Dzienne kopie do skasowania, żeby zostało `keep` najnowszych. Liczą się
     * tylko nazwy z datą (cokolwiek innego w folderze zostaje); daty sortują się jak tekst.
     */
    fun autoToPrune(names: List<String>, keep: Int = AUTO_KEEP): List<String> {
        val dated = names.filter { AUTO_NAME.matches(it) }.sortedDescending()
        return if (dated.size <= keep) emptyList() else dated.drop(keep)
    }
}

/** Import historii z CSV, który Kropi umie wyeksportować od wersji 1.5 (zakładka Historia). */
object CsvHistory {
    data class Row(val epochDay: Long, val ml: Int, val goal: Int)

    private const val BOM = '\uFEFF'

    /** Nagłówek eksportu; sprawdzamy go, żeby przypadkowy plik nie udawał historii. */
    private const val HEADER_START = "data;wypite_ml;cel_ml"

    fun looksLikeCsv(text: String): Boolean =
        text.trimStart(BOM, ' ', '\n', '\r').startsWith(HEADER_START)

    /** Wiersze `RRRR-MM-DD;ml;cel;…`; błędne linie pomijamy, nie przerywamy całego importu. */
    fun parse(text: String): List<Row> {
        if (!looksLikeCsv(text)) return emptyList()
        return text.lineSequence().drop(1).mapNotNull { line ->
            val p = line.trim().split(";")
            if (p.size < 3) return@mapNotNull null
            val date = runCatching { LocalDate.parse(p[0].trim()) }.getOrNull() ?: return@mapNotNull null
            val ml = p[1].trim().toIntOrNull() ?: return@mapNotNull null
            val goal = p[2].trim().toIntOrNull() ?: return@mapNotNull null
            if (ml < 0 || goal < 0) return@mapNotNull null
            Row(date.toEpochDay(), ml, goal)
        }.toList()
    }
}
