package gg.strims.android.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import gg.strims.android.*
import gg.strims.android.models.Stream
import io.ktor.util.KtorExperimentalAPI
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
        hideFragment(activity!!, this)
        view.setOnTouchListener { _, _ -> return@setOnTouchListener true }
        val layoutManager = LinearLayoutManager(view.context)
        layoutManager.stackFromEnd = true
        recyclerViewStreams.layoutManager = layoutManager
        recyclerViewStreams.adapter = streamsAdapter
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (CurrentUser.streams != null && !hidden) {
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
            if (stream.live) {
                viewHolder.itemView.streamViewerCount.setTextColor(Color.parseColor("#FFFFFF"))
            } else {
                viewHolder.itemView.streamViewerCount.setTextColor(Color.parseColor("#F44336"))
            }

            viewHolder.itemView.setOnClickListener {
                hideFragment(activity!!, this@StreamsFragment)
                hideFragment(activity!!, fragmentManager!!.findFragmentById(R.id.angelthump_fragment)!!)
                hideFragment(activity!!, fragmentManager!!.findFragmentById(R.id.twitch_fragment)!!)
                hideFragment(activity!!, fragmentManager!!.findFragmentById(R.id.youtube_fragment)!!)
                val bottomNavigationView = activity!!.findViewById<BottomNavigationView>(R.id.chatBottomNavigationView)
                bottomNavigationView.selectedItemId = bottomNavigationView.menu.findItem(R.id.chatChat).itemId
                when (stream.service) {
                    "angelthump", "m3u8" -> {
                        CurrentUser.tempStream = stream
                        val fragment = fragmentManager!!.findFragmentById(R.id.angelthump_fragment)
                        showFragment(activity!!, fragment!!)
                    }
                    "twitch" -> {
                        CurrentUser.tempTwitchUrl = stream.channel
                        CurrentUser.tempTwitchVod = false
                        val fragment = fragmentManager!!.findFragmentById(R.id.twitch_fragment)
                        showFragment(activity!!, fragment!!)
                    }
                    "youtube" -> {
                        CurrentUser.tempYouTubeId = stream.channel
                        val fragment = fragmentManager!!.findFragmentById(R.id.youtube_fragment)
                        showFragment(activity!!, fragment!!)
                    }
                    "twitch-vod" -> {
                        CurrentUser.tempTwitchUrl = stream.channel
                        CurrentUser.tempTwitchVod = true
                        val fragment = fragmentManager!!.findFragmentById(R.id.twitch_fragment)
                        showFragment(activity!!, fragment!!)
                    }
                }
            }
        }
    }
}