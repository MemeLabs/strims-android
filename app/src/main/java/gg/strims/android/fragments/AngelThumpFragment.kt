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
        hideFragment(requireActivity(), this)
        view.setOnTouchListener { view, motionEvent -> return@setOnTouchListener true }
        angelThumpClose.setOnClickListener {
            angelThumpVideoView.stopPlayback()
            CurrentUser.tempStream = null
            requireFragmentManager().beginTransaction()
                .hide(this)
                .commit()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (CurrentUser.tempStream != null && !hidden) {
            if (CurrentUser.tempStream!!.service == "m3u8") {
                angelThumpVideoView.setVideoURI(CurrentUser.tempStream!!.channel.toUri())
                angelThumpVideoView.start()
            } else {
                angelThumpVideoView.setVideoURI("https://video-cdn.angelthump.com/hls/${CurrentUser.tempStream!!.channel}/index.m3u8".toUri())
                angelThumpVideoView.start()
                angelThumpStreamTitle.text = CurrentUser.tempStream!!.title
            }
        } else {
            angelThumpVideoView.stopPlayback()
        }
    }
}