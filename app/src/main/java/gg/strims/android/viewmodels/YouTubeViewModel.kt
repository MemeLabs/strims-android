package gg.strims.android.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class YouTubeViewModel: ViewModel() {
    var videoId = MutableLiveData<String?>()
}