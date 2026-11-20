package com.kropi.hydration.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NfcTagTest {
    @Test
    fun uriRoundTrips() {
        for (d in Drink.entries) {
            val parsed = NfcTag.parse(NfcTag.uri(330, d))!!
            assertEquals(330, parsed.ml)
            assertEquals(d, parsed.drink)
        }
    }

    @Test
    fun oldShortcutFormatStillWorks() {
        assertEquals(QuickAdd(500, null), NfcTag.parse("kropi://add/500"))
        assertEquals(QuickAdd(null, null), NfcTag.parse("kropi://add"))
        assertEquals(QuickAdd(null, null), NfcTag.parse("kropi://add/glass"))
    }

    @Test
    fun amountFromATagIsClamped() {
        assertEquals(QuickAdd.MAX_ML, NfcTag.parse("kropi://add?ml=999999")!!.ml)
        assertEquals(QuickAdd.MIN_ML, NfcTag.parse("kropi://add?ml=-5")!!.ml)
        assertEquals(QuickAdd.MAX_ML, NfcTag.parse(NfcTag.uri(99999, Drink.WATER))!!.ml)
    }

    @Test
    fun unknownDrinkMeansNoDrinkAndLowercaseIsAccepted() {
        assertNull(NfcTag.parse("kropi://add?ml=250&drink=BLEACH")!!.drink)
        assertEquals(Drink.TEA, NfcTag.parse("kropi://add?ml=250&drink=tea")!!.drink)
    }

    @Test
    fun foreignOrBrokenAddressesAreRejected() {
        assertNull(NfcTag.parse(null))
        assertNull(NfcTag.parse(""))
        assertNull(NfcTag.parse("https://example.com/add?ml=250"))
        assertNull(NfcTag.parse("kropi://other?ml=250"))
        assertNull(NfcTag.parse("kropi://add?ml=250 &drink=%%%"))
    }

    @Test
    fun repeatedTapIsDebouncedPerTag() {
        val uri = NfcTag.uri(250, Drink.WATER)
        assertTrue(NfcTag.isRepeat(uri, 1_000, uri, 1_000 + NfcTag.DEBOUNCE_MS - 1))
        assertFalse(NfcTag.isRepeat(uri, 1_000, uri, 1_000 + NfcTag.DEBOUNCE_MS))
        assertFalse(NfcTag.isRepeat(uri, 1_000, NfcTag.uri(250, Drink.COFFEE), 2_000))
        assertFalse(NfcTag.isRepeat(null, 0, uri, 2_000))
    }

    @Test
    fun timeAndNoteFromAnotherAppAreParsed() {
        val now = 1_795_000_000_000L
        val twoHoursAgo = now - 2 * 3_600_000L
        val parsed = NfcTag.parse("kropi://add?ml=500&drink=ALCOHOL&at=$twoHoursAgo&note=Kieliszek+w%C3%B3dki", now)!!
        assertEquals(QuickAdd(500, Drink.ALCOHOL, twoHoursAgo, "Kieliszek wódki"), parsed)
    }

    @Test
    fun timeOutsideTheWindowMeansNow() {
        val now = 1_795_000_000_000L
        val tooOld = now - (QuickAdd.MAX_PAST_DAYS + 1) * 24 * 3_600_000L
        val tooFarAhead = now + QuickAdd.MAX_FUTURE_MS + 1
        assertNull(NfcTag.parse("kropi://add?ml=500&at=$tooOld", now)!!.atMs)
        assertNull(NfcTag.parse("kropi://add?ml=500&at=$tooFarAhead", now)!!.atMs)
        assertNull(NfcTag.parse("kropi://add?ml=500&at=yesterday", now)!!.atMs)
        assertEquals(now + 60_000, NfcTag.parse("kropi://add?ml=500&at=${now + 60_000}", now)!!.atMs)
    }

    @Test
    fun noteIsTrimmedCappedAndEmptyMeansNone() {
        val long = "x".repeat(QuickAdd.MAX_NOTE + 40)
        assertEquals(QuickAdd.MAX_NOTE, NfcTag.parse("kropi://add?note=$long")!!.note!!.length)
        assertNull(NfcTag.parse("kropi://add?note=%20%20")!!.note)
        assertEquals("Piwo", NfcTag.parse("kropi://add?note=%0APiwo%09")!!.note)
    }

    @Test
    fun addressesWithoutNewParametersStayUnchanged() {
        assertEquals(QuickAdd(250, Drink.COFFEE), NfcTag.parse("kropi://add?ml=250&drink=COFFEE"))
    }
}

