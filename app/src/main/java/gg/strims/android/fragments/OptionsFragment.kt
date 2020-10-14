package gg.strims.android.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import gg.strims.android.CurrentUser
import gg.strims.android.R
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_chat_options.*

@KtorExperimentalAPI
class OptionsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat_options, container, false)
    }

    private fun retrieveOptions() {
        if (CurrentUser.options != null) {
            checkBoxTimestamp.isChecked = CurrentUser.options!!.showTime
            checkBoxGreentext.isChecked = CurrentUser.options!!.greentext
            checkBoxHarshIgnore.isChecked = CurrentUser.options!!.harshIgnore
            checkBoxHideNsfw.isChecked = CurrentUser.options!!.hideNsfw
            checkBoxNotifications.isChecked = CurrentUser.options!!.notifications
            checkBoxEmotes.isChecked = CurrentUser.options!!.emotes

            ignoredUsersTextViewOptions.text =
                CurrentUser.options!!.ignoreList.toString()
                    .substringAfter('[').substringBefore(']')

            customHighlightsTextViewOptions.text =
                CurrentUser.options!!.customHighlights.toString()
                    .substringAfter('[').substringBefore(']')
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().toolbar.title = "Settings"

        retrieveOptions()

        saveOptionsButton.setOnClickListener {
            CurrentUser.saveOptions(requireContext())
            val recycler = requireActivity().findViewById<RecyclerView>(R.id.recyclerViewChat)
            recycler.adapter!!.notifyDataSetChanged()
        }

        checkBoxTimestamp.setOnCheckedChangeListener { buttonView, isChecked ->
            CurrentUser.options!!.showTime = isChecked
        }

        checkBoxGreentext.setOnCheckedChangeListener { buttonView, isChecked ->
            CurrentUser.options!!.greentext = isChecked
        }

        checkBoxHarshIgnore.setOnCheckedChangeListener { buttonView, isChecked ->
            CurrentUser.options!!.harshIgnore = isChecked
        }

        checkBoxHideNsfw.setOnCheckedChangeListener { buttonView, isChecked ->
            CurrentUser.options!!.hideNsfw = isChecked
        }

        checkBoxNotifications.setOnCheckedChangeListener { buttonView, isChecked ->
            CurrentUser.options!!.notifications = isChecked
        }

        checkBoxEmotes.setOnCheckedChangeListener { buttonView, isChecked ->
            CurrentUser.options!!.emotes = isChecked
        }
    }
}