package com.frameout.core.quality

import org.junit.Assert.assertEquals
import org.junit.Test

class PeopleRuleTest {
    @Test
    fun aFaceIsEnough() = assertEquals(true, PeopleRule.isPerson(1, false))

    @Test
    fun aHumanShapedPoseIsEnoughWithoutAFace() = assertEquals(true, PeopleRule.isPerson(0, true))

    @Test
    fun emptySceneIsNotAPerson() = assertEquals(false, PeopleRule.isPerson(0, false))

    @Test
    fun faceWinsEvenIfPoseFailed() = assertEquals(PersonVerdict.Person, PeopleRule.verdict(1, null, true))

    @Test
    fun poseWinsEvenIfFaceDetectorFailed() = assertEquals(PersonVerdict.Person, PeopleRule.verdict(null, true, true))

    @Test
    fun bothDetectorsSayNobodyMeansNoPerson() = assertEquals(PersonVerdict.NoPerson, PeopleRule.verdict(0, false, true))

    @Test
    fun failedFaceDetectorIsNeverNoPerson() {
        assertEquals(PersonVerdict.Unknown, PeopleRule.verdict(null, false, true))
        assertEquals(PersonVerdict.Unknown, PeopleRule.verdict(null, null, true))
        assertEquals(PersonVerdict.Unknown, PeopleRule.verdict(null, null, false))
    }

    @Test
    fun failedPoseDetectorIsNeverNoPersonWhenBodiesCount() =
        assertEquals(PersonVerdict.Unknown, PeopleRule.verdict(0, null, true))

    @Test
    fun facesOnlyModeTrustsTheFaceDetectorAlone() {
        assertEquals(PersonVerdict.NoPerson, PeopleRule.verdict(0, null, false))
        assertEquals(PersonVerdict.Person, PeopleRule.verdict(2, null, false))
    }
}
