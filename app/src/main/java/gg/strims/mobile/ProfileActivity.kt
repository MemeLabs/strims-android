package gg.strims.mobile

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_profile.*

class ProfileActivity : AppCompatActivity() {

    @KtorExperimentalAPI
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        supportActionBar?.title = "Profile"

        usernameEditTextProfile.setText(CurrentUser.user!!.username)
        streamPathEditTextProfile.setText(CurrentUser.user!!.stream_path)
        channelEditTextProfile.setText(CurrentUser.user!!.channel)
    }
}
