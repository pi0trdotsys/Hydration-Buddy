package com.kropi.hydration.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BackupFormatV2Test {
    @Test
    fun intakesAndDayGoalsSurviveRoundTrip() {
        val snap = BackupFormat.Snapshot(
            createdAtMs = 1_800_000_000_000L,
            appVersion = "1.8.0",
            entries = listOf(BackupFormat.Entry("goal", BackupFormat.Type.INT, 2500)),
            intakes = listOf(
                BackupFormat.IntakeRow(20_800, 450, 250, "COFFEE", "po bieganiu"),
                BackupFormat.IntakeRow(20_800, 600, 330, "WATER", null),
            ),
            dayGoals = listOf(BackupFormat.DayGoalRow(20_799, 2400)),
        )
        val decoded = BackupFormat.decode(BackupFormat.encode(snap))
        assertNotNull(decoded)
        assertEquals(snap.intakes, decoded!!.intakes)
        assertEquals(snap.dayGoals, decoded.dayGoals)
        assertEquals(snap.entries, decoded.entries)
    }

    @Test
    fun snapshotWithoutIntakesDecodesToEmptyLists() {
        val snap = BackupFormat.Snapshot(1L, "1.8.0", listOf(BackupFormat.Entry("goal", BackupFormat.Type.INT, 2000)))
        val decoded = BackupFormat.decode(BackupFormat.encode(snap))!!
        assertEquals(emptyList<BackupFormat.IntakeRow>(), decoded.intakes)
        assertEquals(emptyList<BackupFormat.DayGoalRow>(), decoded.dayGoals)
    }

    @Test
    fun formatOneBackupStillRestores() {
        val v1 = """{"app":"kropi","format":1,"createdAt":5,"appVersion":"1.7.0",
            "prefs":{"intakes":{"t":"string","v":"7:20:250"},"goal":{"t":"int","v":2500}}}"""
        val decoded = BackupFormat.decode(v1)
        assertNotNull(decoded)
        assertEquals(2, decoded!!.entries.size)
        assertEquals(emptyList<BackupFormat.IntakeRow>(), decoded.intakes)
    }

    @Test
    fun emptyBackupIsRejected() {
        assertNull(BackupFormat.decode(BackupFormat.encode(BackupFormat.Snapshot(1L, "1.8.0", emptyList()))))
    }

    @Test
    fun garbageIsRejected() {
        assertNull(BackupFormat.decode("to nie json"))
    }
}
