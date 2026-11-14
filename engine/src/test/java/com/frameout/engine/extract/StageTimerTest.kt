package com.frameout.engine.extract

import org.junit.Assert.assertTrue
import org.junit.Test

class StageTimerTest {
    @Test
    fun summaryCountsEachStage() {
        val timer = StageTimer()
        timer.time(StageTimer.Stage.Write) { 1 }
        timer.time(StageTimer.Stage.Write) { 2 }
        timer.time(StageTimer.Stage.Decode) { 3 }
        val text = timer.summary(2, 2, 3)
        assertTrue(text, text.contains("write=") && text.contains("/2"))
        assertTrue(text, text.contains("decoders=2 writers=3"))
    }
}

class StageTimerSeekTest {
    @Test
    fun summaryReportsSeekCounters() {
        val timer = StageTimer()
        timer.count(StageTimer.Counter.SeekDone)
        timer.count(StageTimer.Counter.SeekAvoided)
        timer.count(StageTimer.Counter.SeekAvoided)
        timer.time(StageTimer.Stage.Seek) { 1 }
        val text = timer.summary(10, 1, 1)
        assertTrue(text, text.contains("seeks=1 avoided=2"))
        assertTrue(text, text.contains("seek=") && text.contains("fps="))
    }
}
