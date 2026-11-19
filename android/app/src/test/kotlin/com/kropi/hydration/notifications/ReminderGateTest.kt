package com.kropi.hydration.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderGateTest {
    @Test
    fun dndHoldsWhenRespected() {
        assertEquals(ReminderGate.Reason.DND, ReminderGate.holdReason(dndActive = true, inMeeting = false, respectDnd = true, quietDuringMeetings = false))
    }

    @Test
    fun dndIgnoredWhenNotRespected() {
        assertNull(ReminderGate.holdReason(dndActive = true, inMeeting = false, respectDnd = false, quietDuringMeetings = false))
    }

    @Test
    fun meetingHoldsOnlyWhenEnabled() {
        assertEquals(ReminderGate.Reason.MEETING, ReminderGate.holdReason(false, true, respectDnd = true, quietDuringMeetings = true))
        assertNull(ReminderGate.holdReason(false, true, respectDnd = true, quietDuringMeetings = false))
    }

    @Test
    fun dndWinsOverMeetingForTheLabel() {
        assertEquals(ReminderGate.Reason.DND, ReminderGate.holdReason(true, true, respectDnd = true, quietDuringMeetings = true))
    }

    @Test
    fun nothingActiveMeansNoHold() {
        assertNull(ReminderGate.holdReason(false, false, respectDnd = true, quietDuringMeetings = true))
    }

    @Test
    fun onlyRealBusyTimedEventsCountAsMeetings() {
        // CalendarContract: AVAILABILITY_BUSY = 0, AVAILABILITY_FREE = 1, ATTENDEE_STATUS_DECLINED = 2
        assertTrue(ReminderGate.isBusyMeeting(allDay = false, availability = 0, selfStatus = 1))
        assertFalse(ReminderGate.isBusyMeeting(allDay = true, availability = 0, selfStatus = 1))   // całodniowe: urlop, urodziny
        assertFalse(ReminderGate.isBusyMeeting(allDay = false, availability = 1, selfStatus = 1))  // „wolny"
        assertFalse(ReminderGate.isBusyMeeting(allDay = false, availability = 0, selfStatus = 2))  // odrzucone
    }
}
