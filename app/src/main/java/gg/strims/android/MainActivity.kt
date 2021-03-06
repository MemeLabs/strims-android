package gg.strims.android

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.beust.klaxon.Klaxon
import com.google.android.material.navigation.NavigationView
import com.melegy.redscreenofdeath.RedScreenOfDeath
import gg.strims.android.clients.ChatService
import gg.strims.android.clients.StreamsService
import gg.strims.android.databinding.ActivityNavigationDrawerBinding
import gg.strims.android.fragments.AngelThumpFragment
import gg.strims.android.models.EmotesParsed
import gg.strims.android.models.Options
import gg.strims.android.singletons.CurrentUser
import gg.strims.android.utils.getBitmapFromURL
import gg.strims.android.utils.getGifFromURL
import gg.strims.android.utils.hideChildFragment
import gg.strims.android.utils.viewBinder
import gg.strims.android.viewmodels.ChatViewModel
import gg.strims.android.viewmodels.ExoPlayerViewModel
import io.ktor.util.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URL
import kotlin.system.exitProcess

@KtorExperimentalAPI
class MainActivity : AppCompatActivity() {

    val binding by viewBinder(ActivityNavigationDrawerBinding::inflate)

    companion object {
        var channelId = "chat_notifications"
        var NOTIFICATION_ID = 1
        var NOTIFICATION_REPLY_KEY = "Text"
    }

    private lateinit var navHostFragment: NavHostFragment
    private lateinit var appBarConfiguration: AppBarConfiguration

    lateinit var chatViewModel: ChatViewModel
    private lateinit var exoPlayerViewModel: ExoPlayerViewModel

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            if (exoPlayerViewModel.player != null) {
                exoPlayerViewModel.player?.release()
                exoPlayerViewModel.player = null
            }
            stopService(chatViewModel.streamsSocketIntent)
            stopService(chatViewModel.chatSocketIntent)
            finish()
            exitProcess(0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_Chat
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        RedScreenOfDeath.init(application)

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            supportActionBar?.hide()
        }

        chatViewModel = ViewModelProvider(this).get(ChatViewModel::class.java)
        exoPlayerViewModel = ViewModelProvider(this).get(ExoPlayerViewModel::class.java)

        CurrentUser.optionsLiveData.observe(this, {
            CurrentUser.saveOptions(this)
            Log.d("TAG", "SAVING OPTIONS")
        })

        if (savedInstanceState != null && CurrentUser.user != null) {
            binding.navView.menu.findItem(R.id.nav_Profile).isVisible = true
            binding.navView.menu.findItem(R.id.nav_Whispers).isVisible = true
            val header = navView.getHeaderView(0)
            header.navHeaderUsername.text = CurrentUser.user!!.username
        } else if (savedInstanceState == null) {
            with(chatViewModel) {
                chatSocketIntent = Intent(this@MainActivity, ChatService::class.java)
                streamsSocketIntent = Intent(this@MainActivity, StreamsService::class.java)
                startService(chatSocketIntent)
                startService(streamsSocketIntent)
            }

            GlobalScope.launch(Dispatchers.IO) {
                Log.d("TAG", "OPTIONS ${(System.currentTimeMillis() - CurrentUser.time)}")
                retrieveOptions()
                Log.d("TAG", "OPTIONS ENDING ${(System.currentTimeMillis() - CurrentUser.time)}")
            }

            GlobalScope.launch(Dispatchers.IO) {
                Log.d("TAG", "EMOTES ${(System.currentTimeMillis() - CurrentUser.time)}")
                retrieveEmotes()
                Log.d("TAG", "EMOTES ENDING ${(System.currentTimeMillis() - CurrentUser.time)}")
            }
        }
    }

    fun restartChatService() {
        with (chatViewModel) {
            stopService(chatSocketIntent)
            chatSocketIntent = Intent(this@MainActivity, ChatService::class.java)
            Thread.sleep(100)
            startService(chatSocketIntent)
        }
    }

    fun restartStreamsService() {
        with (chatViewModel) {
            stopService(streamsSocketIntent)
            streamsSocketIntent = Intent(this@MainActivity, StreamsService::class.java)
            Thread.sleep(100)
            startService(streamsSocketIntent)
        }
    }

    private fun retrieveEmotes() {
        val text = URL("https://chat.strims.gg/emote-manifest.json").readText()
        val emotesParsed: EmotesParsed = Klaxon().parse(text)!!
        CurrentUser.emotes = emotesParsed.emotes.toMutableList()
        cacheEmotes()
    }

    private fun retrieveOptions() {
        val sharedPreferences = getSharedPreferences("ChatOptions", Context.MODE_PRIVATE)
        val options = sharedPreferences.getString("options", "")
        if (options != null && options.isNotEmpty()) {
            runOnUiThread {
                CurrentUser.optionsLiveData.value = Klaxon().parse(options)
            }
        } else {
            runOnUiThread {
                CurrentUser.optionsLiveData.value = Options()
            }
        }
    }

    private fun cacheEmotes() {
        CurrentUser.emotes?.forEach {
            val size = it.versions.size - 1
            val biggestEmote = it.versions[size]
            val url = "https://chat.strims.gg/${biggestEmote.path}"
            if (!biggestEmote.animated) {
                GlobalScope.launch {
                    val bitmap = getBitmapFromURL(url)
                    CurrentUser.bitmapMemoryCache[it.name] = bitmap!!
                }
            } else {
                GlobalScope.launch {
                    val gif = getGifFromURL(url)
                    CurrentUser.gifMemoryCache[it.name] = gif!!
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.navigation_drawer_options, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu!!.findItem(R.id.nav_LogIn).isVisible = CurrentUser.user == null
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_LogIn -> {
                val navController = navHostFragment.navController
                navController.navigate(R.id.nav_LogIn)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = navHostFragment.navController
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        val childFragment = navHostFragment.childFragmentManager.fragments[0]
        childFragment.childFragmentManager.fragments.forEach {
            if (it.tag == "EmotesMenuFragment" || it.tag == "UserListFragment" && it.isVisible) {
                hideChildFragment(navHostFragment.childFragmentManager.fragments[0], it)
                return
            }
        }
        if (binding.navView.checkedItem?.title == "Chat" && supportActionBar?.title == "Chat") {
            return
        }
        super.onBackPressed()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (CurrentUser.optionsLiveData.value?.pictureInPicture!!) {
            val childFragment = navHostFragment.childFragmentManager.fragments[0]
            if (exoPlayerViewModel.liveDataStream.value != null) {
                childFragment.childFragmentManager.fragments.forEach {
                    if (it.tag == "AngelThumpFragment") {
                        (it as AngelThumpFragment).enterPIPMode()
                        return
                    }
                }
            }
        }
    }
}
