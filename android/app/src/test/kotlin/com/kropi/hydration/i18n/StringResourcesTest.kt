package com.kropi.hydration.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Pilnuje, żeby polskie (domyślne) i angielskie zasoby nie rozjechały się: te same klucze, te same
 * podstawienia (%1$s…) i kompletne liczby mnogie. Bez tego brakujące tłumaczenie wychodzi dopiero u
 * użytkownika, jako polski napis w środku angielskiego ekranu.
 */
class StringResourcesTest {
    private val res = File("src/main/res")

    private data class Res(val strings: Map<String, String>, val plurals: Map<String, Map<String, String>>)

    private fun load(dir: String): Res {
        val strings = linkedMapOf<String, String>()
        val plurals = linkedMapOf<String, Map<String, String>>()
        File(res, dir).listFiles { f -> f.extension == "xml" }!!.sortedBy { it.name }.forEach { file ->
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            val strNodes = doc.getElementsByTagName("string")
            for (i in 0 until strNodes.length) {
                val n = strNodes.item(i)
                val name = n.attributes.getNamedItem("name").nodeValue
                check(strings.put(name, n.textContent) == null) { "Zdublowany klucz: $name ($dir)" }
            }
            val plNodes = doc.getElementsByTagName("plurals")
            for (i in 0 until plNodes.length) {
                val n = plNodes.item(i)
                val items = n.childNodes
                val map = linkedMapOf<String, String>()
                for (j in 0 until items.length) {
                    val item = items.item(j)
                    if (item.nodeName == "item") map[item.attributes.getNamedItem("quantity").nodeValue] = item.textContent
                }
                plurals[n.attributes.getNamedItem("name").nodeValue] = map
            }
        }
        return Res(strings, plurals)
    }

    private val pl = load("values")
    private val en = load("values-en")

    private fun placeholders(text: String) =
        Regex("%(\\d+\\$)?[sd]").findAll(text).map { it.value.replace(Regex("^%\\d+\\$"), "%") }.sorted().toList()

    private fun numbered(text: String) = Regex("%(\\d+)\\\$s").findAll(text).map { it.groupValues[1] }.toSortedSet()

    @Test
    fun bothLanguagesHaveTheSameKeys() {
        assertEquals("Brak w angielskim", emptySet<String>(), pl.strings.keys - en.strings.keys)
        assertEquals("Nadmiarowe w angielskim", emptySet<String>(), en.strings.keys - pl.strings.keys)
        assertEquals(emptySet<String>(), pl.plurals.keys - en.plurals.keys)
        assertEquals(emptySet<String>(), en.plurals.keys - pl.plurals.keys)
    }

    @Test
    fun placeholdersMatchBetweenLanguages() {
        for ((key, plText) in pl.strings) {
            val enText = en.strings[key] ?: continue
            assertEquals("Różne podstawienia w „$key”", numbered(plText), numbered(enText))
        }
    }

    @Test
    fun noEmptyOrTodoStrings() {
        for ((lang, r) in listOf("pl" to pl, "en" to en)) {
            for ((key, text) in r.strings) {
                assertTrue("Pusty tekst „$key” ($lang)", text.isNotBlank() || key.endsWith("_suffix"))
                assertTrue("TODO w „$key” ($lang)", !text.contains("TODO"))
            }
        }
    }

    @Test
    fun englishStringsContainNoPolishLetters() {
        val polish = Regex("[ąćęłńóśźżĄĆĘŁŃÓŚŹŻ]")
        for ((key, text) in en.strings) assertTrue("Polskie znaki w angielskim „$key”: $text", !polish.containsMatchIn(text))
        for ((key, items) in en.plurals) for ((q, text) in items) assertTrue("Polskie znaki w plurals $key/$q", !polish.containsMatchIn(text))
    }

    @Test
    fun pluralsHaveTheRightQuantitiesForEachLanguage() {
        for ((key, items) in pl.plurals) assertEquals("pl $key", setOf("one", "few", "many", "other"), items.keys)
        for ((key, items) in en.plurals) assertEquals("en $key", setOf("one", "other"), items.keys)
    }

    @Test
    fun pluralItemsAcceptTheirNumber() {
        for ((lang, r) in listOf("pl" to pl, "en" to en)) {
            for ((key, items) in r.plurals) for ((q, text) in items) {
                assertTrue("$lang plurals $key/$q bez %d: $text", text.contains("%d"))
            }
        }
    }

    @Test
    fun percentSignsMatchHowTheStringIsUsed() {
        // Tekst z podstawieniami idzie przez String.format: tam „%" musi być „%%". Tekst bez podstawień
        // jest wyświetlany wprost: „%%" pokazałoby się użytkownikowi dosłownie.
        val bad = Regex("%(?!\\d+\\$[sd]|[sd])")
        for ((lang, r) in listOf("pl" to pl, "en" to en)) {
            for ((key, text) in r.strings) {
                val formatted = Regex("%\\d+\\$[sd]").containsMatchIn(text)
                if (formatted) {
                    assertTrue("$lang „$key” ma niezabezpieczone %: $text", !bad.containsMatchIn(text.replace("%%", "")))
                } else {
                    assertTrue("$lang „$key” nie ma podstawień, a zawiera %%: $text", !text.contains("%%"))
                }
            }
        }
    }
}
