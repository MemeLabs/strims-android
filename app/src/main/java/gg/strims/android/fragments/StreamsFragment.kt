package gg.strims.android.fragments

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import gg.strims.android.*
import gg.strims.android.models.Stream
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.activity_navigation_drawer.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_streams.*
import kotlinx.android.synthetic.main.stream_item.view.*

@SuppressLint("SetTextI18n")
@KtorExperimentalAPI
class StreamsFragment : Fragment() {

    private val streamsAdapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_streams, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val layoutManager = GridLayoutManager(view.context, 2)
            recyclerViewStreams.layoutManager = layoutManager
        } else {
            val layoutManager = LinearLayoutManager(view.context)
            recyclerViewStreams.layoutManager = layoutManager
        }
        recyclerViewStreams.adapter = streamsAdapter

        requireActivity().toolbar.title = "Streams"

        requireActivity().nav_view.setCheckedItem(R.id.nav_Streams)

        displayStreams()

        this.retainInstance = true
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
        val test = Gson().fromJson(input, JsonElement::class.java)
        when (test.asJsonArray[0].asString) {
            "RUSTLERS_SET" -> {
                if (CurrentUser.streams != null) {
                    CurrentUser.streams!!.forEach {
                        if (it.id == test.asJsonArray[1].asLong) {
                            it.rustlers = test.asJsonArray[2].asInt
                            it.afk_rustlers = test.asJsonArray[3].asInt
                        }
                    }
                }
            }
            "STREAMS_SET" -> {
                val streams2 = Gson().fromJson(test.asJsonArray[1], Array<Stream>::class.java)
                CurrentUser.streams = streams2.toMutableList()
            }
        }
    }

    private fun displayStreams() {
        if (CurrentUser.streams != null) {
            streamsAdapter.clear()
            CurrentUser.streams!!.sortByDescending {
                it.live
            }
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
        override fun getLayout(): Int {
            return R.layout.stream_item
        }

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
                for (i in 0..parentFragmentManager.backStackEntryCount) {
                    parentFragmentManager.popBackStack()
                }

                hideFragment(activity!!, parentFragmentManager.findFragmentById(R.id.angelthump_fragment)!!)
                hideFragment(activity!!, parentFragmentManager.findFragmentById(R.id.twitch_fragment)!!)
                hideFragment(activity!!, parentFragmentManager.findFragmentById(R.id.youtube_fragment)!!)
                val navView = requireActivity().findViewById<NavigationView>(R.id.nav_view)
                navView.setCheckedItem(R.id.nav_Chat)
                requireActivity().toolbar.title = "Chat"

                if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    activity!!.constraintLayoutStream.visibility = View.VISIBLE
                }

                when (stream.service) {
                    "angelthump", "m3u8" -> {
                        CurrentUser.tempStream = stream
                        val fragment = parentFragmentManager.findFragmentById(R.id.angelthump_fragment)
                        showFragment(activity!!, fragment!!)
                    }
                    "twitch" -> {
                        CurrentUser.tempTwitchUrl = stream.channel
                        CurrentUser.tempTwitchVod = false
                        val fragment = parentFragmentManager.findFragmentById(R.id.twitch_fragment)
                        showFragment(activity!!, fragment!!)
                    }
                    "youtube" -> {
                        CurrentUser.tempYouTubeId = stream.channel
                        val fragment = parentFragmentManager.findFragmentById(R.id.youtube_fragment)
                        showFragment(activity!!, fragment!!)
                    }
                    "twitch-vod" -> {
                        CurrentUser.tempTwitchUrl = stream.channel
                        CurrentUser.tempTwitchVod = true
                        val fragment = parentFragmentManager.findFragmentById(R.id.twitch_fragment)
                        showFragment(activity!!, fragment!!)
                    }
                }
            }
        }
    }
}