package io.ajarara.music

import android.media.MediaPlayer
import android.os.Bundle
import androidx.fragment.app.Fragment

class MusicFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mp = MediaPlayer.create(context)
    }
}