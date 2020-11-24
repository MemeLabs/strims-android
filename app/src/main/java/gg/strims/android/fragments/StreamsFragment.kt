package gg.strims.android.fragments

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
import com.xwray.groupie.viewbinding.BindableItem
import gg.strims.android.*
import gg.strims.android.databinding.FragmentStreamsBinding
import gg.strims.android.databinding.StreamItemBinding
import gg.strims.android.models.Stream
import gg.strims.android.singletons.CurrentUser
import gg.strims.android.viewmodels.ExoPlayerViewModel
import gg.strims.android.viewmodels.StreamsViewModel
import gg.strims.android.viewmodels.TwitchViewModel
import gg.strims.android.viewmodels.YouTubeViewModel
import io.ktor.util.*
import jp.wasabeef.blurry.Blurry
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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
    ): View {
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
        with (exoPlayerViewModel) {
            player?.release()
            player = null
            liveDataStream.value = null
        }
    }

    inner class StreamItem(private val stream: Stream): BindableItem<StreamItemBinding>() {

        override fun getLayout(): Int = R.layout.stream_item

        override fun bind(viewBinding: StreamItemBinding, position: Int) {
            with (viewBinding) {
                if (stream.thumbnail.isNotEmpty() && !stream.nsfw) {
                    Picasso.get().load(stream.thumbnail).into(streamThumbnail)
                } else if (stream.thumbnail.isNotEmpty() && stream.nsfw) {
                    var bitmap: Bitmap?
                    GlobalScope.launch {
                        bitmap = getBitmapFromURL(stream.thumbnail)

                        requireActivity().runOnUiThread {
                            Blurry.with(requireContext())
                                .radius(35)
                                .from(bitmap)
                                .into(streamThumbnail)
                        }
                    }
                } else {
                    Picasso.get().load(R.drawable.jigglymonkey)
                        .into(streamThumbnail)
                }
                streamTitle.text = resources.getString(
                    R.string.channel_presents_title_via_service,
                    stream.channel,
                    stream.title,
                    stream.service
                )
                streamViewerCount.text = stream.rustlers.toString()
                streamViewerState.setColorFilter(stream.colour)
                if (stream.live) {
                    constraintLayoutStreamViewers.background =
                        ResourcesCompat.getDrawable(
                            resources,
                            R.drawable.rounded_text_viewers_online,
                            null
                        )
                } else {
                    constraintLayoutStreamViewers.background =
                        ResourcesCompat.getDrawable(
                            resources,
                            R.drawable.rounded_text_viewers_offline,
                            null
                        )
                }

                if (stream.live) {
                    root.setOnClickListener {

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

                        (requireActivity() as MainActivity).onBackPressed()
                    }
                } else {
                    root.setOnClickListener(null)
                }
            }
        }

        override fun initializeViewBinding(view: View): StreamItemBinding {
            return StreamItemBinding.bind(view)
        }
    }
}