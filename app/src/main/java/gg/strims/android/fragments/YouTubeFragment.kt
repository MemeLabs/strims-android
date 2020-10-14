package gg.strims.android.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerCallback
import gg.strims.android.CurrentUser
import gg.strims.android.R
import gg.strims.android.hideFragment
import kotlinx.android.synthetic.main.fragment_youtube.*

class YouTubeFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_youtube, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        hideFragment(requireActivity(), this)
        view.setOnTouchListener { view, motionEvent -> return@setOnTouchListener true }
        youTubeClose.setOnClickListener {
            youTubeView.getYouTubePlayerWhenReady(object : YouTubePlayerCallback {
                override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
                    youTubePlayer.pause()
                }
            })
            CurrentUser.tempYouTubeId = null
            requireFragmentManager().beginTransaction()
                .hide(this)
                .commit()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (CurrentUser.tempYouTubeId != null && !hidden) {
            youTubeView.getYouTubePlayerWhenReady(object : YouTubePlayerCallback {
                override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
                    youTubePlayer.loadVideo(CurrentUser.tempYouTubeId!!, 0f)
                }
            })
        }
    }
}