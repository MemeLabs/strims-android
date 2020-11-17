package gg.strims.android.fragments

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import gg.strims.android.R
import gg.strims.android.hideChildFragment
import gg.strims.android.viewmodels.ExoPlayerViewModel
import io.ktor.util.*
import kotlinx.android.synthetic.main.fragment_angelthump.*
import kotlinx.android.synthetic.main.fragment_chat.*

@KtorExperimentalAPI
class AngelThumpFragment: Fragment() {

    private var player: SimpleExoPlayer? = null

    private lateinit var exoPlayerViewModel: ExoPlayerViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_angelthump, container, false)
    }

    override fun onResume() {
        super.onResume()
        if (!requireActivity().isInPictureInPictureMode) {
            angelThumpStreamTitle.visibility = View.VISIBLE
            if (exoPlayerViewModel.liveDataStream.value?.service != "m3u8") {
                angelThumpStreamTitle.text = exoPlayerViewModel.liveDataStream.value?.title
            }
            angelThumpSeparator.visibility = View.VISIBLE
            angelThumpClose.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        if (!requireActivity().isChangingConfigurations) {
            player?.stop()
            player?.release()
        }
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        hideChildFragment(requireParentFragment(), this)

        exoPlayerViewModel = ViewModelProvider(requireActivity()).get(ExoPlayerViewModel::class.java)

        angelThumpClose.setOnClickListener {
            exoPlayerViewModel.player?.release()
            exoPlayerViewModel.player = null
            exoPlayerViewModel.liveDataStream.value = null

            parentFragmentManager.beginTransaction()
                .hide(this)
                .commit()

            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                val parentFragment = requireParentFragment() as ChatFragment
                parentFragment.constraintLayoutStreamFragment.visibility = View.GONE
            }
        }
    }

    fun enterPIPMode() {
        angelThumpVideoView.useController = false
        val params = PictureInPictureParams.Builder()
        requireActivity().enterPictureInPictureMode(params.build())
        angelThumpStreamTitle.visibility = View.GONE
        angelThumpSeparator.visibility = View.GONE
        angelThumpClose.visibility = View.GONE
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (exoPlayerViewModel.liveDataStream.value != null && !hidden) {

            if (exoPlayerViewModel.player != null && exoPlayerViewModel.liveDataStream.value?.channel == exoPlayerViewModel.currentlyPlaying) {
                Log.d("TAG", "EXISTING PLAYER")
                angelThumpVideoView.player = exoPlayerViewModel.player
            } else {
                Log.d("TAG", "NEW PLAYER")
                player = SimpleExoPlayer.Builder(requireView().context).build()
                angelThumpVideoView.player = player

                exoPlayerViewModel.liveDataStream.observe(viewLifecycleOwner, {
                    if (it != null) {
                        Log.d("TAG", "OBSERVED NOT NULL")
                        exoPlayerViewModel.player?.release()

                        if (it.service == "m3u8") {
                            player?.addMediaItem(MediaItem.fromUri(it.channel.toUri()))
                        } else {
                            player?.addMediaItem(MediaItem.fromUri("https://video-cdn.angelthump.com/hls/${it.channel}/index.m3u8".toUri()))
                            angelThumpStreamTitle.text = it.title
                        }

                        player?.prepare()
                        player?.play()
                        exoPlayerViewModel.player = player
                        exoPlayerViewModel.currentlyPlaying = it.channel
                    }
                })
            }
        }
    }
}