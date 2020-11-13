package gg.strims.android.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.exoplayer2.SimpleExoPlayer
import gg.strims.android.models.Stream

class ExoPlayerViewModel: ViewModel() {

    var player: SimpleExoPlayer? = null
    lateinit var currentlyPlaying: String

    var liveDataPlayer = MutableLiveData<SimpleExoPlayer>()
}