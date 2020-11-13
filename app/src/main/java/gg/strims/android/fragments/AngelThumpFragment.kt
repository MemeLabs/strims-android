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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import gg.strims.android.CurrentUser
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
            if (CurrentUser.tempStream?.service != "m3u8") {
                angelThumpStreamTitle.text = CurrentUser.tempStream?.title
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
//        hideFragment(requireActivity(), this)
        hideChildFragment(requireParentFragment(), this)

        exoPlayerViewModel = ViewModelProvider(this).get(ExoPlayerViewModel::class.java)

        exoPlayerViewModel.liveDataPlayer.observe(viewLifecycleOwner, {
            it.let {
                Log.d("TAG", "${exoPlayerViewModel.liveDataPlayer.value}")
            }
        })

        angelThumpClose.setOnClickListener {
            exoPlayerViewModel.player?.release()

            exoPlayerViewModel.player = null

            CurrentUser.tempStream = null
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
        if (CurrentUser.tempStream != null && !hidden) {
            player = SimpleExoPlayer.Builder(requireView().context).build()
            angelThumpVideoView.player = player

            if (exoPlayerViewModel.player != null && CurrentUser.tempStream!!.channel == exoPlayerViewModel.currentlyPlaying) {
                angelThumpVideoView.player = exoPlayerViewModel.player
            } else {
                exoPlayerViewModel.player?.release()

                if (CurrentUser.tempStream!!.service == "m3u8") {
                    player?.addMediaItem(MediaItem.fromUri(CurrentUser.tempStream!!.channel.toUri()))
                } else {
                    player?.addMediaItem(MediaItem.fromUri("https://video-cdn.angelthump.com/hls/${CurrentUser.tempStream!!.channel}/index.m3u8".toUri()))
                    angelThumpStreamTitle.text = CurrentUser.tempStream!!.title
                }

                player?.prepare()
                player?.play()
                exoPlayerViewModel.player = player
                exoPlayerViewModel.currentlyPlaying = CurrentUser.tempStream!!.channel
            }

            angelThumpVideoView.hideController()
        }
    }
}