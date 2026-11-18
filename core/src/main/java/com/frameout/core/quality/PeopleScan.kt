package com.frameout.core.quality

import com.frameout.core.db.FrameDao

object PeopleScan {
    class Result(val withPeople: List<Long>, val withoutPeople: List<Long>, val unknown: Int) {
        val scanned: Int get() = withPeople.size + withoutPeople.size + unknown
        val allUndecided: Boolean get() = scanned > 0 && unknown == scanned
    }
    suspend fun scan(dao: FrameDao, projectId: Long): Result {
        val verdicts = FrameScan.mapThumbnails(dao, projectId) { frame, bitmap ->
            frame.id to PeopleFilter.verdict(bitmap)
        }
        return Result(
            withPeople = verdicts.filter { it.second == PersonVerdict.Person }.map { it.first },
            withoutPeople = verdicts.filter { it.second == PersonVerdict.NoPerson }.map { it.first },
            unknown = verdicts.count { it.second == PersonVerdict.Unknown }
        )
    }

    suspend fun withPeople(dao: FrameDao, projectId: Long): List<Long> = scan(dao, projectId).withPeople

    suspend fun withoutPeople(dao: FrameDao, projectId: Long): List<Long> = scan(dao, projectId).withoutPeople
}
