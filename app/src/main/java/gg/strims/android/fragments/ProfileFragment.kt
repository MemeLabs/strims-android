package gg.strims.android.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import gg.strims.android.MainActivity
import gg.strims.android.singletons.CurrentUser
import gg.strims.android.R
import gg.strims.android.databinding.FragmentProfileBinding
import gg.strims.android.viewBinding
import gg.strims.android.viewmodels.ProfileViewModel
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.util.*
import kotlinx.android.synthetic.main.nav_header_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@KtorExperimentalAPI
class ProfileFragment: Fragment() {

    private val binding by viewBinding(FragmentProfileBinding::bind)

    private lateinit var profileViewModel: ProfileViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentProfileBinding.inflate(layoutInflater).root
    }

    private fun fetchProfile() {
        if (CurrentUser.user != null) {
            with (binding) {
                usernameEditTextProfile.text = CurrentUser.user!!.username
                streamPathEditTextProfile.setText(CurrentUser.user!!.stream_path)
                channelEditTextProfile.setText(CurrentUser.user!!.channel)
                checkBoxUserViewerState.isChecked = CurrentUser.user!!.enable_public_state

                val array = resources.getStringArray(R.array.streaming_service_spinner_names)
                array.forEach {
                    if (CurrentUser.user!!.service == it) {
                        streamingServiceSpinnerProfile.setSelection(
                            resources.getStringArray(
                                R.array.streaming_service_spinner_names
                            ).indexOf(it)
                        )
                    }
                }
            }
        }
    }

    private fun deleteCookie() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        profileViewModel = ViewModelProvider(requireActivity()).get(ProfileViewModel::class.java)

        val spinnerArray = resources.getStringArray(R.array.streaming_service_spinner)

        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, spinnerArray)
        arrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)

        binding.streamingServiceSpinnerProfile.adapter = arrayAdapter

        binding.saveProfile.setOnClickListener {
            if (CurrentUser.user != null) {
                val client = HttpClient {
                    install(WebSockets)
                }

                GlobalScope.launch {
                    val id = resources.getStringArray(R.array.streaming_service_spinner_names)[binding.streamingServiceSpinnerProfile.selectedItemPosition]
                    with (CurrentUser.user!!) {
                        service = id
                        stream_path = binding.streamPathEditTextProfile.text.toString()
                        channel = binding.channelEditTextProfile.text.toString()
                        enable_public_state = binding.checkBoxUserViewerState.isChecked
                    }

                    client.post("https://strims.gg/api/profile") {
                        header("Cookie", "jwt=${profileViewModel.jwt}")
                        body = Gson().toJson(CurrentUser.user)
                    }
                }
            }
            (requireActivity() as MainActivity).onBackPressed()
        }

        binding.logOutProfile.setOnClickListener {
            CurrentUser.user = null
            deleteCookie()
            with (requireActivity() as MainActivity) {
                stopService(chatViewModel.chatSocketIntent)
                startService(chatViewModel.chatSocketIntent)
                onBackPressed()
                invalidateOptionsMenu()
                navHeaderUsername.text = resources.getString(R.string.anonymous)
                binding.navView.menu.findItem(R.id.nav_Profile).isVisible = false
                binding.navView.menu.findItem(R.id.nav_Whispers).isVisible = false
            }
            profileViewModel.logOut.value = true
        }

        fetchProfile()
    }
}