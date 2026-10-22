package com.rubik3d.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.rubik3d.R
import java.util.Random

class CubeAudioPlayer(context: Context) {

    private val soundPool: SoundPool
    private val snapId: Int
    private val clickId: Int
    private val frictionId: Int
    private val victoryId: Int

    var isSoundEnabled: Boolean = true
    var volume: Float = 0.7f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    private val random = Random()
    private val handler = Handler(Looper.getMainLooper())

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(audioAttributes)
            .build()

        snapId = soundPool.load(context, R.raw.cube_snap, 1)
        clickId = soundPool.load(context, R.raw.cube_click, 1)
        frictionId = soundPool.load(context, R.raw.cube_friction, 1)
        victoryId = soundPool.load(context, R.raw.cube_victory, 1)
    }

    private fun getRandomRate(baseRate: Float = 1.0f): Float {
        // Natural speedcube acoustic variation: +/- 6%
        val variation = (random.nextFloat() * 0.12f) - 0.06f
        return (baseRate + variation).coerceIn(0.5f, 2.0f)
    }

    /**
     * Mechanical locking snap when layer settles into grid
     */
    fun playSnap(isDouble: Boolean = false) {
        if (!isSoundEnabled || volume <= 0.01f) return
        val sound = if (random.nextBoolean()) snapId else clickId
        soundPool.play(sound, volume, volume, 1, 0, getRandomRate(1.0f))

        if (isDouble) {
            handler.postDelayed({
                if (isSoundEnabled && volume > 0.01f) {
                    val secondSound = if (sound == snapId) clickId else snapId
                    soundPool.play(secondSound, volume * 0.9f, volume * 0.9f, 1, 0, getRandomRate(1.05f))
                }
            }, 60)
        }
    }

    /**
     * Slice turning friction sound
     */
    fun playFriction() {
        if (!isSoundEnabled || volume <= 0.01f) return
        soundPool.play(frictionId, volume * 0.45f, volume * 0.45f, 0, 0, getRandomRate(1.0f))
    }

    /**
     * Fanfare sound on cube solved
     */
    fun playVictory() {
        if (!isSoundEnabled || volume <= 0.01f) return
        soundPool.play(victoryId, volume, volume, 2, 0, 1.0f)
    }

    fun release() {
        handler.removeCallbacksAndMessages(null)
        soundPool.release()
    }
}
