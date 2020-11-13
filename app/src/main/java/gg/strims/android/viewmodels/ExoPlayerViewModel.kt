package gg.strims.android.viewmodels

import androidx.lifecycle.ViewModel
import com.google.android.exoplayer2.SimpleExoPlayer

class ExoPlayerViewModel: ViewModel() {

    var player: SimpleExoPlayer? = null
    lateinit var currentlyPlaying: String
}