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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat_options, container, false)
    }

    private fun retrieveOptions() {
        if (CurrentUser.optionsLiveData.value != null) {
            checkBoxTimestamp.isChecked = CurrentUser.optionsLiveData.value?.showTime!!
            checkBoxGreentext.isChecked = CurrentUser.optionsLiveData.value?.greentext!!
            checkBoxHarshIgnore.isChecked = CurrentUser.optionsLiveData.value?.harshIgnore!!
            checkBoxHideNsfw.isChecked = CurrentUser.optionsLiveData.value?.hideNsfw!!
            checkBoxNotifications.isChecked = CurrentUser.optionsLiveData.value?.notifications!!
            checkBoxEmotes.isChecked = CurrentUser.optionsLiveData.value?.emotes!!
            checkBoxViewerState.isChecked = CurrentUser.optionsLiveData.value?.showViewerState!!
            checkBoxPictureInPicture.isChecked = CurrentUser.optionsLiveData.value?.pictureInPicture!!

            ignoredUsersTextViewOptions.text =
                CurrentUser.optionsLiveData.value?.ignoreList!!.toString()
                    .substringAfter('[').substringBefore(']')

            customHighlightsTextViewOptions.text =
                CurrentUser.optionsLiveData.value?.customHighlights!!.toString()
                    .substringAfter('[').substringBefore(']')
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        retrieveOptions()

        saveOptionsButton.setOnClickListener {
            CurrentUser.saveOptions(requireContext())
            requireActivity().onBackPressed()
        }

        checkBoxTimestamp.setOnCheckedChangeListener { _, isChecked ->
            CurrentUser.optionsLiveData.value?.showTime = isChecked
        }

        checkBoxGreentext.setOnCheckedChangeListener { _, isChecked ->
            CurrentUser.optionsLiveData.value?.greentext = isChecked
        }

        checkBoxHarshIgnore.setOnCheckedChangeListener { _, isChecked ->
            CurrentUser.optionsLiveData.value?.harshIgnore = isChecked
        }

        checkBoxHideNsfw.setOnCheckedChangeListener { _, isChecked ->
            CurrentUser.optionsLiveData.value?.hideNsfw = isChecked
        }

        checkBoxNotifications.setOnCheckedChangeListener { _, isChecked ->
            CurrentUser.optionsLiveData.value?.notifications = isChecked
        }

        checkBoxEmotes.setOnCheckedChangeListener { _, isChecked ->
            CurrentUser.optionsLiveData.value?.emotes = isChecked
        }

        checkBoxViewerState.setOnCheckedChangeListener { _, isChecked ->
            CurrentUser.optionsLiveData.value?.showViewerState = isChecked
        }

        checkBoxPictureInPicture.setOnCheckedChangeListener { _, isChecked ->
            CurrentUser.optionsLiveData.value?.pictureInPicture = isChecked
        }
    }
}