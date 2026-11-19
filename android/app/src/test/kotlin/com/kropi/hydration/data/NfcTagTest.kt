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
}
