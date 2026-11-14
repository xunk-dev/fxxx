package com.frameout.engine.player

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class PlayerController(internal val context: Context) {
    internal val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext, DefaultRenderersFactory(context.applicationContext).setEnableDecoderFallback(true))
        .setLoadControl(smoothLoadControl())
        .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
        .setHandleAudioBecomingNoisy(true)
        .build()
    internal val _state = MutableStateFlow(PlayerUiState())
    internal val _position = MutableStateFlow(0L)
    internal var mediaUri: Uri? = null
    internal var grabber: FrameGrabber? = null
    private val steps = StepQueue(::performStep)
    internal val recovery = PlaybackRecovery()
    var onSourceRepaired: ((Uri) -> Unit)? = null
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    internal val precapture = FramePrecapture(scope)
    internal val _frame = MutableStateFlow<Bitmap?>(null)
    internal var animated: AnimatedPlayback? = null
    internal var openJob: Job? = null
    @Volatile internal var ptsIndex: LongArray? = null
    internal var indexJob: Job? = null
    internal val _still = MutableStateFlow<Bitmap?>(null)
    internal var stillJob: Job? = null
    @Volatile internal var stillMs = NO_POSITION
    @Volatile internal var pendingStillMs = NO_POSITION
    @Volatile internal var sourceHdr = false
    @Volatile internal var windowHdr = false
    internal var released = false
    internal var fallbackTried = false
    @Volatile internal var windowStartMs = -1L
    @Volatile internal var windowEndMs = -1L
    @Volatile internal var scrubbing = false
    @Volatile internal var scrubTargetMs = -1L
    internal var frameCache: GopFrameCache? = null
    internal var warmJob: Job? = null
    @Volatile internal var lastWarmMs = NO_POSITION
    @Volatile internal var keepStill = false
    @Volatile internal var awaitRender = false
    @Volatile internal var cachedShown = false
    @Volatile internal var bufferingSinceMs = 0L
    internal val main = Handler(Looper.getMainLooper())
    internal var lastScrubSeekTarget = -1L
    internal var resumeAfterScrub = false
    val frame: StateFlow<Bitmap?> = _frame.asStateFlow()
    val still: StateFlow<Bitmap?> = _still.asStateFlow()
    internal val _hdr = MutableStateFlow(false)
    val hdrSource: StateFlow<Boolean> = _hdr.asStateFlow()
    var hdrNativeSurface: Boolean = false
    var hdrStillEnabled: Boolean = true
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()
    val position: StateFlow<Long> = _position.asStateFlow()
    val playing: Boolean get() = animated?.playing ?: player.isPlaying

    private val listener = PlayerListener(this)

    init {
        player.addListener(listener)
        player.repeatMode = Player.REPEAT_MODE_OFF
    }

    fun attach(): ExoPlayer = player

    fun setUri(uri: Uri) = setUriImpl(uri)

    fun setWindow(startMs: Long, endMs: Long) {
        if (endMs - startMs < MIN_WINDOW_MS) {
            clearWindow()
            return
        }
        windowStartMs = startMs.coerceAtLeast(0L)
        windowEndMs = endMs
    }

    fun clearWindow() {
        windowStartMs = -1L
        windowEndMs = -1L
    }

    internal val windowActive: Boolean get() = windowStartMs >= 0L && windowEndMs > windowStartMs

    fun play() {
        precapture.clear()
        warmJob?.cancel()
        frameCache?.clear()
        lastWarmMs = NO_POSITION
        dropStill()
        animated?.let {
            if (outsideWindow(_position.value)) seekTo(windowStartMs)
            it.play()
            return
        }
        val duration = player.duration
        val atEnd = player.playbackState == Player.STATE_ENDED || (duration > 0 && player.currentPosition >= duration - END_TOLERANCE_MS)
        if (windowActive && outsideWindow(player.currentPosition)) player.seekTo(windowStartMs)
        else if (atEnd) player.seekTo(0L)
        player.play()
    }

    fun pause() {
        val wasPlaying = playing
        val clip = animated
        if (clip != null) clip.pause() else player.pause()
        if (wasPlaying) settleInWindow()
    }

    fun repairPlayhead() {
        if (!windowActive || scrubbing || playing) return
        WindowRules.repairAfterRangeChange(windowStartMs, windowEndMs, _position.value)?.let(::seekTo)
    }

    fun setLooping(loop: Boolean) {
        if (released) return
        player.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun setMuted(muted: Boolean) {
        if (released) return
        player.volume = if (muted) 0f else 1f
        _state.value = _state.value.copy(muted = muted)
    }

    fun cycleSpeed() {
        val speed = PlaybackSpeeds.next(_state.value.speed)
        applySpeed(speed)
        _state.value = _state.value.copy(speed = speed)
    }

    fun holdSpeed(speed: Float) = applySpeed(speed)

    fun releaseSpeed() = applySpeed(_state.value.speed)

    fun toggle() {
        if (playing) pause() else play()
    }

    suspend fun stepFrame(forward: Boolean, count: Int = 1) = steps.push(if (forward) count else -count)

    private suspend fun performStep(delta: Int) {
        val uri = mediaUri ?: return
        animated?.let { clip ->
            _position.value = clip.step(delta)
            return
        }
        player.pause()
        val from = if (scrubbing && scrubTargetMs >= 0L) scrubTargetMs else player.currentPosition
        val index = ptsIndex
        if (index != null) {
            val targetUs = FramePtsIndex.stepUs(index, from, delta > 0, kotlin.math.abs(delta)) ?: return
            if (scrubbing && showCached(targetUs)) return
            if (scrubbing) dropCachedStill()
            seekTo(targetUs / 1000L)
            return
        }
        val target = withContext(Dispatchers.IO) {
            FrameStepper.step(context.applicationContext, uri, from, delta > 0, kotlin.math.abs(delta))
        } ?: return
        seekTo(target)
    }

    suspend fun captureFrame(): Bitmap? = captureAt(player.currentPosition)

    suspend fun captureAt(positionMs: Long): Bitmap? {
        val uri = mediaUri ?: return null
        animated?.let { return it.captureAt(positionMs) }
        precapture.take(positionMs)?.let { return it }
        return grabNow(uri, positionMs)
    }

    internal suspend fun grabNow(uri: Uri, positionMs: Long): Bitmap? {
        val known = ptsIndex?.takeIf { it.isNotEmpty() }?.let { FramePtsIndex.displayedUs(it, positionMs) }
        return grabber?.grab(positionMs, known) ?: FrameStepper.capture(context.applicationContext, uri, positionMs)
    }

    fun seekTo(ms: Long) {
        animated?.let {
            it.seekTo(ms)
            _position.value = ms.coerceIn(0L, it.durationMs)
            return
        }
        val d = player.duration.coerceAtLeast(0)
        val target = ms.coerceAtLeast(0L).coerceAtMost(if (d > 0) d else Long.MAX_VALUE)
        if (scrubbing) scrubTargetMs = target
        player.seekTo(target)
        refresh()
    }

    fun scrubTo(ms: Long) {
        if (!scrubbing || released) { seekTo(ms); return }
        if (animated != null) { seekTo(ms); return }
        val d = player.duration.coerceAtLeast(0)
        val target = ms.coerceAtLeast(0L).coerceAtMost(if (d > 0) d else Long.MAX_VALUE)
        scrubTargetMs = target
        _position.value = target
        if (showCachedAt(target)) return
        dropCachedStill()
        fireScrubSeek()
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun scrub(active: Boolean) {
        if (released) return
        val was = scrubbing
        scrubbing = active
        if (active && !was) {
            resumeAfterScrub = playing
            if (resumeAfterScrub) pause()
        }
        if (active) { dropStill(); precapture.clear() }
        player.setScrubbingModeEnabled(active)
        player.setSeekParameters(SeekParameters.EXACT)
        if (active) lastScrubSeekTarget = -1L
        if (active) requestWarm(_position.value, immediate = true)
        if (!active) {
            val target = scrubTargetMs
            scrubTargetMs = -1L
            if (target >= 0L && animated == null) {
                if (cachedShown) armCachedStillRelease()
                player.seekTo(target)
                _position.value = target
            }
            cachedShown = false
            if (was && resumeAfterScrub) {
                resumeAfterScrub = false
                val d = player.duration
                if (target < 0L || d <= 0L || target < d - RESUME_TAIL_MS) play()
            }
        }
    }

    fun positionNow(): Long = _position.value
    fun poll() {
        refresh()
        enforceWindow()
    }

    fun setWindowShowsHdr(value: Boolean) {
        windowHdr = value
        if (value) dropStill()
    }

    fun release() {
        released = true
        precapture.clear()
        main.removeCallbacksAndMessages(null)
        dropStill()
        openJob?.cancel()
        indexJob?.cancel()
        ptsIndex = null
        warmJob?.cancel()
        frameCache?.clear()
        frameCache = null
        animated?.close()
        animated = null
        scope.cancel()
        player.removeListener(listener)
        mediaUri = null
        grabber?.close()
        grabber = null
        player.release()
    }
}
