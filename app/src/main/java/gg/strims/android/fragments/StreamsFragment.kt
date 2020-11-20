package gg.strims.android.fragments

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import gg.strims.android.CurrentUser
import gg.strims.android.R
import gg.strims.android.databinding.FragmentStreamsBinding
import gg.strims.android.getBitmapFromURL
import gg.strims.android.models.Stream
import gg.strims.android.viewBinding
import gg.strims.android.viewmodels.ExoPlayerViewModel
import gg.strims.android.viewmodels.StreamsViewModel
import gg.strims.android.viewmodels.TwitchViewModel
import gg.strims.android.viewmodels.YouTubeViewModel
import io.ktor.util.*
import jp.wasabeef.blurry.Blurry
import kotlinx.android.synthetic.main.stream_item.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@SuppressLint("SetTextI18n")
@KtorExperimentalAPI
class StreamsFragment : Fragment() {

    private val binding by viewBinding(FragmentStreamsBinding::bind)

    private val streamsAdapter = GroupAdapter<GroupieViewHolder>()

    private lateinit var exoPlayerViewModel: ExoPlayerViewModel
    private lateinit var twitchViewModel: TwitchViewModel
    private lateinit var youTubeViewModel: YouTubeViewModel
    private lateinit var streamsViewModel: StreamsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return FragmentStreamsBinding.inflate(layoutInflater).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        exoPlayerViewModel = ViewModelProvider(requireActivity()).get(ExoPlayerViewModel::class.java)
        twitchViewModel = ViewModelProvider(requireActivity()).get(TwitchViewModel::class.java)
        youTubeViewModel = ViewModelProvider(requireActivity()).get(YouTubeViewModel::class.java)
        streamsViewModel = ViewModelProvider(requireActivity()).get(StreamsViewModel::class.java)

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val layoutManager = GridLayoutManager(view.context, 2)
            binding.recyclerViewStreams.layoutManager = layoutManager
        } else {
            val layoutManager = LinearLayoutManager(view.context)
            binding.recyclerViewStreams.layoutManager = layoutManager
        }
        binding.recyclerViewStreams.adapter = streamsAdapter

        streamsViewModel.streams.observe(viewLifecycleOwner, {
            displayStreams(it)
        })
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val layoutManager = GridLayoutManager(view?.context, 2)
            binding.recyclerViewStreams.layoutManager = layoutManager
        } else {
            val layoutManager = LinearLayoutManager(view?.context)
            binding.recyclerViewStreams.layoutManager = layoutManager
        }
    }

    private fun displayStreams(streams: MutableList<Stream>) {
        streamsAdapter.clear()

        val sortedStreams = streams.sortedWith(
            compareBy(
                { it.live },
                { it.service == "angelthump" },
                { it.rustlers })
        ).reversed().toMutableList()

        sortedStreams.forEach {
            if (CurrentUser.user != null) {
                if (!CurrentUser.user!!.show_hidden && it.hidden) {
                    return@forEach
                }
            }
            streamsAdapter.add(StreamItem(it))
        }
        streamsAdapter.notifyDataSetChanged()
    }

    fun closeTwitch() {
        twitchViewModel.channel.value = null
        twitchViewModel.vod = false
    }

    fun closeYouTube() {
        youTubeViewModel.videoId.value = null
    }

    private fun closeAngelThump() {
        exoPlayerViewModel.player?.release()
        exoPlayerViewModel.player = null
        exoPlayerViewModel.liveDataStream.value = null
    }

    inner class StreamItem(private val stream: Stream): Item<GroupieViewHolder>() {

        override fun getLayout(): Int = R.layout.stream_item

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            if (stream.thumbnail.isNotEmpty() && !stream.nsfw) {
                Picasso.get().load(stream.thumbnail).into(viewHolder.itemView.streamThumbnail)
            } else if (stream.thumbnail.isNotEmpty() && stream.nsfw) {
                var bitmap: Bitmap?
                GlobalScope.launch {
                    bitmap = getBitmapFromURL(stream.thumbnail)

                    requireActivity().runOnUiThread {
                        Blurry.with(requireContext())
                            .radius(35)
                            .from(bitmap)
                            .into(viewHolder.itemView.streamThumbnail)
                    }
                }
            } else {
                Picasso.get().load(R.drawable.jigglymonkey).into(viewHolder.itemView.streamThumbnail)
            }
            viewHolder.itemView.streamTitle.text = "${stream.channel} presents ${stream.title} via ${stream.service}"
            viewHolder.itemView.streamViewerCount.text = stream.rustlers.toString()
            viewHolder.itemView.streamViewerState.setColorFilter(stream.colour)
            if (stream.live) {
                viewHolder.itemView.constraintLayoutStreamViewers.background =
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.rounded_text_viewers_online,
                        null
                    )
            } else {
                viewHolder.itemView.constraintLayoutStreamViewers.background =
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.rounded_text_viewers_offline,
                        null
                    )
            }

            viewHolder.itemView.setOnClickListener {

                when (stream.service) {
                    "angelthump", "m3u8" -> {
                        exoPlayerViewModel.liveDataStream.value = stream
                        closeTwitch()
                        closeYouTube()
                    }
                    "twitch" -> {
                        twitchViewModel.channel.value = stream.channel
                        closeAngelThump()
                        closeYouTube()
                    }
                    "youtube" -> {
                        youTubeViewModel.videoId.value = stream.channel
                        closeAngelThump()
                        closeTwitch()
                    }
                    "twitch-vod" -> {
                        twitchViewModel.channel.value = stream.channel
                        twitchViewModel.vod = true
                    }
                }

                requireActivity().onBackPressed()
            }
        }
    }
}