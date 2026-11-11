package com.example.counter.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CounterActionReceiverTest {

    @Test
    fun acceptsIncrementForPositiveId() {
        assertEquals(
            CounterAction.INCREMENT,
            CounterActionReceiver.parseAction(CounterActionReceiver.ACTION_INCREMENT, 12)
        )
    }

    @Test
    fun acceptsDecrementForPositiveId() {
        assertEquals(
            CounterAction.DECREMENT,
            CounterActionReceiver.parseAction(CounterActionReceiver.ACTION_DECREMENT, 12)
        )
    }

    @Test
    fun rejectsUnknownOrMissingAction() {
        assertNull(CounterActionReceiver.parseAction("ACTION_DELETE", 12))
        assertNull(CounterActionReceiver.parseAction(null, 12))
    }

    @Test
    fun rejectsZeroAndNegativeCounterIds() {
        assertNull(CounterActionReceiver.parseAction(CounterActionReceiver.ACTION_INCREMENT, 0))
        assertNull(CounterActionReceiver.parseAction(CounterActionReceiver.ACTION_INCREMENT, -1))
    }
}
