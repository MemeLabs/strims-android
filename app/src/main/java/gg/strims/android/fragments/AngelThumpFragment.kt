package gg.strims.android.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import gg.strims.android.CurrentUser
import gg.strims.android.R
import gg.strims.android.hideFragment
import kotlinx.android.synthetic.main.fragment_angelthump.*

class AngelThumpFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_angelthump, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        hideFragment(activity!!, this)
        angelThumpClose.setOnClickListener {
            angelThumpVideoView.stopPlayback()
            CurrentUser.tempStream = null
            fragmentManager!!.beginTransaction()
                .hide(this)
                .commit()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (CurrentUser.tempStream != null && !hidden) {
            angelThumpVideoView.setVideoURI("https://video-cdn.angelthump.com/hls/${CurrentUser.tempStream!!.channel}/index.m3u8".toUri())
            angelThumpVideoView.start()
            angelThumpVideoView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            angelThumpStreamTitle.text = CurrentUser.tempStream!!.title
        }
    }
}