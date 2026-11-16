package com.kropi.hydration.backup

import com.kropi.hydration.backup.BackupFormat.Type
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class BackupFormatTest {

    private val snapshot = BackupFormat.Snapshot(
        createdAtMs = 1_790_000_000_000L,
        appVersion = "1.7.0",
        entries = listOf(
            BackupFormat.Entry("goal", Type.INT, 2500),
            BackupFormat.Entry("last_change_epoch_ms", Type.LONG, 1_790_000_000_123L),
            BackupFormat.Entry("weight_kg", Type.FLOAT, 72.5f),
            BackupFormat.Entry("intakes", Type.STRING, "7:20:250,9:5:330"),
            BackupFormat.Entry("reminders_enabled", Type.BOOL, true),
        ),
    )

    @Test
    fun roundTripKeepsEveryTypeAndValue() {
        val decoded = BackupFormat.decode(BackupFormat.encode(snapshot))
        assertNotNull(decoded)
        assertEquals(1_790_000_000_000L, decoded!!.createdAtMs)
        assertEquals("1.7.0", decoded.appVersion)
        assertEquals(snapshot.entries.sortedBy { it.name }, decoded.entries.sortedBy { it.name })
    }

    @Test
    fun entryOfMapsDataStoreTypes() {
        assertEquals(Type.INT, BackupFormat.entryOf("a", 1)!!.type)
        assertEquals(Type.LONG, BackupFormat.entryOf("a", 1L)!!.type)
        assertEquals(Type.FLOAT, BackupFormat.entryOf("a", 1f)!!.type)
        assertEquals(Type.STRING, BackupFormat.entryOf("a", "x")!!.type)
        assertEquals(Type.BOOL, BackupFormat.entryOf("a", false)!!.type)
        assertNull(BackupFormat.entryOf("a", setOf("x")))
    }

    @Test
    fun rejectsForeignOrBrokenFiles() {
        assertNull(BackupFormat.decode("nie json"))
        assertNull(BackupFormat.decode("""{"app":"szpila","format":1,"prefs":{"a":{"t":"int","v":1}}}"""))
        assertNull(BackupFormat.decode("""{"app":"kropi","format":99,"prefs":{"a":{"t":"int","v":1}}}"""))
        assertNull(BackupFormat.decode("""{"app":"kropi","format":1,"prefs":{}}"""))
        assertNull(BackupFormat.decode("""{"app":"kropi","format":1,"prefs":{"a":{"t":"wat","v":1}}}"""))
    }

    @Test
    fun foldersAndNamesFollowSzpilaConvention() {
        assertEquals("Download/Kropi/Kopie/", BackupRules.relPath(BackupRules.Dir.MANUAL))
        assertEquals("Download/Kropi/Kopie/Automatyczne/", BackupRules.relPath(BackupRules.Dir.AUTO))
        assertEquals("kropi-kopia-2026-11-16-2136.json", BackupRules.manualName(LocalDateTime.of(2026, 11, 16, 21, 36)))
        assertEquals("kropi-auto-2026-11-16.json", BackupRules.autoName(LocalDate.of(2026, 11, 16)))
    }

    @Test
    fun pruneKeepsNewestDailyCopiesAndIgnoresOtherFiles() {
        val names = (1..20).map { "kropi-auto-2026-11-%02d.json".format(it) } + "kropi-kopia-2026-11-01-1000.json"
        val drop = BackupRules.autoToPrune(names, keep = 14)
        assertEquals(6, drop.size)
        assertTrue(drop.all { it.startsWith("kropi-auto-2026-11-0") })
        assertTrue("kropi-kopia-2026-11-01-1000.json" !in drop)
        assertTrue(BackupRules.autoToPrune(names.take(5), keep = 14).isEmpty())
    }

    @Test
    fun csvExportedByKropiIsReadBack() {
        val csv = "﻿data;wypite_ml;cel_ml;procent;cel_osiagniety\n" +
            "2026-11-14;2500;2500;100;tak\n2026-11-15;1200;2500;48;nie\nśmieci\n2026-11-16;x;2500;0;nie\n"
        assertTrue(CsvHistory.looksLikeCsv(csv))
        val rows = CsvHistory.parse(csv)
        assertEquals(2, rows.size)
        assertEquals(CsvHistory.Row(LocalDate.of(2026, 11, 14).toEpochDay(), 2500, 2500), rows[0])
        assertTrue(CsvHistory.parse("{\"app\":\"kropi\"}").isEmpty())
    }
}
