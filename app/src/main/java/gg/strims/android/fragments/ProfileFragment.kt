package gg.strims.android.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import gg.strims.android.CurrentUser
import gg.strims.android.R
import gg.strims.android.hideFragment
import kotlinx.android.synthetic.main.fragment_profile.*

class ProfileFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container,false)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (CurrentUser.user != null) {
            usernameEditTextProfile.setText(CurrentUser.user!!.username)
            streamPathEditTextProfile.setText(CurrentUser.user!!.stream_path)
            channelEditTextProfile.setText(CurrentUser.user!!.channel)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        hideFragment(activity!!, this)
        closeProfile.setOnClickListener {
            fragmentManager!!.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .hide(this)
                .commit()
        }
    }
}