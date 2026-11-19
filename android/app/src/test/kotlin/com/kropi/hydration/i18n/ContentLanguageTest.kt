package com.kropi.hydration.i18n

import com.kropi.hydration.data.ActivityLevel
import com.kropi.hydration.data.DAYPART_NOTES
import com.kropi.hydration.data.Daypart
import com.kropi.hydration.data.Drink
import com.kropi.hydration.data.FACTS
import com.kropi.hydration.data.Level
import com.kropi.hydration.data.MASCOT_LINES
import com.kropi.hydration.data.NotificationTone
import com.kropi.hydration.data.SELF_CARE
import com.kropi.hydration.data.SNARK_CLOSERS
import com.kropi.hydration.data.SNARK_GAP_JABS
import com.kropi.hydration.data.SNARK_JABS
import com.kropi.hydration.data.SNARK_JABS_MILD
import com.kropi.hydration.data.SNARK_JABS_SAVAGE
import com.kropi.hydration.data.SNARK_MASCOT_LINES
import com.kropi.hydration.data.SNARK_TITLES
import com.kropi.hydration.data.SNARK_TITLES_AFTER_HOURS
import com.kropi.hydration.data.SNARK_TITLES_BEHIND
import com.kropi.hydration.data.SNARK_TITLES_SAVAGE
import com.kropi.hydration.data.SnarkIntensity
import com.kropi.hydration.data.Temperature
import com.kropi.hydration.data.WidgetStyle
import com.kropi.hydration.data.tr
import com.kropi.hydration.testutil.LanguageRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/** Tabele treści i etykiety enumów w obu językach: kompletne i bez resztek po polsku w angielskim. */
class ContentLanguageTest {
    private val polishLetters = Regex("[ąćęłńóśźżĄĆĘŁŃÓŚŹŻ]")

    private fun <T> inLanguage(tag: String, block: () -> T): T {
        val saved = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag(tag))
        try { return block() } finally { Locale.setDefault(saved) }
    }

    private fun allTables(): List<Pair<String, List<String>>> = listOf(
        "FACTS" to FACTS,
        "SNARK_TITLES_BEHIND" to SNARK_TITLES_BEHIND,
        "SNARK_TITLES_AFTER_HOURS" to SNARK_TITLES_AFTER_HOURS,
        "SNARK_GAP_JABS" to SNARK_GAP_JABS,
        "SNARK_CLOSERS" to SNARK_CLOSERS,
        "SNARK_JABS_MILD" to SNARK_JABS_MILD,
        "SNARK_JABS_SAVAGE" to SNARK_JABS_SAVAGE,
        "SNARK_TITLES_SAVAGE" to SNARK_TITLES_SAVAGE,
    ) + Level.entries.flatMap { l ->
        listOf(
            "SELF_CARE[$l]" to SELF_CARE.getValue(l),
            "MASCOT_LINES[$l]" to MASCOT_LINES.getValue(l),
            "SNARK_TITLES[$l]" to SNARK_TITLES.getValue(l),
            "SNARK_JABS[$l]" to SNARK_JABS.getValue(l),
            "SNARK_MASCOT_LINES[$l]" to SNARK_MASCOT_LINES.getValue(l),
        )
    } + Daypart.entries.map { "DAYPART_NOTES[$it]" to DAYPART_NOTES.getValue(it) }

    @Test
    fun englishTablesAreCompleteAndFreeOfPolishLetters() = inLanguage("en-US") {
        for ((name, lines) in allTables()) {
            assertTrue("$name jest puste", lines.isNotEmpty())
            for (line in lines) {
                assertTrue("Polskie znaki w EN $name: $line", !polishLetters.containsMatchIn(line))
                assertTrue("Pusta linia w $name", line.isNotBlank())
            }
        }
    }

    @Test
    fun polishTablesStayPolish() = inLanguage("pl-PL") {
        assertTrue(FACTS.any { polishLetters.containsMatchIn(it) })
        assertTrue(SELF_CARE.getValue(Level.LOW).first().startsWith("Zrób przerwę"))
    }

    @Test
    fun bothLanguagesHaveTheSameShape() {
        val pl = inLanguage("pl-PL") { allTables().associate { (n, l) -> n to l.size } }
        val en = inLanguage("en-US") { allTables().associate { (n, l) -> n to l.size } }
        assertEquals("Różne tabele/rozmiary", pl, en)
    }

    @Test
    fun enumLabelsAreTranslated() {
        val enumLabels = {
            Drink.entries.map { it.label } + Temperature.entries.map { it.label } + ActivityLevel.entries.map { it.label } +
                NotificationTone.entries.flatMap { listOf(it.label, it.hint) } +
                SnarkIntensity.entries.flatMap { listOf(it.label, it.hint) } +
                WidgetStyle.entries.flatMap { listOf(it.label, it.hint) }
        }
        val pl = inLanguage("pl-PL", enumLabels)
        val en = inLanguage("en-US", enumLabels)
        assertEquals(pl.size, en.size)
        en.forEach { assertTrue("Polskie znaki w etykiecie EN: $it", !polishLetters.containsMatchIn(it)) }
        assertEquals("Water", inLanguage("en-US") { Drink.WATER.label })
        assertEquals("Woda", inLanguage("pl-PL") { Drink.WATER.label })
    }

    @Test
    fun trPicksByLanguage() {
        assertEquals("tak", inLanguage("pl-PL") { tr("tak", "yes") })
        assertEquals("yes", inLanguage("en-US") { tr("tak", "yes") })
        // Inny język systemu (np. niemiecki) → zostajemy przy domyślnym polskim.
        assertNotEquals("yes", inLanguage("de-DE") { tr("tak", "yes") })
    }
}
