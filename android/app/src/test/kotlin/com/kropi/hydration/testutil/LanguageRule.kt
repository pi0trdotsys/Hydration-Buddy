package com.kropi.hydration.testutil

import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.Locale

/** Ustawia język domyślny na czas testu (i przywraca go) — teksty aplikacji zależą od [Locale.getDefault]. */
class LanguageRule(private val tag: String) : TestWatcher() {
    private var saved: Locale = Locale.getDefault()
    override fun starting(description: Description) { saved = Locale.getDefault(); Locale.setDefault(Locale.forLanguageTag(tag)) }
    override fun finished(description: Description) { Locale.setDefault(saved) }
}

fun polish() = LanguageRule("pl-PL")
fun english() = LanguageRule("en-US")
