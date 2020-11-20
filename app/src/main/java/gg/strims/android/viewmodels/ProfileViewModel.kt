package gg.strims.android.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ProfileViewModel: ViewModel() {
    var jwt: String? = null
    var logOut = MutableLiveData<Boolean>()
}