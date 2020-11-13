package gg.strims.android.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerCallback
import gg.strims.android.CurrentUser
import gg.strims.android.R
import gg.strims.android.hideChildFragment
import io.ktor.util.*
import kotlinx.android.synthetic.main.fragment_chat.*
import kotlinx.android.synthetic.main.fragment_youtube.*

@KtorExperimentalAPI
class YouTubeFragment: Fragment() {

    override fun onDestroy() {
        if (youTubeView != null) {
            youTubeView.release()
        }
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        youTubeView.getYouTubePlayerWhenReady(object : YouTubePlayerCallback {
            override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
                youTubePlayer.play()
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_youtube, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        hideFragment(requireActivity(), this)
        hideChildFragment(requireParentFragment(), this)

        lifecycle.addObserver(youTubeView)

        youTubeClose.setOnClickListener {
            youTubeView.getYouTubePlayerWhenReady(object : YouTubePlayerCallback {
                override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
                    youTubePlayer.pause()
                }
            })
            CurrentUser.tempYouTubeId = null
            parentFragmentManager.beginTransaction()
                .hide(this)
                .commit()

            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                val parentFragment = requireParentFragment() as ChatFragment
                parentFragment.constraintLayoutStreamFragment.visibility = View.GONE
            }
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