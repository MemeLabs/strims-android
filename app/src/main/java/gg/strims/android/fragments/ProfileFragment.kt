package gg.strims.android.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import gg.strims.android.ChatActivity
import gg.strims.android.CurrentUser
import gg.strims.android.R
import gg.strims.android.models.Message
import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.activity_navigation_drawer.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.nav_header_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.HashMap

@KtorExperimentalAPI
class ProfileFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container,false)
    }

    private fun fetchProfile() {
        if (CurrentUser.user != null) {
            usernameEditTextProfile.text = CurrentUser.user!!.username
            streamPathEditTextProfile.setText(CurrentUser.user!!.stream_path)
            channelEditTextProfile.setText(CurrentUser.user!!.channel)
            checkBoxViewerState.isChecked = CurrentUser.user!!.enable_public_state

            val array = resources.getStringArray(R.array.streaming_service_spinner_names)
            array.forEach {
                if (CurrentUser.user!!.service == it) {
                    streamingServiceSpinnerProfile.setSelection(resources.getStringArray(R.array.streaming_service_spinner_names).indexOf(it))
                }
            }
        }
    }

    private fun deleteCookie() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().toolbar.title = "Profile"

        requireActivity().nav_view.setCheckedItem(R.id.nav_Profile)

        val spinnerArray = resources.getStringArray(R.array.streaming_service_spinner)

        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item, spinnerArray)
        arrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)

        streamingServiceSpinnerProfile.adapter = arrayAdapter

        saveProfile.setOnClickListener {
            if (CurrentUser.user != null) {
                val client = HttpClient {
                    install(WebSockets)
                }

                GlobalScope.launch {
                    CurrentUser.user!!.stream_path = streamPathEditTextProfile.text.toString()
                    CurrentUser.user!!.channel = channelEditTextProfile.text.toString()
                    CurrentUser.user!!.enable_public_state = checkBoxViewerState.isChecked
                    val id = resources.getStringArray(R.array.streaming_service_spinner_names)[streamingServiceSpinnerProfile.selectedItemPosition]
                    CurrentUser.user!!.service = id

                    client.post("https://strims.gg/api/profile") {
                        header("Cookie", "jwt=${CurrentUser.jwt}")
                        body = Gson().toJson(CurrentUser.user)
                    }
                }
            }
        }

        logOutProfile.setOnClickListener {
            CurrentUser.user = null
            CurrentUser.tempWhisperUser = null
            CurrentUser.whispersDictionary = HashMap<String, MutableList<Message>>()
            CurrentUser.privateMessageUsers = null

            deleteCookie()
            val activity = requireActivity() as ChatActivity
            requireActivity().stopService(activity.chatSocketIntent)
            requireActivity().startService(activity.chatSocketIntent)
            activity.onBackPressed()
            activity.invalidateOptionsMenu()
            activity.navHeaderUsername.text = resources.getString(R.string.anonymous)
            activity.nav_view.menu.findItem(R.id.nav_Profile).isVisible = false
            activity.nav_view.menu.findItem(R.id.nav_Whispers).isVisible = false
            activity.sendMessageText.hint = "Log in to send messages"
            requireActivity().progressBar.visibility = View.VISIBLE
        }

        fetchProfile()
    }
}