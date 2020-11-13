package gg.strims.android.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import gg.strims.android.CurrentUser
import gg.strims.android.R
import gg.strims.android.models.Stream
import gg.strims.android.viewmodels.ExoPlayerViewModel
import io.ktor.util.*
import kotlinx.android.synthetic.main.fragment_streams.*
import kotlinx.android.synthetic.main.stream_item.view.*

@SuppressLint("SetTextI18n")
@KtorExperimentalAPI
class StreamsFragment : Fragment() {

    private val streamsAdapter = GroupAdapter<GroupieViewHolder>()

    private lateinit var exoPlayerViewModel: ExoPlayerViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_streams, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        exoPlayerViewModel = ViewModelProvider(this).get(ExoPlayerViewModel::class.java)

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val layoutManager = GridLayoutManager(view.context, 2)
            recyclerViewStreams.layoutManager = layoutManager
        } else {
            val layoutManager = LinearLayoutManager(view.context)
            recyclerViewStreams.layoutManager = layoutManager
        }
        recyclerViewStreams.adapter = streamsAdapter

        displayStreams()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val layoutManager = GridLayoutManager(view?.context, 2)
            recyclerViewStreams.layoutManager = layoutManager
        } else {
            val layoutManager = LinearLayoutManager(view?.context)
            recyclerViewStreams.layoutManager = layoutManager
        }
    }

    fun parseStream(input: String) {
        val stream = Gson().fromJson(input, JsonElement::class.java)
        when (stream.asJsonArray[0].asString) {
            "RUSTLERS_SET" -> {
                if (CurrentUser.streams != null) {
                    CurrentUser.streams!!.forEach {
                        if (it.id == stream.asJsonArray[1].asLong) {
                            it.rustlers = stream.asJsonArray[2].asInt
                            it.afk_rustlers = stream.asJsonArray[3].asInt
                        }
                    }
                }
            }
            "STREAMS_SET" -> {
                val streams = Gson().fromJson(stream.asJsonArray[1], Array<Stream>::class.java)
                CurrentUser.streams = streams.toMutableList()
            }
        }
    }

    private fun displayStreams() {
        if (CurrentUser.streams != null) {
            streamsAdapter.clear()

            CurrentUser.streams = CurrentUser.streams!!.sortedWith(
                compareBy(
                    { it.live },
                    { it.service == "angelthump" },
                    { it.rustlers })
            ).reversed().toMutableList()

            CurrentUser.streams!!.forEach {
                if (CurrentUser.user != null) {
                    if (!CurrentUser.user!!.show_hidden && it.hidden) {
                        return@forEach
                    }
                }
                streamsAdapter.add(StreamItem(it))
            }
        }
        recyclerViewStreams.scrollToPosition(0)
        streamsAdapter.notifyDataSetChanged()
    }

    inner class StreamItem(private val stream: Stream): Item<GroupieViewHolder>() {

        override fun getLayout(): Int = R.layout.stream_item

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            if (stream.thumbnail.isNotEmpty()) {
                Picasso.get().load(stream.thumbnail).into(viewHolder.itemView.streamThumbnail)
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
                CurrentUser.tempTwitchUrl = null
                CurrentUser.tempTwitchVod = null
                CurrentUser.tempStream = null
                CurrentUser.tempYouTubeId = null

                requireActivity().onBackPressed()

                when (stream.service) {
                    "angelthump", "m3u8" -> {
                        CurrentUser.tempStream = stream
                    }
                    "twitch" -> {
                        CurrentUser.tempTwitchUrl = stream.channel
                        CurrentUser.tempTwitchVod = false
                    }
                    "youtube" -> {
                        CurrentUser.tempYouTubeId = stream.channel
                    }
                    "twitch-vod" -> {
                        CurrentUser.tempTwitchUrl = stream.channel
                        CurrentUser.tempTwitchVod = true
                    }
                }
                requireContext().sendBroadcast(Intent("gg.strims.android.SHOWSTREAM"))
            }
        }
    }
}