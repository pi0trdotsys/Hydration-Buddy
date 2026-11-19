package com.kropi.hydration.backup

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Format kopii zapasowej Kropi: jeden plik JSON z całością danych — ustawieniami
 * i licznikami z DataStore oraz łykami i celami dni z bazy. Czysta logika, bez
 * Androida, testowana na JVM.
 *
 * ```
 * { "app": "kropi", "format": 2, "createdAt": 1790000000000, "appVersion": "1.8.0",
 *   "prefs":    { "goal": {"t": "int", "v": 2500} },
 *   "intakes":  [ {"d": 20773, "m": 440, "ml": 250, "k": "WATER", "n": "po bieganiu"} ],
 *   "dayGoals": [ {"d": 20772, "g": 2500} ] }
 * ```
 *
 * Format 1 (do wersji 1.7) nie miał tablic `intakes`/`dayGoals` — łyki siedziały
 * w `prefs` jako tekst; takie pliki nadal się wczytują.
 */
object BackupFormat {
    const val APP = "kropi"
    const val VERSION = 2

    enum class Type(val id: String) {
        INT("int"), LONG("long"), FLOAT("float"), STRING("string"), BOOL("bool");

        companion object {
            fun of(id: String?): Type? = entries.firstOrNull { it.id == id }
        }
    }

    data class Entry(val name: String, val type: Type, val value: Any)

    data class IntakeRow(val epochDay: Long, val minuteOfDay: Int, val ml: Int, val drink: String, val note: String?)

    data class DayGoalRow(val epochDay: Long, val goal: Int)

    data class Snapshot(
        val createdAtMs: Long,
        val appVersion: String,
        val entries: List<Entry>,
        val intakes: List<IntakeRow> = emptyList(),
        val dayGoals: List<DayGoalRow> = emptyList(),
    )

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
            .put(
                "intakes",
                JSONArray().also { arr ->
                    s.intakes.forEach {
                        val o = JSONObject().put("d", it.epochDay).put("m", it.minuteOfDay).put("ml", it.ml).put("k", it.drink)
                        if (it.note != null) o.put("n", it.note)
                        arr.put(o)
                    }
                },
            )
            .put(
                "dayGoals",
                JSONArray().also { arr -> s.dayGoals.forEach { arr.put(JSONObject().put("d", it.epochDay).put("g", it.goal)) } },
            )
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
        val intakes = buildList {
            val arr = root.optJSONArray("intakes")
            for (i in 0 until (arr?.length() ?: 0)) {
                val o = arr!!.getJSONObject(i)
                add(IntakeRow(o.getLong("d"), o.getInt("m"), o.getInt("ml"), o.optString("k", "WATER"), o.optString("n").takeIf { o.has("n") && it.isNotEmpty() }))
            }
        }
        val dayGoals = buildList {
            val arr = root.optJSONArray("dayGoals")
            for (i in 0 until (arr?.length() ?: 0)) {
                val o = arr!!.getJSONObject(i)
                add(DayGoalRow(o.getLong("d"), o.getInt("g")))
            }
        }
        if (entries.isEmpty() && intakes.isEmpty()) return null
        Snapshot(root.optLong("createdAt", 0L), root.optString("appVersion", ""), entries, intakes, dayGoals)
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


/**
 * Eksport pojedynczych wpisów (nie sum dni): data, godzina, napój, ile wlano, ile się liczy do celu
 * i notatka. Średnik jako separator — tak samo jak w eksporcie dni, dobrze otwiera się w polskim Excelu.
 */
object CsvEntries {
    /** Nagłówek w języku aplikacji; eksport dni (import) zostaje po polsku, bo musi dać się wczytać z powrotem. */
    val HEADER: String get() = com.kropi.hydration.data.tr("data;godzina;napoj;wypite_ml;liczy_sie_ml;notatka", "date;time;drink;drunk_ml;counted_ml;note")

    fun build(intakes: List<com.kropi.hydration.data.Intake>): String = buildString {
        appendLine(HEADER)
        intakes.sortedWith(compareBy({ it.epochDay }, { it.minuteOfDay }, { it.id })).forEach { i ->
            append(LocalDate.ofEpochDay(i.epochDay)).append(';')
            append(String.format("%02d:%02d", i.hour, i.minute)).append(';')
            append(field(i.drink.label)).append(';')
            append(i.ml).append(';')
            append(i.counted).append(';')
            appendLine(field(i.note.orEmpty()))
        }
    }

    /** Pole z separatorem, cudzysłowem albo łamaniem linii bierzemy w cudzysłów (RFC 4180). */
    fun field(value: String): String =
        if (value.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value
}
