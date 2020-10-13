package gg.strims.android.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import gg.strims.android.ChatActivity
import gg.strims.android.CurrentUser
import gg.strims.android.R
import gg.strims.android.hideFragment
import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


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

//    private fun closeProfile() {
//        val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.chatBottomNavigationView)
//        bottomNavigationView.selectedItemId = bottomNavigationView.menu.findItem(R.id.chatChat).itemId
//        requireFragmentManager().beginTransaction()
//            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
//            .hide(this)
//            .commit()
//    }

    private fun deleteCookie() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.setOnTouchListener { _, _ -> return@setOnTouchListener true }
        streamingServiceSpinnerProfile.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                (view as TextView).setTextColor(Color.WHITE)
                view.textSize = 14f
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        closeProfile.setOnClickListener {
//            closeProfile()
        }

        saveProfile.setOnClickListener {
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

//            closeProfile()
        }

        logOutProfile.setOnClickListener {
            deleteCookie()
            startActivity(Intent(context, ChatActivity::class.java))
        }

        fetchProfile()
    }
}