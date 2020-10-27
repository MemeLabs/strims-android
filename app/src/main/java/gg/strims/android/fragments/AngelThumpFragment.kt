package gg.strims.android.fragments

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import gg.strims.android.CurrentUser
import gg.strims.android.R
import gg.strims.android.hideFragment
import io.ktor.util.*
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.fragment_angelthump.*

@KtorExperimentalAPI
class AngelThumpFragment: Fragment() {

    private var player: SimpleExoPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_angelthump, container, false)
    }

    override fun onResume() {
        super.onResume()
        if (CurrentUser.tempStream != null && !player!!.isPlaying) {
            player?.play()
        }

        angelThumpStreamTitle.visibility = View.VISIBLE
        angelThumpSeparator.visibility = View.VISIBLE
        angelThumpClose.visibility = View.VISIBLE
    }

    override fun onPause() {
        if (CurrentUser.tempStream != null && !requireActivity().isChangingConfigurations) {
            enterPIPMode()
        }
        super.onPause()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        hideFragment(requireActivity(), this)

        player = SimpleExoPlayer.Builder(view.context).build()
        angelThumpVideoView.player = player

        angelThumpClose.setOnClickListener {
            player?.stop()
            player?.removeMediaItems(0, player?.mediaItemCount!!)
            CurrentUser.tempStream = null
            parentFragmentManager.beginTransaction()
                .hide(this)
                .commit()

            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                requireActivity().constraintLayoutStream.visibility = View.GONE
            }
        }
    }

    private fun enterPIPMode() {
        angelThumpVideoView.useController = false
        val params = PictureInPictureParams.Builder()
        requireActivity().enterPictureInPictureMode(params.build())
        angelThumpStreamTitle.visibility = View.GONE
        angelThumpSeparator.visibility = View.GONE
        angelThumpClose.visibility = View.GONE
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (CurrentUser.tempStream != null && !hidden) {
            if (CurrentUser.tempStream!!.service == "m3u8") {
                player?.addMediaItem(MediaItem.fromUri(CurrentUser.tempStream!!.channel.toUri()))
            } else {
                player?.addMediaItem(MediaItem.fromUri("https://video-cdn.angelthump.com/hls/${CurrentUser.tempStream!!.channel}/index.m3u8".toUri()))
                angelThumpStreamTitle.text = CurrentUser.tempStream!!.title
            }
            player?.prepare()
            player?.play()
            angelThumpVideoView.hideController()
        } else {
            player?.stop()
            player?.removeMediaItems(0, player?.mediaItemCount!!)
        }
    }
}