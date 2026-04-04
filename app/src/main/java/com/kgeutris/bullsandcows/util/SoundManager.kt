package com.kgeutris.bullsandcows.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.annotation.RawRes
import com.kgeutris.bullsandcows.R

class SoundManager(context: Context) {
    private val soundPool: SoundPool
    private val soundIds = mutableMapOf<Sound, Int>()

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        // Загрузка звуков — импорт R не нужен!
        Sound.entries.forEach { sound ->
            soundIds[sound] = soundPool.load(context, sound.resId, 1)
        }
    }

    fun play(sound: Sound) {
        val id = soundIds[sound] ?: return
        soundPool.play(id, 1.0f, 1.0f, 0, 0, 1.0f)
    }

    fun release() {
        soundPool.release()
    }

    enum class Sound(@RawRes val resId: Int) {
        CLICK(R.raw.click),
        BUZZ(R.raw.buzz),
        WIN(R.raw.win)
    }
}