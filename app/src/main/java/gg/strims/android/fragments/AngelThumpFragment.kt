package gg.strims.android.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import gg.strims.android.CurrentUser
import gg.strims.android.R
import gg.strims.android.hideFragment
import io.ktor.util.*
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.fragment_angelthump.*

@KtorExperimentalAPI
class AngelThumpFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_angelthump, container, false)
    }

    override fun onResume() {
        super.onResume()
        if (CurrentUser.tempStream != null && !angelThumpVideoView.isPlaying) {
            angelThumpVideoView.start()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        hideFragment(requireActivity(), this)

        view.setOnTouchListener { view, motionEvent -> return@setOnTouchListener true }

        angelThumpClose.setOnClickListener {
            angelThumpVideoView.stopPlayback()
            CurrentUser.tempStream = null
            parentFragmentManager.beginTransaction()
                .hide(this)
                .commit()

            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                requireActivity().constraintLayoutStream.visibility = View.GONE
            }
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