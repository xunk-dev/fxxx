package com.frameout.app.library

import android.net.Uri
import com.frameout.app.FrameOutApp
import com.frameout.app.ProjectCover
import com.frameout.core.db.ProjectEntity
import com.frameout.core.util.Fanout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File

private const val COVER_WIDTH = 3

internal class CoverBackfill(private val app: FrameOutApp, private val scope: CoroutineScope) {
    private val tried = HashSet<Long>()
    private val gate = Semaphore(Fanout.videoWidth().coerceAtMost(COVER_WIDTH))

    fun run(projects: List<ProjectEntity>) {
        projects.filter { missing(it) && tried.add(it.id) }.forEach { project ->
            scope.launch(Dispatchers.IO) {
                gate.withPermit {
                    ProjectCover.create(app, app.container.storage, app.container.projects, project.id, Uri.parse(project.sourceUri), project.startMs)
                }
            }
        }
    }

    private fun missing(project: ProjectEntity): Boolean {
        val path = project.thumbnailPath
        return path.isNullOrBlank() || !File(path).exists()
    }
}
