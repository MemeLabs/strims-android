package gg.strims.android.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TwitchViewModel: ViewModel() {
    var channel = MutableLiveData<String?>()
    var vod: Boolean = false
}