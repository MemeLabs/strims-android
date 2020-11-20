package gg.strims.android.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerCallback
import gg.strims.android.databinding.FragmentYoutubeBinding
import gg.strims.android.hideChildFragment
import gg.strims.android.viewBinding
import gg.strims.android.viewmodels.YouTubeViewModel
import io.ktor.util.*

@KtorExperimentalAPI
class YouTubeFragment: Fragment() {

    val binding by viewBinding(FragmentYoutubeBinding::bind)

    private lateinit var youTubeViewModel: YouTubeViewModel

    override fun onDestroy() {
        binding.youTubeView.release()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        binding.youTubeView.getYouTubePlayerWhenReady(object : YouTubePlayerCallback {
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
        return FragmentYoutubeBinding.inflate(layoutInflater).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        hideChildFragment(requireParentFragment(), this)

        youTubeViewModel = ViewModelProvider(requireActivity()).get(YouTubeViewModel::class.java)

        lifecycle.addObserver(binding.youTubeView)

        binding.youTubeClose.setOnClickListener {
            binding.youTubeView.getYouTubePlayerWhenReady(object : YouTubePlayerCallback {
                override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
                    youTubePlayer.pause()
                }
            })

            youTubeViewModel.videoId.value = null

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
                    youTubePlayer.loadVideo(youTubeViewModel.videoId.value!!, 0f)
                }
            })
        }
    }
}