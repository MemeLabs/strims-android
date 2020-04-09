package gg.strims.android.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import gg.strims.android.CurrentUser
import gg.strims.android.R
import gg.strims.android.hideFragment
import gg.strims.android.models.Stream
import gg.strims.android.showFragment
import kotlinx.android.synthetic.main.fragment_strims_homepage.*
import kotlinx.android.synthetic.main.stream_item.view.*

@SuppressLint("SetTextI18n")
class HomeFragment : Fragment() {

    private val streamsAdapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_strims_homepage, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        hideFragment(activity!!, this)
        val layoutManager =
            LinearLayoutManager(view.context)
        layoutManager.stackFromEnd = true
        recyclerViewStreams.layoutManager = layoutManager
        recyclerViewStreams.adapter = streamsAdapter
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (CurrentUser.streams != null && !hidden) {
            CurrentUser.streams!!.sortByDescending {
                it.live
            }
            CurrentUser.streams!!.forEach {
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
                viewHolder.itemView.streamViewerCount.setTextColor(Color.parseColor("#4CAF50"))
            } else {
                viewHolder.itemView.streamViewerCount.setTextColor(Color.parseColor("#F44336"))
            }

            viewHolder.itemView.setOnClickListener {
                hideFragment(activity!!, this@HomeFragment)
                hideFragment(activity!!, fragmentManager!!.findFragmentById(R.id.angelthump_fragment)!!)
                hideFragment(activity!!, fragmentManager!!.findFragmentById(R.id.twitch_fragment)!!)
                hideFragment(activity!!, fragmentManager!!.findFragmentById(R.id.youtube_fragment)!!)
                when (stream.service) {
                    "angelthump" -> {
                        CurrentUser.tempStream = stream
                        val fragment = fragmentManager!!.findFragmentById(R.id.angelthump_fragment)
                        showFragment(activity!!, fragment!!)
                    }
                    "twitch" -> {
                        CurrentUser.tempTwitchUrl = stream.channel
                        val fragment = fragmentManager!!.findFragmentById(R.id.twitch_fragment)
                        showFragment(activity!!, fragment!!)
                    }
                    "youtube" -> {
                        CurrentUser.tempYouTubeId = stream.channel
                        val fragment = fragmentManager!!.findFragmentById(R.id.youtube_fragment)
                        showFragment(activity!!, fragment!!)
                    }
                }
            }
        }
    }
}