package gg.strims.android.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import gg.strims.android.CurrentUser
import gg.strims.android.R
import io.ktor.util.*
import kotlinx.android.synthetic.main.fragment_chat_options.*

@KtorExperimentalAPI
@SuppressLint("CommitPrefEdits")
class OptionsFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences.Editor

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
            checkBoxViewerState.isChecked = CurrentUser.options!!.showViewerState
            checkBoxPictureInPicture.isChecked = CurrentUser.options!!.pictureInPicture

            ignoredUsersTextViewOptions.text =
                CurrentUser.options!!.ignoreList.toString()
                    .substringAfter('[').substringBefore(']')

            customHighlightsTextViewOptions.text =
                CurrentUser.options!!.customHighlights.toString()
                    .substringAfter('[').substringBefore(']')
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        sharedPreferences = requireActivity().getSharedPreferences("ChatOptions", Context.MODE_PRIVATE).edit()

//        requireActivity().toolbar.title = "Settings"
//
//        requireActivity().nav_view.setCheckedItem(R.id.nav_Settings)

        retrieveOptions()

        saveOptionsButton.setOnClickListener {
            CurrentUser.saveOptions(requireContext())
            requireActivity().onBackPressed()
        }

        checkBoxTimestamp.setOnCheckedChangeListener { _, isChecked ->
            CurrentUser.options?.showTime = isChecked
        }

        checkBoxGreentext.setOnCheckedChangeListener { _, isChecked ->
            CurrentUser.options?.greentext = isChecked
        }

        checkBoxHarshIgnore.setOnCheckedChangeListener { _, isChecked ->
            CurrentUser.options?.harshIgnore = isChecked
        }

        checkBoxHideNsfw.setOnCheckedChangeListener { _, isChecked ->
            CurrentUser.options?.hideNsfw = isChecked
        }

        checkBoxNotifications.setOnCheckedChangeListener { _, isChecked ->
            CurrentUser.options?.notifications = isChecked
        }

        checkBoxEmotes.setOnCheckedChangeListener { _, isChecked ->
            CurrentUser.options?.emotes = isChecked
        }

        checkBoxViewerState.setOnCheckedChangeListener { _, isChecked ->
            CurrentUser.options?.showViewerState = isChecked
        }

        checkBoxPictureInPicture.setOnCheckedChangeListener { _, isChecked ->
            CurrentUser.options?.pictureInPicture = isChecked
        }
    }
}