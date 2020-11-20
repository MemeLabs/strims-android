package gg.strims.android.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import gg.strims.android.CurrentUser
import gg.strims.android.databinding.FragmentChatOptionsBinding
import gg.strims.android.viewBinding
import io.ktor.util.*

@KtorExperimentalAPI
@SuppressLint("CommitPrefEdits")
class OptionsFragment : Fragment() {

    private val binding by viewBinding(FragmentChatOptionsBinding::bind)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return FragmentChatOptionsBinding.inflate(layoutInflater).root
    }

    private fun retrieveOptions() {
        if (CurrentUser.optionsLiveData.value != null) {
            binding.checkBoxTimestamp.isChecked = CurrentUser.optionsLiveData.value?.showTime!!
            binding.checkBoxGreentext.isChecked = CurrentUser.optionsLiveData.value?.greentext!!
            binding.checkBoxHarshIgnore.isChecked = CurrentUser.optionsLiveData.value?.harshIgnore!!
            binding.checkBoxHideNsfw.isChecked = CurrentUser.optionsLiveData.value?.hideNsfw!!
            binding.checkBoxNotifications.isChecked = CurrentUser.optionsLiveData.value?.notifications!!
            binding.checkBoxEmotes.isChecked = CurrentUser.optionsLiveData.value?.emotes!!
            binding.checkBoxViewerState.isChecked = CurrentUser.optionsLiveData.value?.showViewerState!!
            binding.checkBoxPictureInPicture.isChecked = CurrentUser.optionsLiveData.value?.pictureInPicture!!

            binding.ignoredUsersTextViewOptions.text =
                CurrentUser.optionsLiveData.value?.ignoreList!!.toString()
                    .substringAfter('[').substringBefore(']')

            binding.customHighlightsTextViewOptions.text =
                CurrentUser.optionsLiveData.value?.customHighlights!!.toString()
                    .substringAfter('[').substringBefore(']')
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        retrieveOptions()

        binding.saveOptionsButton.setOnClickListener {
            CurrentUser.saveOptions(requireContext())
            requireActivity().onBackPressed()
        }

        binding.checkBoxTimestamp.setOnCheckedChangeListener { _, isChecked ->
            CurrentUser.optionsLiveData.value?.showTime = isChecked
        }

        binding.checkBoxGreentext.setOnCheckedChangeListener { _, isChecked ->
            CurrentUser.optionsLiveData.value?.greentext = isChecked
        }

        binding.checkBoxHarshIgnore.setOnCheckedChangeListener { _, isChecked ->
            CurrentUser.optionsLiveData.value?.harshIgnore = isChecked
        }

        binding.checkBoxHideNsfw.setOnCheckedChangeListener { _, isChecked ->
            CurrentUser.optionsLiveData.value?.hideNsfw = isChecked
        }

        binding.checkBoxNotifications.setOnCheckedChangeListener { _, isChecked ->
            CurrentUser.optionsLiveData.value?.notifications = isChecked
        }

        binding.checkBoxEmotes.setOnCheckedChangeListener { _, isChecked ->
            CurrentUser.optionsLiveData.value?.emotes = isChecked
        }

        binding.checkBoxViewerState.setOnCheckedChangeListener { _, isChecked ->
            CurrentUser.optionsLiveData.value?.showViewerState = isChecked
        }

        binding.checkBoxPictureInPicture.setOnCheckedChangeListener { _, isChecked ->
            CurrentUser.optionsLiveData.value?.pictureInPicture = isChecked
        }
    }
}