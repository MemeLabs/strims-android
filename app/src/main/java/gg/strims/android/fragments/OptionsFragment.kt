package gg.strims.android.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import gg.strims.android.singletons.CurrentUser
import gg.strims.android.MainActivity
import gg.strims.android.databinding.FragmentChatOptionsBinding
import gg.strims.android.utils.viewBinding
import io.ktor.util.*

@KtorExperimentalAPI
class OptionsFragment : Fragment() {

    private val binding by viewBinding(FragmentChatOptionsBinding::bind)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentChatOptionsBinding.inflate(layoutInflater).root

    private fun retrieveOptions() {
        with (binding) {
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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        retrieveOptions()

        with (binding) {
            saveOptionsButton.setOnClickListener {
                CurrentUser.saveOptions(requireContext())
                (requireActivity() as MainActivity).onBackPressed()
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
}