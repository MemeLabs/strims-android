package gg.strims.android.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerCallback
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerListener
import gg.strims.android.databinding.FragmentYoutubeBinding
import gg.strims.android.utils.hideChildFragment
import gg.strims.android.utils.viewBinding
import gg.strims.android.viewmodels.YouTubeViewModel
import io.ktor.util.*
import kotlin.math.roundToInt

@KtorExperimentalAPI
class YouTubeFragment: Fragment() {

    val binding by viewBinding(FragmentYoutubeBinding::bind)

    private lateinit var youTubeViewModel: YouTubeViewModel

    override fun onStop() {
        binding.youTubeView.release()
        binding.youTubeView.getYouTubePlayerWhenReady(object : YouTubePlayerCallback {
            override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
                youTubePlayer.addListener(object : YouTubePlayerListener {
                    override fun onApiChange(youTubePlayer: YouTubePlayer) {}

                    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                        youTubeViewModel.currentPosition = second.roundToInt()
                    }

                    override fun onError(
                        youTubePlayer: YouTubePlayer,
                        error: PlayerConstants.PlayerError
                    ) {}

                    override fun onPlaybackQualityChange(
                        youTubePlayer: YouTubePlayer,
                        playbackQuality: PlayerConstants.PlaybackQuality
                    ) {}

                    override fun onPlaybackRateChange(
                        youTubePlayer: YouTubePlayer,
                        playbackRate: PlayerConstants.PlaybackRate
                    ) {}

                    override fun onReady(youTubePlayer: YouTubePlayer) {}

                    override fun onStateChange(
                        youTubePlayer: YouTubePlayer,
                        state: PlayerConstants.PlayerState
                    ) {}

                    override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {}

                    override fun onVideoId(youTubePlayer: YouTubePlayer, videoId: String) {}

                    override fun onVideoLoadedFraction(
                        youTubePlayer: YouTubePlayer,
                        loadedFraction: Float
                    ) {}
                })
            }
        })
        super.onStop()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentYoutubeBinding.inflate(layoutInflater).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        hideChildFragment(requireParentFragment(), this)

        youTubeViewModel = ViewModelProvider(requireActivity()).get(YouTubeViewModel::class.java)

        binding.youTubeClose.setOnClickListener {
            binding.youTubeView.getYouTubePlayerWhenReady(object : YouTubePlayerCallback {
                override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
                    youTubePlayer.pause()
                }
            })

            youTubeViewModel.videoId.value = null
            youTubeViewModel.currentPosition = null

            parentFragmentManager.beginTransaction()
                .hide(this)
                .commit()

            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                val parentFragment = requireParentFragment() as ChatFragment
                parentFragment.binding.constraintLayoutStreamFragment?.visibility = View.GONE
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (youTubeViewModel.videoId.value != null && !hidden) {
            binding.youTubeView.getYouTubePlayerWhenReady(object : YouTubePlayerCallback {
                override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
                    var startSeconds = 0f
                    if (youTubeViewModel.currentPosition != null) {
                        startSeconds = youTubeViewModel.currentPosition!!.toFloat()
                    }
                    youTubePlayer.loadVideo(youTubeViewModel.videoId.value!!, startSeconds)
                    youTubeViewModel.currentPosition = null
                }
            })
        }
    }
}