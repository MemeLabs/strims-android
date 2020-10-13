package gg.strims.android

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.*
import android.util.DisplayMetrics
import android.util.Log
import android.util.LruCache
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.app.TaskStackBuilder
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.beust.klaxon.Klaxon
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import gg.strims.android.clients.StrimsClient
import gg.strims.android.customspans.CenteredImageSpan
import gg.strims.android.customspans.ColouredUnderlineSpan
import gg.strims.android.customspans.NoUnderlineClickableSpan
import gg.strims.android.fragments.OptionsFragment
import gg.strims.android.fragments.ProfileFragment
import gg.strims.android.fragments.StreamsFragment
import gg.strims.android.models.*
import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.wss
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readBytes
import io.ktor.http.cio.websocket.readText
import io.ktor.http.cio.websocket.send
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.activity_navigation_drawer.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.autofill_item.view.*
import kotlinx.android.synthetic.main.chat_message_item.view.*
import kotlinx.android.synthetic.main.chat_message_item_emote_combo.view.*
import kotlinx.android.synthetic.main.error_chat_message_item.view.*
import kotlinx.android.synthetic.main.nav_header_main.*
import kotlinx.android.synthetic.main.private_chat_message_item.view.*
import kotlinx.android.synthetic.main.whisper_message_item_right.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import pl.droidsonroids.gif.GifDrawable
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.regex.Matcher
import java.util.regex.Pattern

@KtorExperimentalAPI
@SuppressLint("SetTextI18n", "SimpleDateFormat", "WrongViewCast")
class ChatActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    companion object {
        var channelId = "chat_notifications"
        var NOTIFICATION_ID = 1
        var NOT_USER_KEY = "NOT_USER_KEY"
        var NOTIFICATION_REPLY_KEY = "Text"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration

    val adapter = GroupAdapter<GroupieViewHolder>()

    private lateinit var bitmapMemoryCache: LruCache<String, Bitmap>

    private lateinit var gifMemoryCache: LruCache<String, GifDrawable>

    private var privateMessageArray = arrayOf("w", "whisper", "msg", "tell", "t", "notify")

    private val autofillAdapter = GroupAdapter<GroupieViewHolder>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation_drawer)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_Streams, R.id.nav_Profile, R.id.nav_Settings
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.setNavigationItemSelectedListener(this)

        navView.menu.getItem(0).isChecked = false

        GlobalScope.launch {
            ChatClient().onConnect()
        }

        GlobalScope.launch {
            StrimsClient().onConnect()
        }

        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        bitmapMemoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }
        gifMemoryCache = object : LruCache<String, GifDrawable>(cacheSize) {}

        sendMessageText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                sendMessageButton.isEnabled = sendMessageText.text.isNotEmpty()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                sendMessageButton.isEnabled = sendMessageText.text.isNotEmpty()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                sendMessageButton.isEnabled = sendMessageText.text.isNotEmpty()
                autofillAdapter.clear()
                if (sendMessageText.text.isNotEmpty()) {
                    recyclerViewAutofill.visibility = View.VISIBLE
                    if (sendMessageText.text.first() == '/' && !sendMessageText.text.contains(' ')) {
                        val currentWord = sendMessageText.text.toString().substringAfter('/')

                        privateMessageArray.forEach {
                            if (it.contains(currentWord, true)) {
                                autofillAdapter.add(AutofillItemCommand(it))
                            }
                        }
                    } else {

                        val currentWord = sendMessageText.text.toString().substringAfterLast(' ')
                        CurrentUser.users!!.sortByDescending {
                            it.nick
                        }
                        CurrentUser.users!!.forEach {
                            if (it.nick.contains(currentWord, true)) {
                                autofillAdapter.add(AutofillItemUser(it))
                            }
                        }
                        CurrentUser.emotes!!.forEach {
                            if (it.name.contains(currentWord, true)) {
                                autofillAdapter.add(AutofillItemEmote(it))
                            }
                        }
                    }
                } else if (sendMessageText.text.isEmpty() || sendMessageText.text.last() == ' ') {
                    recyclerViewAutofill.visibility = View.GONE
                }
            }
        })

        sendMessageText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessageButton.performClick()
            }
            true
        }

        sendMessageText.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                sendMessageButton.performClick()
            }
            false
        }

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerViewChat.layoutManager = layoutManager
        recyclerViewChat.adapter = adapter

        recyclerViewChat.setOnScrollChangeListener { _, _, _, _, _ ->
            val layoutTest = recyclerViewChat.layoutManager as LinearLayoutManager
            val lastItem = layoutTest.findLastVisibleItemPosition()
            if (lastItem < recyclerViewChat.adapter!!.itemCount - 1) {
                goToBottom.visibility = View.VISIBLE
                goToBottom.isEnabled = true
            } else {
                goToBottom.visibility = View.GONE
                goToBottom.isEnabled = false
            }
        }

        recyclerViewChat.itemAnimator = null

        recyclerViewAutofill.adapter = autofillAdapter
        recyclerViewAutofill.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        goToBottom.setOnClickListener {
            recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
        }

//        optionsButton.setOnClickListener {
//            goToBottom.visibility = View.GONE
//            hideKeyboardFrom(this, sendMessageText)
//            val fragment = supportFragmentManager.findFragmentById(R.id.user_list_fragment)
//            if (!fragment!!.isHidden) {
//                showHideFragment(this, fragment)
//            }
//            showHideFragment(this, supportFragmentManager.findFragmentById(R.id.options_fragment)!!)
//        }

        userListButton.setOnClickListener {
            goToBottom.visibility = View.GONE
            hideKeyboardFrom(this, sendMessageText)
            showHideFragment(
                this,
                supportFragmentManager.findFragmentById(R.id.user_list_fragment)!!
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.navigation_drawer, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu!!.findItem(R.id.optionsLogIn).isVisible = CurrentUser.user == null
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.optionsLogIn -> {
                showFragment(this, supportFragmentManager.findFragmentById(R.id.login_fragment)!!)
            }
            android.R.id.home -> {

            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_Streams -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, StreamsFragment(), "StreamsFragment")
                    .addToBackStack("StreamsFragment").commit()
            }

            R.id.nav_Chat -> {
                supportFragmentManager.fragments.forEach {
                    supportFragmentManager.beginTransaction().remove(it).commit()
                }
            }

            R.id.nav_Profile -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, ProfileFragment(), "ProfileFragment")
                    .addToBackStack("ProfileFragment").commit()
            }

            R.id.nav_Settings -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, OptionsFragment(), "OptionsFragment")
                    .addToBackStack("OptionsFragment").commit()
            }
        }
        drawer_layout.closeDrawers()
        return true
    }

    override fun onBackPressed() {
        if (supportFragmentManager.fragments.size > 1) {
            supportFragmentManager.popBackStack()
            supportFragmentManager.beginTransaction()
                .remove(supportFragmentManager.fragments[1])
                .commit()
            if (supportFragmentManager.backStackEntryCount > 1) {
                val fragment =
                    supportFragmentManager.getBackStackEntryAt(supportFragmentManager.backStackEntryCount - 2)
                when (fragment.name) {
                    "ProfileFragment" -> {
                        nav_view.setCheckedItem(R.id.nav_Profile)
                    }

                    "StreamsFragment" -> {
                        nav_view.setCheckedItem(R.id.nav_Streams)
                    }

                    "OptionsFragment" -> {
                        nav_view.setCheckedItem(R.id.nav_Settings)
                    }
                }
            } else {
                nav_view.setCheckedItem(R.id.nav_Chat)
                toolbar.title = "Chat"
            }
        } else {
            super.onBackPressed()
        }
    }

    fun createMessageTextView(
        messageData: Message,
        messageTextView: TextView,
        emotes: Boolean = true,
        greentext: Boolean = true,
        links: Boolean = true,
        codes: Boolean = true,
        spoilers: Boolean = true,
        me: Boolean = true
    ) {
        val ssb = SpannableStringBuilder(messageData.data)

        if (CurrentUser.options!!.emotes && emotes) {
            if (messageData.entities.emotes != null && messageData.entities.emotes!!.isNotEmpty() && messageData.entities.emotes!![0].name != "") {
                messageData.entities.emotes!!.forEach {
                    var animated = false
                    CurrentUser.emotes!!.forEach { it2 ->
                        if (it.name == it2.name && it2.versions[0].animated) {
                            animated = true
                        }
                    }
                    if (!animated) {
                        var bitmap: Bitmap? = null
                        while (bitmap == null) {
                            bitmap = bitmapMemoryCache.get(it.name)
                        }
                        var width = bitmap.width * 0.75
                        if (it.modifiers.contains("wide")) {
                            width = (bitmap.width * 1.5)
                        }
                        val height = bitmap.height * 0.75
                        val resized =
                            Bitmap.createScaledBitmap(bitmap, width.toInt(), height.toInt(), false)
                        ssb.setSpan(
                            CenteredImageSpan(
                                this@ChatActivity,
                                resized
                            ),
                            it.bounds[0],
                            it.bounds[1],
                            Spannable.SPAN_INCLUSIVE_INCLUSIVE
                        )
                    } else {
                        var gif: GifDrawable? = null
                        while (gif == null) {
                            gif = gifMemoryCache.get(it.name)
                        }
                        val animatedEmote = ImageSpan(gif)
                        animatedEmote.drawable.setBounds(0, 0, gif.minimumWidth, gif.minimumHeight)
                        ssb.setSpan(
                            animatedEmote,
                            it.bounds[0],
                            it.bounds[1],
                            Spannable.SPAN_INCLUSIVE_INCLUSIVE
                        )
                    }
                }
            }
        }
        if (messageData.entities.greentext!!.bounds.isNotEmpty() && greentext) {
            ssb.setSpan(
                ForegroundColorSpan(Color.parseColor("#789922")),
                messageData.entities.greentext!!.bounds[0],
                messageData.entities.greentext!!.bounds[1],
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
        }

        if (messageData.entities.links!!.isNotEmpty() && links) {
            messageData.entities.links!!.forEach {
                val clickSpan: ClickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        if (messageData.entities.spoilers!!.isNotEmpty()) {
                            messageData.entities.spoilers!!.forEach { it2 ->
                                if (it.bounds[0] >= it2.bounds[0] && it.bounds[1] <= it2.bounds[1]) {
                                    val span3 = ssb.getSpans(
                                        it.bounds[0],
                                        it.bounds[1],
                                        ForegroundColorSpan::class.java
                                    )
                                    if (span3[span3.size - 1].foregroundColor == Color.parseColor(
                                            "#FFFFFF"
                                        ) ||
                                        span3[span3.size - 1].foregroundColor == Color.parseColor(
                                            "#03DAC5"
                                        )
                                    ) {
                                        var webpage = Uri.parse(it.url)

                                        if (!it.url!!.startsWith("http://") && !it.url!!.startsWith(
                                                "https://"
                                            )
                                        ) {
                                            webpage = Uri.parse("http://${it.url}")
                                        }

                                        val intent = Intent(Intent.ACTION_VIEW, webpage)
                                        if (intent.resolveActivity(packageManager) != null) {
                                            startActivity(intent)
                                        }
                                    }
                                }
                            }
                        } else {
                            var webpage = Uri.parse(it.url)

                            if (!it.url!!.startsWith("http://") && !it.url!!.startsWith(
                                    "https://"
                                )
                            ) {
                                webpage = Uri.parse("http://${it.url}")
                            }

                            val intent = Intent(Intent.ACTION_VIEW, webpage)
                            if (intent.resolveActivity(packageManager) != null) {
                                startActivity(intent)
                            }
                        }
                    }
                }
                if (messageData.entities.codes!!.isNotEmpty()) {
                    messageData.entities.codes!!.forEach { it2 ->
                        if (it.bounds[0] >= it2.bounds[0] && it.bounds[1] <= it2.bounds[1]) {
                            return@forEach
                        } else {
                            ssb.setSpan(
                                clickSpan,
                                it.bounds[0],
                                it.bounds[1],
                                Spannable.SPAN_INCLUSIVE_INCLUSIVE
                            )
                        }
                    }
                } else {
                    ssb.setSpan(
                        clickSpan,
                        it.bounds[0],
                        it.bounds[1],
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                    )
                }
            }
            if (messageData.data.contains("nsfl")) {
                messageData.entities.links!!.forEach {
                    ssb.setSpan(
                        ColouredUnderlineSpan(Color.parseColor("#FFFF00")),
                        it.bounds[0],
                        it.bounds[1],
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                    )
                }
            } else if (messageData.data.contains("nsfw")) {
                messageData.entities.links!!.forEach {
                    ssb.setSpan(
                        ColouredUnderlineSpan(Color.parseColor("#FF2D00")),
                        it.bounds[0],
                        it.bounds[1],
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                    )
                }
            }
        }

        if (messageData.entities.codes!!.isNotEmpty() && codes) {
            messageData.entities.codes!!.forEach {
                ssb.setSpan(
                    BackgroundColorSpan(Color.parseColor("#353535")),
                    it.bounds[0],
                    it.bounds[1],
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                ssb.setSpan(
                    ForegroundColorSpan(Color.parseColor("#D8D8D8")),
                    it.bounds[0],
                    it.bounds[1],
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                ssb.setSpan(
                    TypefaceSpan("monospace"),
                    it.bounds[0],
                    it.bounds[1],
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                ssb.setSpan(
                    RelativeSizeSpan(0f),
                    it.bounds[0],
                    it.bounds[0] + 1,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                ssb.setSpan(
                    RelativeSizeSpan(0f),
                    it.bounds[1] - 1,
                    it.bounds[1],
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                if (messageData.entities.links!!.isNotEmpty()) {
                    messageData.entities.links!!.forEach { it2 ->
                        if (it2.bounds[0] >= it.bounds[0] && it2.bounds[1] <= it.bounds[1]) {
                            val span3 = ssb.getSpans(
                                it2.bounds[0],
                                it2.bounds[1],
                                ColouredUnderlineSpan::class.java
                            )
                            if (span3.isNotEmpty()) {
                                span3[span3.size - 1].color = Color.parseColor("#00000000")
                            }
                        }
                    }
                }
            }
        }

        if (messageData.entities.spoilers!!.isNotEmpty() && spoilers) {
            messageData.entities.spoilers!!.forEach {
                val span1: NoUnderlineClickableSpan = object : NoUnderlineClickableSpan() {
                    override fun onClick(widget: View) {
                        val span = ssb.getSpans(
                            it.bounds[0], it.bounds[1],
                            ForegroundColorSpan::class.java
                        )
                        if (span[span.size - 1].foregroundColor == Color.parseColor("#00000000")) {
                            ssb.setSpan(
                                ForegroundColorSpan(Color.parseColor("#FFFFFF")),
                                it.bounds[0] + 2,
                                it.bounds[1] - 2,
                                Spannable.SPAN_INCLUSIVE_INCLUSIVE
                            )
                            if (messageData.entities.links!!.isNotEmpty()) {
                                messageData.entities.links!!.forEach { it2 ->
                                    if (it2.bounds[0] >= it.bounds[0] && it2.bounds[1] <= it.bounds[1]) {
                                        ssb.setSpan(
                                            ForegroundColorSpan(Color.parseColor("#03DAC5")),
                                            it2.bounds[0],
                                            it2.bounds[1],
                                            Spannable.SPAN_INCLUSIVE_INCLUSIVE
                                        )
                                        messageData.entities.tags!!.forEach { it3 ->
                                            if (it3.name == "nsfl") {
                                                ssb.setSpan(
                                                    ColouredUnderlineSpan(Color.parseColor("#FFFF00")),
                                                    it2.bounds[0],
                                                    it2.bounds[1],
                                                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                                                )
                                            } else if (it3.name == "nsfw") {
                                                ssb.setSpan(
                                                    ColouredUnderlineSpan(Color.parseColor("#FF2D00")),
                                                    it2.bounds[0],
                                                    it2.bounds[1],
                                                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (span[span.size - 1].foregroundColor == Color.parseColor("#FFFFFF") ||
                            span[span.size - 1].foregroundColor == Color.parseColor("#03DAC5")
                        ) {
                            ssb.setSpan(
                                ForegroundColorSpan(Color.parseColor("#00000000")),
                                it.bounds[0] + 2,
                                it.bounds[1] - 2,
                                Spannable.SPAN_INCLUSIVE_INCLUSIVE
                            )
                            if (messageData.entities.links!!.isNotEmpty()) {
                                messageData.entities.links!!.forEach { it2 ->
                                    if (it2.bounds[0] >= it.bounds[0] && it2.bounds[1] <= it.bounds[1]) {
                                        val span3 = ssb.getSpans(
                                            it2.bounds[0],
                                            it2.bounds[1],
                                            ColouredUnderlineSpan::class.java
                                        )
                                        if (span3.isNotEmpty()) {
                                            span3[span3.size - 1].color =
                                                Color.parseColor("#00000000")
                                        }
                                    }
                                }
                            }
                        }
                        messageTextView.setText(
                            ssb,
                            TextView.BufferType.SPANNABLE
                        )
                    }
                }

                if (messageData.entities.links!!.isNotEmpty()) {
                    messageData.entities.links!!.forEach { it2 ->
                        if (it2.bounds[0] >= it.bounds[0] && it2.bounds[1] <= it.bounds[1]) {
                            val span3 = ssb.getSpans(
                                it2.bounds[0],
                                it2.bounds[1],
                                ColouredUnderlineSpan::class.java
                            )
                            if (span3.isNotEmpty()) {
                                span3[span3.size - 1].color = Color.parseColor("#00000000")
                            }
                        }
                    }
                }

                ssb.setSpan(
                    span1,
                    it.bounds[0],
                    it.bounds[1],
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                ssb.setSpan(
                    RelativeSizeSpan(0f),
                    it.bounds[0],
                    it.bounds[0] + 2,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                ssb.setSpan(
                    RelativeSizeSpan(0f),
                    it.bounds[1] - 2,
                    it.bounds[1],
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                ssb.setSpan(
                    BackgroundColorSpan(Color.parseColor("#353535")),
                    it.bounds[0],
                    it.bounds[1],
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                ssb.setSpan(
                    ForegroundColorSpan(Color.parseColor("#00000000")),
                    it.bounds[0],
                    it.bounds[1],
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
            }
        }
        if (messageData.entities.me!!.bounds.isNotEmpty() && me) {
            messageTextView.setTypeface(
                Typeface.DEFAULT,
                Typeface.ITALIC
            )
            ssb.setSpan(
                RelativeSizeSpan(0f),
                0,
                3,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
        } else {
            messageTextView.typeface = Typeface.DEFAULT
        }
        messageTextView.setText(ssb, TextView.BufferType.SPANNABLE)
    }

    inner class AutofillItemCommand(private val command: String) : Item<GroupieViewHolder>() {
        override fun getLayout(): Int {
            return R.layout.autofill_item
        }

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.usernameAutofill.text = "/$command"

            viewHolder.itemView.usernameAutofill.setOnClickListener {

                sendMessageText.setText("/$command ")
                sendMessageText.setSelection(sendMessageText.length())
                recyclerViewAutofill.visibility = View.GONE
            }
        }
    }

    inner class AutofillItemUser(private val user: ChatUser) : Item<GroupieViewHolder>() {
        override fun getLayout(): Int {
            return R.layout.autofill_item
        }

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.usernameAutofill.text = user.nick

            viewHolder.itemView.usernameAutofill.setOnClickListener {
                val currentWord = sendMessageText.text.toString().substringAfterLast(' ')
                val currentMessage = sendMessageText.text.toString().substringBefore(currentWord)
                sendMessageText.setText("${currentMessage}${user.nick} ")
                sendMessageText.setSelection(sendMessageText.length())
                recyclerViewAutofill.visibility = View.GONE
            }
        }
    }

    inner class AutofillItemEmote(private val emote: Emote) : Item<GroupieViewHolder>() {
        override fun getLayout(): Int {
            return R.layout.autofill_item
        }

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.usernameAutofill.text = emote.name
            viewHolder.itemView.usernameAutofill.setOnClickListener {
                val currentWord = sendMessageText.text.toString().substringAfterLast(' ')
                val currentMessage = sendMessageText.text.toString().substringBefore(currentWord)
                sendMessageText.setText("${currentMessage}${emote.name} ")
                sendMessageText.setSelection(sendMessageText.length())
                recyclerViewAutofill.visibility = View.GONE
            }
        }
    }

    fun savePrivateMessage(whisperMessageItem: WhisperMessageItem) {
        if (CurrentUser.privateMessages == null) {
            CurrentUser.privateMessages = mutableListOf()
        }
        if (CurrentUser.user == null) {
            return
        }
        CurrentUser.privateMessages!!.add(whisperMessageItem)
        if (CurrentUser.tempWhisperUser != null) {
            if (CurrentUser.tempWhisperUser == whisperMessageItem.getNick()) {
                if (supportFragmentManager.findFragmentById(R.id.whispers_user_fragment)!!.isVisible) {
                    supportFragmentManager.beginTransaction()
                        .hide(supportFragmentManager.findFragmentById(R.id.whispers_user_fragment)!!)
                        .commit()
                    supportFragmentManager.beginTransaction()
                        .show(supportFragmentManager.findFragmentById(R.id.whispers_user_fragment)!!)
                        .commit()
                }
            }
        }
        if (supportFragmentManager.findFragmentById(R.id.whispers_fragment)!!.isVisible) {
            supportFragmentManager.findFragmentById(R.id.whispers_fragment)!!
                .onHiddenChanged(false)
        }
        val file =
            baseContext.getFileStreamPath("${CurrentUser.user!!.username}_private_messages.txt")
        if (file.exists()) {
            try {
                val fileOutputStream = baseContext.openFileOutput(
                    "${CurrentUser.user!!.username}_private_messages.txt",
                    Context.MODE_APPEND
                )
                fileOutputStream.write("\n${Gson().toJson(whisperMessageItem)}".toByteArray())
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                val fileOutputStream = baseContext.openFileOutput(
                    "${CurrentUser.user!!.username}_private_messages.txt",
                    Context.MODE_PRIVATE
                )

                fileOutputStream.write(Gson().toJson(whisperMessageItem).toByteArray())
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    fun retrievePrivateMessages() {
        if (CurrentUser.privateMessages == null) {
            CurrentUser.privateMessages = mutableListOf()
        }
        if (CurrentUser.user == null) {
            return
        }
        val file =
            baseContext.getFileStreamPath("${CurrentUser.user!!.username}_private_messages.txt")
        if (file.exists()) {
            val fileInputStream =
                openFileInput("${CurrentUser.user!!.username}_private_messages.txt")
            val inputStreamReader = InputStreamReader(fileInputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            while (bufferedReader.ready()) {

                val line = bufferedReader.readLine()
                val curPMessage: WhisperMessageItem? =
                    Gson().fromJson(line, WhisperMessageItem::class.java)
                if (curPMessage != null) {
                    CurrentUser.privateMessages!!.add(
                        WhisperMessageItem(
                            curPMessage.message,
                            curPMessage.isReceived
                        )
                    )
                }
            }
        }
    }

    fun retrieveOptions() {
        val file = baseContext.getFileStreamPath("filename.txt")
        if (file.exists()) {
            val fileInputStream = openFileInput("filename.txt")
            val inputStreamReader = InputStreamReader(fileInputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            val stringBuilder = StringBuilder()
            var text: String? = null
            while ({ text = bufferedReader.readLine(); text }() != null) {
                stringBuilder.append(text)
            }
            CurrentUser.options = Klaxon().parse(stringBuilder.toString())
        } else {
            CurrentUser.options = Options()
        }
    }

    private fun displayNotification(message: Message) {
        val pendingIntent = TaskStackBuilder.create(this)
            .addNextIntent(Intent(this, ChatActivity::class.java))
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(message.nick)
            .setContentText(message.data)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val remoteInput = RemoteInput.Builder(NOTIFICATION_REPLY_KEY).setLabel("Reply").build()

        val replyIntent = Intent(this, ChatActivity::class.java)
            .putExtra(NOT_USER_KEY, message.nick)

        val replyPendingIntent =
            PendingIntent.getActivity(this, 0, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val action = NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_foreground,
            "Reply",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput).build()

        notificationBuilder.addAction(action)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel =
            NotificationChannel(channelId, "Chat Messages", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    inner class ErrorChatMessage(private val message: String) : Item<GroupieViewHolder>() {
        override fun getLayout(): Int {
            return R.layout.error_chat_message_item
        }

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            if (CurrentUser.options!!.showTime) {
                val dateFormat = SimpleDateFormat("HH:mm")
                val time = dateFormat.format(System.currentTimeMillis())
                viewHolder.itemView.timestampErrorChatMessage.visibility = View.VISIBLE
                viewHolder.itemView.timestampErrorChatMessage.text = time
            }
            viewHolder.itemView.messageErrorChatMessage.text = message
        }
    }

    inner class ChatMessageCombo(
        private val messageData: Message,
        private var count: Int = 2
    ) :
        Item<GroupieViewHolder>() {
        var state: Int = 0 // 0 hit animation // 1 ccccombo animation // 2 combo static
        private var comboCountInitialSize = -1f
        private var xInitialSize = -1f
        private var hitsInitialSize = -1f

        override fun getLayout(): Int {
            return R.layout.chat_message_item_emote_combo
        }

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            if (comboCountInitialSize == -1f) {
                comboCountInitialSize =
                    viewHolder.itemView.comboCountChatMessageCombo.textSize / resources.displayMetrics.scaledDensity
            }
            if (xInitialSize == -1f) {
                xInitialSize =
                    viewHolder.itemView.xChatMessageCombo.textSize / resources.displayMetrics.scaledDensity
            }
            if (hitsInitialSize == -1f) {
                hitsInitialSize =
                    viewHolder.itemView.hitsComboChatMessageCombo.textSize / resources.displayMetrics.scaledDensity
            }
            if (CurrentUser.options!!.showTime) {
                val dateFormat = SimpleDateFormat("HH:mm")
                val time = dateFormat.format(messageData.timestamp)
                viewHolder.itemView.timestampChatMessageCombo.visibility = View.VISIBLE
                viewHolder.itemView.timestampChatMessageCombo.text = time
            }

            if (CurrentUser.tempHighlightNick != null) {
                viewHolder.itemView.alpha = 0.5f
            }
            viewHolder.itemView.setOnClickListener {
                CurrentUser.tempHighlightNick = null
                for (i in 0 until adapter.itemCount) {
                    if (adapter.getItem(i).layout == R.layout.chat_message_item || adapter.getItem(i).layout == R.layout.chat_message_item_consecutive_nick) {
                        val adapterItem =
                            recyclerViewChat.findViewHolderForAdapterPosition(i)
                        adapterItem?.itemView?.alpha = 1f

                    } else if (adapter.getItem(i).layout == R.layout.private_chat_message_item) {
                        val adapterItem =
                            recyclerViewChat.findViewHolderForAdapterPosition(i)
                        adapterItem?.itemView?.alpha = 1f

                    } else {
                        val adapterItem =
                            recyclerViewChat.findViewHolderForAdapterPosition(i)
                        adapterItem?.itemView?.alpha = 1f
                    }
                    adapter.notifyItemChanged(i)
                }
            }

            viewHolder.itemView.comboCountChatMessageCombo.text = "$count"


            if (count >= 10) {
                viewHolder.itemView.hitsComboChatMessageCombo.setTypeface(
                    viewHolder.itemView.hitsComboChatMessageCombo.typeface,
                    Typeface.BOLD_ITALIC
                )
            }
            createMessageTextView(
                messageData,
                viewHolder.itemView.messageChatMessageCombo,
                emotes = true,
                greentext = false,
                links = false,
                codes = false,
                spoilers = false,
                me = false
            )
            when (state) {
                0 -> {
                    var scaleValue = 1.0
                    when {
                        count >= 50 -> {
                            scaleValue = 1.80
                        }
                        count >= 30 -> {
                            scaleValue = 1.60
                        }
                        count >= 20 -> {
                            scaleValue = 1.40
                        }
                        count >= 10 -> {
                            scaleValue = 1.20
                        }
                        count >= 5 -> {
                            scaleValue = 1.10
                        }
                    }
                    viewHolder.itemView.comboCountChatMessageCombo.textSize =
                        (comboCountInitialSize * scaleValue).toFloat()
                    viewHolder.itemView.xChatMessageCombo.textSize =
                        (xInitialSize * scaleValue).toFloat()
                    viewHolder.itemView.hitsComboChatMessageCombo.textSize =
                        (hitsInitialSize * scaleValue).toFloat()
                    if (count >= 10) {
                        viewHolder.itemView.redSplatChatMessageCombo.visibility = View.VISIBLE
                        viewHolder.itemView.graySplatChatMessageCombo.visibility = View.VISIBLE
                    }
                    fun TextView.hitsAnimation(
                    ) {
                        val bright = Color.parseColor("#FFF7F9")
                        val red = Color.parseColor("#B91010")

                        //sixth
                        val sixthScaleAnimation = ScaleAnimation(
                            2f, 1.0f, 2f, 1.0f, Animation.RELATIVE_TO_SELF,
                            0f,
                            Animation.RELATIVE_TO_SELF,
                            0.5f
                        )
                        sixthScaleAnimation.fillAfter = true
                        sixthScaleAnimation.duration = 570
                        val sixthColorAnimation =
                            ObjectAnimator.ofInt(
                                this,
                                "textColor",
                                red,
                                bright
                            )
                        sixthColorAnimation.duration = 570
                        sixthColorAnimation.setEvaluator(ArgbEvaluator())
                        sixthScaleAnimation.setAnimationListener(object :
                            Animation.AnimationListener {

                            override fun onAnimationStart(animation: Animation?) {
                                sixthColorAnimation.start()
                            }

                            override fun onAnimationRepeat(animation: Animation?) {
                            }

                            override fun onAnimationEnd(animation: Animation?) {
                            }
                        })
                        //fifth
                        val fifthScaleAnimation = ScaleAnimation(
                            1.9f, 2f, 1.9f, 2f, Animation.RELATIVE_TO_SELF,
                            0f,
                            Animation.RELATIVE_TO_SELF,
                            0.5f
                        )
                        fifthScaleAnimation.fillAfter = true
                        fifthScaleAnimation.duration = 6
                        val fifthColorAnimation =
                            ObjectAnimator.ofInt(
                                this,
                                "textColor",
                                bright,
                                red
                            )
                        fifthColorAnimation.duration = 6
                        fifthColorAnimation.setEvaluator(ArgbEvaluator())
                        fifthScaleAnimation.setAnimationListener(object :
                            Animation.AnimationListener {

                            override fun onAnimationStart(animation: Animation?) {
                                fifthColorAnimation.start()
                            }

                            override fun onAnimationRepeat(animation: Animation?) {
                            }

                            override fun onAnimationEnd(animation: Animation?) {
                                this@hitsAnimation.startAnimation(sixthScaleAnimation)
                            }
                        })
                        //fourth
                        val fourthScaleAnimation = ScaleAnimation(
                            2f, 1.9f, 2f, 1.9f, Animation.RELATIVE_TO_SELF,
                            0f,
                            Animation.RELATIVE_TO_SELF,
                            0.5f
                        )
                        fourthScaleAnimation.fillAfter = true
                        fourthScaleAnimation.duration = 6
                        val fourthColorAnimation =
                            ObjectAnimator.ofInt(
                                this,
                                "textColor",
                                red,
                                bright
                            )
                        fourthColorAnimation.duration = 6
                        fourthColorAnimation.setEvaluator(ArgbEvaluator())
                        fourthScaleAnimation.setAnimationListener(object :
                            Animation.AnimationListener {

                            override fun onAnimationStart(animation: Animation?) {
                                fourthColorAnimation.start()
                            }

                            override fun onAnimationRepeat(animation: Animation?) {
                            }

                            override fun onAnimationEnd(animation: Animation?) {
                                this@hitsAnimation.startAnimation(fifthScaleAnimation)
                            }
                        })
                        //third
                        val thirdScaleAnimation = ScaleAnimation(
                            1.9f, 2f, 1.9f, 2f, Animation.RELATIVE_TO_SELF,
                            0f,
                            Animation.RELATIVE_TO_SELF,
                            0.5f
                        )
                        thirdScaleAnimation.fillAfter = true
                        thirdScaleAnimation.duration = 6
                        val thirdColorAnimation =
                            ObjectAnimator.ofInt(
                                this,
                                "textColor",
                                bright,
                                red
                            )
                        thirdColorAnimation.duration = 6
                        thirdColorAnimation.setEvaluator(ArgbEvaluator())
                        thirdScaleAnimation.setAnimationListener(object :
                            Animation.AnimationListener {

                            override fun onAnimationStart(animation: Animation?) {
                                thirdColorAnimation.start()
                            }

                            override fun onAnimationRepeat(animation: Animation?) {
                            }

                            override fun onAnimationEnd(animation: Animation?) {
                                this@hitsAnimation.startAnimation(fourthScaleAnimation)
                            }
                        })
                        //second
                        val secondScaleAnimation = ScaleAnimation(
                            2f, 1.9f, 2f, 1.9f, Animation.RELATIVE_TO_SELF,
                            0f,
                            Animation.RELATIVE_TO_SELF,
                            0.5f
                        )
                        secondScaleAnimation.fillAfter = true
                        secondScaleAnimation.duration = 6
                        val secondColorAnimation =
                            ObjectAnimator.ofInt(
                                this,
                                "textColor",
                                red,
                                bright
                            )
                        secondColorAnimation.duration = 6
                        secondColorAnimation.setEvaluator(ArgbEvaluator())
                        secondScaleAnimation.setAnimationListener(object :
                            Animation.AnimationListener {

                            override fun onAnimationStart(animation: Animation?) {
                                secondColorAnimation.start()
                            }

                            override fun onAnimationRepeat(animation: Animation?) {
                            }

                            override fun onAnimationEnd(animation: Animation?) {
                                this@hitsAnimation.startAnimation(thirdScaleAnimation)
                            }
                        })

                        //first
                        val firstScaleAnimation = ScaleAnimation(
                            1f,
                            2f,
                            1f,
                            2f,
                            Animation.RELATIVE_TO_SELF,
                            0f,
                            Animation.RELATIVE_TO_SELF,
                            0.5f
                        )
                        firstScaleAnimation.fillAfter = true
                        firstScaleAnimation.duration = 6
                        val firstColorAnimation =
                            ObjectAnimator.ofInt(
                                this,
                                "textColor",
                                bright,
                                red
                            )
                        firstColorAnimation.duration = 6
                        firstColorAnimation.setEvaluator(ArgbEvaluator())
                        firstScaleAnimation.setAnimationListener(object :
                            Animation.AnimationListener {

                            override fun onAnimationStart(animation: Animation?) {
                                firstColorAnimation.start()
                            }

                            override fun onAnimationRepeat(animation: Animation?) {
                            }

                            override fun onAnimationEnd(animation: Animation?) {
                                this@hitsAnimation.startAnimation(secondScaleAnimation)
                            }
                        })
                        this.startAnimation(firstScaleAnimation)

                    }
                    viewHolder.itemView.hitsComboChatMessageCombo.text = "HITS"
                    viewHolder.itemView.hitsComboChatMessageCombo.hitsAnimation()
                }
                1 -> {

                    val gray = Color.parseColor("#999999")
                    val bright = Color.parseColor("#FFF7F9")

                    fun splatFade(red: ImageView, gray: ImageView) {
                        val grayAnim = AlphaAnimation(0.0f, 1.0f)
                        grayAnim.duration = 750

                        val redAnim = AlphaAnimation(1.0f, 0.0f)
                        redAnim.duration = 500
                        redAnim.setAnimationListener(object : Animation.AnimationListener {
                            override fun onAnimationEnd(animation: Animation?) {
                                red.visibility = View.GONE
                            }

                            override fun onAnimationStart(animation: Animation?) {
                            }

                            override fun onAnimationRepeat(animation: Animation?) {
                            }
                        })
                        val redStaticAnim = AlphaAnimation(1.0f, 1.0f)
                        redStaticAnim.duration = 500
                        redStaticAnim.setAnimationListener(object : Animation.AnimationListener {
                            override fun onAnimationEnd(animation: Animation?) {

                                red.startAnimation(redAnim)
                                gray.startAnimation(grayAnim)
                            }

                            override fun onAnimationStart(animation: Animation?) {
                            }

                            override fun onAnimationRepeat(animation: Animation?) {
                            }
                        })
                        red.startAnimation(redStaticAnim)
                    }
                    viewHolder.itemView.hitsComboChatMessageCombo.textSize =
                        (hitsInitialSize * 1.25).toFloat()
                    viewHolder.itemView.xChatMessageCombo.textSize =
                        (xInitialSize * 1.25).toFloat()
                    fun TextView.comboAnimation() {
                        val comboColorAnimation =
                            ObjectAnimator.ofInt(
                                this,
                                "textColor",
                                bright,
                                gray
                            )
                        comboColorAnimation.duration = 500
                        comboColorAnimation.setEvaluator(ArgbEvaluator())
                        val anim = AlphaAnimation(1.0f, 0.0f)
                        anim.duration = 500
                        anim.repeatCount = 1
                        anim.repeatMode = Animation.REVERSE

                        val xDelta =
                            2f * (resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)
                        val slideLeft = TranslateAnimation(xDelta, 0f, 0f, 0f)
                        slideLeft.duration = 500
                        slideLeft.fillAfter = true
                        anim.setAnimationListener(object : Animation.AnimationListener {
                            override fun onAnimationEnd(animation: Animation?) {}
                            override fun onAnimationStart(animation: Animation?) {
                                this@comboAnimation.text = "C-C-C-COMBO"
                            }

                            override fun onAnimationRepeat(animation: Animation?) {
                                comboColorAnimation.start()
                                this@comboAnimation.startAnimation(slideLeft)


                            }
                        })
                        this.startAnimation(anim)
                    }
                    viewHolder.itemView.hitsComboChatMessageCombo.comboAnimation()
                    if (count >= 10) {
                        splatFade(
                            viewHolder.itemView.redSplatChatMessageCombo,
                            viewHolder.itemView.graySplatChatMessageCombo
                        )
                    }
                    state = 2
                }
                2 -> {
                    //static
                }
            }
            // 600 ms total
            // 0-1 % change to 200% text size : colour #B91010 // 6ms
            // 1-2 % change to 190% text size : colour #FFF7F9 // 6ms
            // 2-3 % change to 200% text size : colour #B91010 // 6ms
            // 3-4 % change to 190% text size : colour #FFF7F9 // 6ms
            // 4-5 % change to 200% text size : colour #B91010 // 6ms
            //5-100% change to 120% text size : colour #FFF7F9 // 570ms
        }

        fun isCombo(): Boolean {
            if (messageData.entities.emotes != null) {
                if (messageData.entities.emotes!![0].combo > 0) {
                    return true
                }
            }
            return false
        }

        fun setCombo(comboCount: Int) {
            count = comboCount
        }

    }

    inner class ChatMessage(
        private val messageData: Message,
        private val isConsecutive: Boolean = false
    ) :
        Item<GroupieViewHolder>() {
        override fun getLayout(): Int {
            if (isConsecutive) {
                return R.layout.chat_message_item_consecutive_nick
            }

            return R.layout.chat_message_item
        }

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            if (CurrentUser.options!!.showTime) {
                val dateFormat = SimpleDateFormat("HH:mm")
                val time = dateFormat.format(messageData.timestamp)
                viewHolder.itemView.timestampChatMessage.visibility = View.VISIBLE
                viewHolder.itemView.timestampChatMessage.text = time
            }

            if (CurrentUser.user != null) {
                if (messageData.data.contains(CurrentUser.user!!.username)) {
                    viewHolder.itemView.setBackgroundColor(Color.parseColor("#001D36"))
                } else if (CurrentUser.user!!.username == messageData.nick) {
                    viewHolder.itemView.setBackgroundColor(Color.parseColor("#151515"))
                } else if (CurrentUser.user!!.username != messageData.nick && !messageData.data.contains(
                        CurrentUser.user!!.username
                    )
                ) {
                    viewHolder.itemView.setBackgroundColor(Color.parseColor("#000000"))
                }
            } else if (CurrentUser.user == null) {
                if (messageData.data.contains("anonymous")) {
                    viewHolder.itemView.setBackgroundColor(Color.parseColor("#001D36"))
                } else {
                    viewHolder.itemView.setBackgroundColor(Color.parseColor("#000000"))
                }
            }

            if (CurrentUser.options!!.customHighlights.isNotEmpty()) {
                CurrentUser.options!!.customHighlights.forEach {
                    if (messageData.nick == it) {
                        viewHolder.itemView.setBackgroundColor(Color.parseColor("#001D36"))
                    }
                }
            }

            if (messageData.features.contains("bot") || messageData.nick == "Info") {
                viewHolder.itemView.usernameChatMessage.setTextColor(Color.parseColor("#FF2196F3"))
                viewHolder.itemView.botFlairChatMessage.visibility = View.VISIBLE
            } else {
                viewHolder.itemView.usernameChatMessage.setTextColor(Color.parseColor("#FFFFFF"))
                viewHolder.itemView.botFlairChatMessage.visibility = View.GONE
            }

            if (CurrentUser.tempHighlightNick != null) {
                when {
                    CurrentUser.tempHighlightNick!!.contains(messageData.nick) -> {
                        viewHolder.itemView.alpha = 1f
                    }
                    CurrentUser.tempHighlightNick!!.isEmpty() -> {
                        viewHolder.itemView.alpha = 1f
                    }
                    else -> {
                        viewHolder.itemView.alpha = 0.5f
                    }
                }
            } else {
                viewHolder.itemView.alpha = 1f
            }

            viewHolder.itemView.usernameChatMessage.text = "${messageData.nick}:"

            viewHolder.itemView.messageChatMessage.movementMethod = LinkMovementMethod.getInstance()

            createMessageTextView(messageData, viewHolder.itemView.messageChatMessage)


            viewHolder.itemView.usernameChatMessage.setOnClickListener {
                for (i in 0 until adapter.itemCount) {
                    if (adapter.getItem(i).layout == R.layout.chat_message_item || adapter.getItem(i).layout == R.layout.chat_message_item_consecutive_nick) {
                        val item = adapter.getItem(i) as ChatMessage
                        if (item.isNickSame(messageData.nick)) {
                            val adapterItem =
                                recyclerViewChat.findViewHolderForAdapterPosition(i)
                            adapterItem?.itemView?.alpha = 1f
                        } else {
                            val adapterItem =
                                recyclerViewChat.findViewHolderForAdapterPosition(i)
                            if (CurrentUser.tempHighlightNick != null && CurrentUser.tempHighlightNick!!.contains(
                                    item.getNick()
                                )
                            ) {
                                adapterItem?.itemView?.alpha = 1f
                            } else {
                                adapterItem?.itemView?.alpha = 0.5f
                            }

                        }


                    } else if (adapter.getItem(i).layout == R.layout.private_chat_message_item) {
                        val item = adapter.getItem(i) as PrivateChatMessage
                        if (item.isNickSame(messageData.nick)) {
                            val adapterItem =
                                recyclerViewChat.findViewHolderForAdapterPosition(i)
                            adapterItem?.itemView?.alpha = 1f
                        } else {
                            val adapterItem =
                                recyclerViewChat.findViewHolderForAdapterPosition(i)
                            if (CurrentUser.tempHighlightNick != null && CurrentUser.tempHighlightNick!!.contains(
                                    item.getNick()
                                )
                            ) {
                                adapterItem?.itemView?.alpha = 1f
                            } else {
                                adapterItem?.itemView?.alpha = 0.5f
                            }
                        }
                    } else {
                        val adapterItem =
                            recyclerViewChat.findViewHolderForAdapterPosition(i)
                        adapterItem?.itemView?.alpha = 0.5f
                    }
                    adapter.notifyItemChanged(i)
                }
                if (CurrentUser.tempHighlightNick == null) {
                    CurrentUser.tempHighlightNick = mutableListOf()
                }
                CurrentUser.tempHighlightNick!!.add(messageData.nick)
            }

            viewHolder.itemView.usernameChatMessage.setOnLongClickListener {
                val wrapper = ContextThemeWrapper(this@ChatActivity, R.style.PopupMenu)
                val pop = PopupMenu(wrapper, it)
                pop.inflate(R.menu.chat_message_username_menu)
                pop.setOnMenuItemClickListener { itMenuItem ->
                    when (itMenuItem.itemId) {
                        R.id.chatWhisper -> {
                            sendMessageText.setText("/w ${messageData.nick} ")
                            keyRequestFocus(sendMessageText, this@ChatActivity)
                            sendMessageText.setSelection(sendMessageText.text.length)
                        }
                        R.id.chatMention -> {
                            val currentMessage = sendMessageText.text.toString()
                            if (currentMessage.isNotEmpty()) {
                                if (currentMessage.last() == ' ') {
                                    sendMessageText.setText(currentMessage.plus("${messageData.nick} "))
                                } else {
                                    sendMessageText.setText(currentMessage.plus(" ${messageData.nick} "))
                                }
                            } else {
                                sendMessageText.setText("${messageData.nick} ")
                            }
                            keyRequestFocus(sendMessageText, this@ChatActivity)
                            sendMessageText.setSelection(sendMessageText.text.length)
                        }
                        R.id.chatIgnore -> {
                            CurrentUser.options!!.ignoreList.add(messageData.nick)
                            CurrentUser.saveOptions(this@ChatActivity)
                            adapter.notifyDataSetChanged()
                        }
                    }
                    true
                }
                pop.show()
                true
            }

            viewHolder.itemView.setOnClickListener {
                CurrentUser.tempHighlightNick = null
                for (i in 0 until adapter.itemCount) {
                    if (adapter.getItem(i).layout == R.layout.chat_message_item || adapter.getItem(i).layout == R.layout.chat_message_item_consecutive_nick) {
                        val item = adapter.getItem(i) as ChatMessage
                        val adapterItem =
                            recyclerViewChat.findViewHolderForAdapterPosition(i)
                        adapterItem?.itemView?.alpha = 1f

                    } else if (adapter.getItem(i).layout == R.layout.private_chat_message_item) {
                        val item = adapter.getItem(i) as PrivateChatMessage
                        val adapterItem =
                            recyclerViewChat.findViewHolderForAdapterPosition(i)
                        adapterItem?.itemView?.alpha = 1f

                    } else {
                        val adapterItem =
                            recyclerViewChat.findViewHolderForAdapterPosition(i)
                        adapterItem?.itemView?.alpha = 1f
                    }
                    adapter.notifyItemChanged(i)
                }
            }
            if (isConsecutive) {
                viewHolder.itemView.usernameChatMessage.visibility = View.GONE
                viewHolder.itemView.botFlairChatMessage.visibility = View.GONE
            }
        }

        fun isCombo(): Boolean {
            if (messageData.entities.emotes != null) {
                if (messageData.entities.emotes!![0].combo > 0) {
                    return true
                }
            }
            return false
        }

        fun isNickSame(nick: String): Boolean {
            return messageData.nick == nick
        }


        fun isFeaturesEmpty(): Boolean {
            return messageData.features.isEmpty()
        }

        fun getNick(): String {
            return messageData.nick
        }
    }

    inner class PrivateChatMessage(
        private val messageData: Message,
        private val isReceived: Boolean = false
    ) :
        Item<GroupieViewHolder>() {
        override fun getLayout(): Int {
            return R.layout.private_chat_message_item
        }

        init {
            savePrivateMessage(WhisperMessageItem(messageData, isReceived))
        }

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            if (CurrentUser.options!!.ignoreList.contains(messageData.nick)) {
                return
            }
            if (isReceived) {
                viewHolder.itemView.whisperedPrivateMessage.visibility = View.VISIBLE

            } else {
                viewHolder.itemView.toPrivateMessage.visibility = View.VISIBLE
                viewHolder.itemView.whisperedPrivateMessage.text = ":"
                viewHolder.itemView.whisperedPrivateMessage.visibility = View.VISIBLE
            }
            if (CurrentUser.options!!.showTime) {
                val dateFormat = SimpleDateFormat("HH:mm")
                val time = dateFormat.format(messageData.timestamp)
                viewHolder.itemView.timestampPrivateMessage.visibility = View.VISIBLE
                viewHolder.itemView.timestampPrivateMessage.text = time
            }

            if (CurrentUser.user != null) {
                if (messageData.data.contains(CurrentUser.user!!.username)) {
                    viewHolder.itemView.setBackgroundColor(Color.parseColor("#001D36"))
                } else {
                    viewHolder.itemView.setBackgroundColor(Color.parseColor("#151515"))
                }
            } else if (CurrentUser.user == null) {
                if (messageData.data.contains("anonymous")) {
                    viewHolder.itemView.setBackgroundColor(Color.parseColor("#001D36"))
                } else {
                    viewHolder.itemView.setBackgroundColor(Color.parseColor("#000000"))
                }
            }

            if (CurrentUser.options!!.customHighlights.isNotEmpty()) {
                CurrentUser.options!!.customHighlights.forEach {
                    if (messageData.nick == it) {
                        viewHolder.itemView.setBackgroundColor(Color.parseColor("#001D36"))
                    }
                }
            }

            if (CurrentUser.tempHighlightNick != null) {
                when {
                    CurrentUser.tempHighlightNick!!.contains(messageData.nick) -> {
                        viewHolder.itemView.alpha = 1f
                    }
                    CurrentUser.tempHighlightNick!!.isEmpty() -> {
                        viewHolder.itemView.alpha = 1f
                    }
                    else -> {
                        viewHolder.itemView.alpha = 0.5f
                    }
                }
            } else {
                viewHolder.itemView.alpha = 1f
            }

            viewHolder.itemView.usernamePrivateMessage.text = messageData.nick

            viewHolder.itemView.messagePrivateMessage.movementMethod =
                LinkMovementMethod.getInstance()

            createMessageTextView(
                messageData,
                viewHolder.itemView.messagePrivateMessage
            )

            viewHolder.itemView.usernamePrivateMessage.setOnClickListener {
                for (i in 0 until adapter.itemCount) {
                    if (adapter.getItem(i).layout == R.layout.chat_message_item || adapter.getItem(i).layout == R.layout.chat_message_item_consecutive_nick) {
                        val item = adapter.getItem(i) as ChatMessage
                        if (item.isNickSame(messageData.nick)) {
                            val adapterItem =
                                recyclerViewChat.findViewHolderForAdapterPosition(i)
                            adapterItem?.itemView?.alpha = 1f

                        } else {

                            val adapterItem =
                                recyclerViewChat.findViewHolderForAdapterPosition(i)

                            if (CurrentUser.tempHighlightNick != null && CurrentUser.tempHighlightNick!!.contains(
                                    item.getNick()
                                )
                            ) {
                                adapterItem?.itemView?.alpha = 1f
                            } else {
                                adapterItem?.itemView?.alpha = 0.5f
                            }
                        }
                    } else if (adapter.getItem(i).layout == R.layout.private_chat_message_item) {
                        val item = adapter.getItem(i) as PrivateChatMessage
                        if (item.messageData.nick == messageData.nick) {
                            val adapterItem =
                                recyclerViewChat.findViewHolderForAdapterPosition(i)
                            adapterItem?.itemView?.alpha = 1f
                        } else {
                            val adapterItem =
                                recyclerViewChat.findViewHolderForAdapterPosition(i)
                            if (CurrentUser.tempHighlightNick != null && CurrentUser.tempHighlightNick!!.contains(
                                    item.getNick()
                                )
                            ) {
                                adapterItem?.itemView?.alpha = 1f
                            } else {
                                adapterItem?.itemView?.alpha = 0.5f
                            }
                        }
                    } else {
                        val adapterItem =
                            recyclerViewChat.findViewHolderForAdapterPosition(i)
                        adapterItem?.itemView?.alpha = 0.5f
                    }
                    adapter.notifyItemChanged(i)
                }
                if (CurrentUser.tempHighlightNick == null) {
                    CurrentUser.tempHighlightNick = mutableListOf()
                }
                CurrentUser.tempHighlightNick!!.add(messageData.nick)
            }

            viewHolder.itemView.usernamePrivateMessage.setOnLongClickListener {
                val wrapper = ContextThemeWrapper(this@ChatActivity, R.style.PopupMenu)
                val pop = PopupMenu(wrapper, it)
                pop.inflate(R.menu.chat_message_username_menu)
                pop.setOnMenuItemClickListener { itMenuItem ->
                    when (itMenuItem.itemId) {
                        R.id.chatWhisper -> {
                            sendMessageText.setText("/w ${messageData.nick} ")
                            keyRequestFocus(sendMessageText, this@ChatActivity)
                            sendMessageText.setSelection(sendMessageText.text.length)
                        }
                        R.id.chatMention -> {
                            val currentMessage = sendMessageText.text.toString()
                            if (currentMessage.isNotEmpty()) {
                                if (currentMessage.last() == ' ') {
                                    sendMessageText.setText(currentMessage.plus("${messageData.nick} "))
                                } else {
                                    sendMessageText.setText(currentMessage.plus(" ${messageData.nick} "))
                                }
                            } else {
                                sendMessageText.setText("${messageData.nick} ")
                            }
                            keyRequestFocus(sendMessageText, this@ChatActivity)
                            sendMessageText.setSelection(sendMessageText.text.length)
                        }
                        R.id.chatIgnore -> {
                            CurrentUser.options!!.ignoreList.add(messageData.nick)
                            CurrentUser.saveOptions(this@ChatActivity)
                            adapter.notifyDataSetChanged()
                        }
                    }
                    true
                }
                pop.show()
                true
            }

            viewHolder.itemView.setOnClickListener {
                CurrentUser.tempHighlightNick = null
                for (i in 0 until adapter.itemCount) {
                    if (adapter.getItem(i).layout == R.layout.chat_message_item || adapter.getItem(i).layout == R.layout.chat_message_item_consecutive_nick) {
                        val item = adapter.getItem(i) as ChatMessage
                        val adapterItem =
                            recyclerViewChat.findViewHolderForAdapterPosition(i)
                        adapterItem?.itemView?.alpha = 1f

                    } else if (adapter.getItem(i).layout == R.layout.private_chat_message_item) {
                        val item = adapter.getItem(i) as PrivateChatMessage
                        val adapterItem =
                            recyclerViewChat.findViewHolderForAdapterPosition(i)
                        adapterItem?.itemView?.alpha = 1f

                    } else {
                        val item = adapter.getItem(i) as ErrorChatMessage
                        val adapterItem =
                            recyclerViewChat.findViewHolderForAdapterPosition(i)
                        adapterItem?.itemView?.alpha = 1f
                    }
                    adapter.notifyItemChanged(i)
                }
            }
        }

        fun isNickSame(nick: String): Boolean {
            return nick == messageData.nick
        }

        fun isFeaturesEmpty(): Boolean {
            return messageData.features.isEmpty()
        }

        fun getNick(): String {
            return messageData.nick
        }
    }

    inner class WhisperMessageItem(val message: Message, val isReceived: Boolean) :
        Item<GroupieViewHolder>() {
        override fun getLayout(): Int {
            if (isReceived) {
                return R.layout.whisper_message_item_left
            }
            return R.layout.whisper_message_item_right
        }

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {

            viewHolder.itemView.messageWhisperMessageItem.movementMethod =
                LinkMovementMethod.getInstance()
            createMessageTextView(message, viewHolder.itemView.messageWhisperMessageItem)

        }

        fun getNick(): String {
            return message.nick
        }
    }


    inner class ChatClient {

        private var jwt: String? = null

        private val client = HttpClient {
            install(WebSockets)
        }

        private fun retrieveHistory() {
            val messageHistory =
                Klaxon().parseArray<String>(URL("https://chat.strims.gg/api/chat/history").readText())
            runOnUiThread {
                messageHistory?.forEach {
                    val msg = parseMessage(it)
                    if (msg != null) {
                        var consecutiveMessage = false
                        if (adapter.itemCount > 0) {
                            if (adapter.getItem(adapter.itemCount - 1).layout == R.layout.chat_message_item || adapter.getItem(
                                    adapter.itemCount - 1
                                ).layout == R.layout.chat_message_item_consecutive_nick
                            ) {
                                val lastMessage =
                                    adapter.getItem(adapter.itemCount - 1) as ChatMessage
                                consecutiveMessage =
                                    lastMessage.isNickSame(msg.nick)
                            }

                        }
                        if (msg.entities.emotes != null && msg.entities.emotes!!.isNotEmpty() && msg.entities.emotes!![0].combo > 1) {
                            if (msg.entities.emotes!![0].combo == 2) {
                                adapter.removeGroupAtAdapterPosition(adapter.itemCount - 1)
                                adapter.add(ChatMessageCombo(msg))
                            } else {
                                if (adapter.getItem(adapter.itemCount - 1).layout == R.layout.chat_message_item_emote_combo) {
                                    val lastMessageCombo =
                                        adapter.getItem(adapter.itemCount - 1) as ChatMessageCombo
                                    lastMessageCombo.setCombo(msg.entities.emotes!![0].combo)
                                    adapter.notifyItemChanged(adapter.itemCount - 1)
                                }
                            }

                        } else {
                            if (adapter.itemCount > 0 && adapter.getItem(adapter.itemCount - 1).layout == R.layout.chat_message_item_emote_combo) {
                                val lastMessage =
                                    adapter.getItem(adapter.itemCount - 1) as ChatMessageCombo
                                lastMessage.state = 1
                                adapter.notifyItemChanged(adapter.itemCount - 1)
                            }
                            adapter.add(
                                ChatMessage(msg, consecutiveMessage)
                            )
                        }
                    }
                }
            }
        }

        private suspend fun retrieveEmotes() {
            val text: String = client.get("https://chat.strims.gg/emote-manifest.json")
            val emotesParsed: EmotesParsed = Klaxon().parse(text)!!
            CurrentUser.emotes = emotesParsed.emotes.toMutableList()
        }

        private fun retrieveCookie() {
            val cookieManager = CookieManager.getInstance()
            val cookies = cookieManager.getCookie("https://strims.gg")
            cookieManager.flush()
            if (cookies != null) {
                val jwt = cookies.substringAfter("jwt=").substringBefore(" ")
                if (jwt != cookies) {
                    this.jwt = jwt
                    CurrentUser.jwt = jwt
                }
            }
        }

        private fun getBitmapFromURL(src: String?): Bitmap? {
            return try {
                val url = URL(src)
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input: InputStream = connection.inputStream
                BitmapFactory.decodeStream(input)
            } catch (e: IOException) {
                null
            }
        }

        private fun getGifFromURL(src: String?): GifDrawable? {
            return try {
                val url = URL(src)
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input: InputStream = connection.inputStream
                val bis = BufferedInputStream(input)
                GifDrawable(bis)
//                GifDrawable.createFromStream(input, src)
            } catch (e: IOException) {
                null
            }
        }

        private fun cacheEmotes() {
            runOnUiThread {
                CurrentUser.emotes?.forEach {
                    val size = it.versions.size - 1
                    val biggestEmote = it.versions[size]
                    val url = "https://chat.strims.gg/${biggestEmote.path}"
                    if (!biggestEmote.animated) {
                        GlobalScope.launch {
                            val bitmap = getBitmapFromURL(url)
                            bitmapMemoryCache.put(it.name, bitmap)
                        }
                    } else {
                        GlobalScope.launch {
                            val gif = getGifFromURL(url)
                            gifMemoryCache.put(it.name, gif)
                        }
                    }
                }
            }
        }

        private suspend fun retrieveProfile() {
            val text: String = client.get("https://strims.gg/api/profile") {
                header("Cookie", "jwt=$jwt")
            }
            GlobalScope.launch {
                runOnUiThread {
                    CurrentUser.user = Klaxon().parse(text)
                    sendMessageText.hint = "Write something ${CurrentUser.user!!.username} ..."
                    navHeaderUsername.text = CurrentUser.user!!.username
                    invalidateOptionsMenu()
                }
            }
        }

        suspend fun onConnect() = client.wss(
            host = "chat.strims.gg",
            path = "/ws",
            request = {
                retrieveCookie()
                if (jwt != null) {
                    Log.d("TAG", "Requesting with JWT: $jwt")
                    header("Cookie", "jwt=$jwt")
                }
            }
        ) {
            if (jwt != null) {
                retrieveProfile()
            }
            retrieveEmotes()
            cacheEmotes()
            retrieveOptions()
            retrieveHistory()
            retrievePrivateMessages()
            sendMessageButton.setOnClickListener {
                GlobalScope.launch {
                    val messageText = sendMessageText.text.toString()
                    if (messageText.isEmpty()) {
                        return@launch
                    }
                    val first = messageText.first()
                    if (supportFragmentManager.findFragmentById(R.id.whispers_user_fragment)!!.isVisible) {
                        when {
                            messageText.trim() == "" -> {
                                //TODO: empty message notify in chat ?
                                return@launch
                            }
                            CurrentUser.tempWhisperUser == null -> {
                                // TODO: error
                            }
                            else -> {
                                val nick = CurrentUser.tempWhisperUser!!
                                send("PRIVMSG {\"nick\":\"$nick\", \"data\":\"$messageText\"}")
                                runOnUiThread {
                                    adapter.add(
                                        PrivateChatMessage(
                                            Message(
                                                true,
                                                nick,
                                                messageText
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    } else if (first == '/' && messageText.substringBefore(' ') != "/me") {
                        var privateMessageCommand = ""
                        for (privateMessageItem in privateMessageArray) {
                            if (privateMessageItem.contains(
                                    messageText.substringAfter(first).substringBefore(' '), true
                                )
                            ) {
                                privateMessageCommand =
                                    messageText.substringAfter(first).substringBefore(' ')
                                break
                            }

                        }
                        if (privateMessageCommand != "") {
                            val command = privateMessageCommand
                            if (messageText.length <= privateMessageCommand.length + 2) { // 1 for '/'  1 for space
                                runOnUiThread {
                                    adapter.add(ErrorChatMessage("Invalid nick - /$privateMessageCommand nick message"))
                                }
                            } else {
                                val nick =
                                    messageText.substringAfter("$command ").substringBefore(' ')
                                val nickRegex = "^[A-Za-z0-9_]{3,20}$"
                                val p: Pattern = Pattern.compile(nickRegex)
                                val m: Matcher = p.matcher(nick)

                                if (!m.find()) {
                                    runOnUiThread {
                                        adapter.add(ErrorChatMessage("Invalid nick - /$privateMessageCommand nick message"))
                                    }
                                } else {
                                    var message = messageText.substringAfter("$command $nick")
                                    message = message.substringAfter(" ")
                                    if (message.trim() == "") {
                                        //TODO: empty message notify in chat ?
                                        return@launch
                                    } else {
                                        send("PRIVMSG {\"nick\":\"$nick\", \"data\":\"$message\"}")
                                        runOnUiThread {
                                            adapter.add(
                                                PrivateChatMessage(
                                                    Message(
                                                        true,
                                                        nick,
                                                        message
                                                    )
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (messageText.substringAfter(first)
                                .substringBefore(' ') == "ignore"
                        ) {
                            val nickIgnore =
                                messageText.substringAfter("/ignore ").substringBefore(' ')
                            CurrentUser.options!!.ignoreList.add(nickIgnore)
                            CurrentUser.saveOptions(this@ChatActivity)
                            runOnUiThread {
                                adapter.add(
                                    ChatMessage(
                                        Message(
                                            false,
                                            "Info",
                                            "Ignoring: $nickIgnore"
                                        )
                                    )
                                )
                            }
                        } else if (messageText.substringAfter(first)
                                .substringBefore(' ') == "unignore"
                        ) {
                            val nickUnignore =
                                messageText.substringAfter("/unignore ").substringBefore(' ')
                            if (CurrentUser.options!!.ignoreList.contains(nickUnignore)) {
                                CurrentUser.options!!.ignoreList.remove(nickUnignore)
                                CurrentUser.saveOptions(this@ChatActivity)
                                runOnUiThread {
                                    adapter.add(
                                        ChatMessage(
                                            Message(
                                                false,
                                                "Info",
                                                "Unignored: $nickUnignore"
                                            )
                                        )
                                    )
                                }
                            } else {
                                runOnUiThread {
                                    adapter.add(
                                        ChatMessage(
                                            Message(
                                                false,
                                                "Info",
                                                "User not currently ignored"
                                            )
                                        )
                                    )
                                }
                            }
                        } else if (messageText.substringAfter(first)
                                .substringBefore(' ') == "highlight"
                        ) {
                            val nickHighlight =
                                messageText.substringAfter("/highlight ").substringBefore(' ')
                            if (CurrentUser.options!!.customHighlights.contains(nickHighlight)) {
                                runOnUiThread {
                                    adapter.add(
                                        ChatMessage(
                                            Message(
                                                false,
                                                "Info",
                                                "User already highlighted"
                                            )
                                        )
                                    )
                                }
                            } else {
                                CurrentUser.options!!.customHighlights.add(nickHighlight)
                                CurrentUser.saveOptions(this@ChatActivity)
                                runOnUiThread {
                                    adapter.add(
                                        ChatMessage(
                                            Message(
                                                false,
                                                "Info",
                                                "Highlighting user: $nickHighlight"
                                            )
                                        )
                                    )
                                }
                            }
                        } else if (messageText.substringAfter(first)
                                .substringBefore(' ') == "unhighlight"
                        ) {
                            val nickUnhighlight =
                                messageText.substringAfter("/unhighlight ").substringBefore(' ')
                            if (CurrentUser.options!!.customHighlights.contains(nickUnhighlight)) {
                                CurrentUser.options!!.customHighlights.remove(nickUnhighlight)
                                CurrentUser.saveOptions(this@ChatActivity)
                                runOnUiThread {
                                    adapter.add(
                                        ChatMessage(
                                            Message(
                                                false,
                                                "Info",
                                                "No longer highlighting user: $nickUnhighlight"
                                            )
                                        )
                                    )
                                }
                            } else {
                                runOnUiThread {
                                    adapter.add(
                                        ChatMessage(
                                            Message(
                                                false,
                                                "Info",
                                                "User not currently highlighted"
                                            )
                                        )
                                    )
                                }
                            }
                        } else {
                            runOnUiThread {
                                adapter.add(
                                    ChatMessage(
                                        Message(
                                            false,
                                            "Info",
                                            "Invalid command"
                                        )
                                    )
                                )
                            }
                        }
                    } else {
                        send("MSG {\"data\":\"${sendMessageText.text}\"}")
                    }
                    runOnUiThread {
                        sendMessageText.text.clear()
                        recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
                    }
                }
            }
            if (intent.getStringExtra(NOT_USER_KEY) != null) {
                val remoteReply = RemoteInput.getResultsFromIntent(intent)

                if (remoteReply != null) {
                    val message = remoteReply.getCharSequence(NOTIFICATION_REPLY_KEY) as String
                    val nick = intent.getStringExtra(NOT_USER_KEY)
                    send("PRIVMSG {\"nick\":\"$nick\", \"data\":\"$message\"}")

                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.cancel(NOTIFICATION_ID)
                }
            }
            while (true) {
                when (val frame = incoming.receive()) {
                    is Frame.Text -> {
                        println(frame.readText())
                        val msg: Message? = parseMessage(frame.readText())
                        if (msg != null) {
                            if (!CurrentUser.options!!.ignoreList.contains(msg.nick)) {
                                runOnUiThread {
                                    if (msg.privMsg) {
                                        adapter.add(
                                            PrivateChatMessage(
                                                msg, true
                                            )
                                        )
                                        if (CurrentUser.options!!.notifications) {
                                            displayNotification(msg)
                                        }
                                    } else {
                                        var consecutiveMessage = false
                                        if (adapter.itemCount > 0) {
                                            if (adapter.getItem(adapter.itemCount - 1).layout == R.layout.chat_message_item || adapter.getItem(
                                                    adapter.itemCount - 1
                                                ).layout == R.layout.chat_message_item_consecutive_nick
                                            ) {
                                                val lastMessage =
                                                    adapter.getItem(adapter.itemCount - 1) as ChatMessage
                                                consecutiveMessage =
                                                    lastMessage.isNickSame(msg.nick)
                                            }

                                        }
                                        if (msg.entities.emotes != null && msg.entities.emotes!!.isNotEmpty() && msg.entities.emotes!![0].combo > 1) {
                                            if (msg.entities.emotes!![0].combo == 2) {
                                                adapter.removeGroupAtAdapterPosition(adapter.itemCount - 1)
                                                adapter.add(ChatMessageCombo(msg))
                                            } else {
                                                if (adapter.getItem(adapter.itemCount - 1).layout == R.layout.chat_message_item_emote_combo) {
                                                    val lastMessageCombo =
                                                        adapter.getItem(adapter.itemCount - 1) as ChatMessageCombo
                                                    lastMessageCombo.setCombo(msg.entities.emotes!![0].combo)
                                                    adapter.notifyItemChanged(adapter.itemCount - 1)
                                                }
                                            }

                                        } else {
                                            if (adapter.itemCount > 0 && adapter.getItem(adapter.itemCount - 1).layout == R.layout.chat_message_item_emote_combo) {
                                                val lastMessage =
                                                    adapter.getItem(adapter.itemCount - 1) as ChatMessageCombo
                                                lastMessage.state = 1
                                                adapter.notifyItemChanged(adapter.itemCount - 1)
                                            }
                                            adapter.add(
                                                ChatMessage(msg, consecutiveMessage)
                                            )
                                        }
                                    }
                                    val layoutTest =
                                        recyclerViewChat.layoutManager as LinearLayoutManager
                                    val lastItem = layoutTest.findLastVisibleItemPosition()
                                    if (lastItem >= recyclerViewChat.adapter!!.itemCount - 3) {
                                        recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
                                    }
                                }
                            }
                        }
                    }
                    is Frame.Binary -> println(frame.readBytes())
                }
            }
        }

        private fun parseMessage(input: String): Message? {
            val msg = input.split(" ", limit = 2)
            when (msg[0]) {
                "NAMES" -> {
                    val names: NamesMessage = Klaxon().parse(msg[1])!!
                    CurrentUser.users = names.users.toMutableList()
                    CurrentUser.connectionCount = names.connectioncount
                    runOnUiThread {
                        if (adapter.itemCount > 0 && adapter.getItem(adapter.itemCount - 1).layout == R.layout.chat_message_item_emote_combo) {
                            val lastMessage =
                                adapter.getItem(adapter.itemCount - 1) as ChatMessageCombo
                            lastMessage.state = 1
                            adapter.notifyItemChanged(adapter.itemCount - 1)
                        }
                        adapter.add(
                            ChatMessage(
                                Message(
                                    false,
                                    "Info",
                                    "Connected users: ${CurrentUser.connectionCount}"
                                )
                            )
                        )
                        recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
                    }
                }
                "JOIN" -> {
                    val userJoin = Klaxon().parse<ChatUser>(msg[1])
                    if (!CurrentUser.users!!.contains(userJoin)) {
                        CurrentUser.users!!.add(userJoin!!)
                    }
                }
                "QUIT" -> {
                    val userQuit = Klaxon().parse<ChatUser>(msg[1])
                    if (CurrentUser.users!!.contains(userQuit)) {
                        CurrentUser.users!!.remove(userQuit)
                    }
                }
                "PRIVMSG" -> {
                    val message = Klaxon().parse<Message>(msg[1])!!
                    message.privMsg = true
                    return message
                }
                "MSG" -> {
                    val message = Klaxon().parse<Message>(msg[1])!!
                    if (CurrentUser.options!!.hideNsfw && message.entities.links!!.isNotEmpty()
                        && message.entities.tags!!.isNotEmpty()
                    ) {
                        message.entities.tags!!.forEach {
                            if (it.name == "nsfw" || it.name == "nsfl") {
                                return null
                            }
                        }
                    }

                    if (CurrentUser.options!!.ignoreList.isNotEmpty()) {
                        CurrentUser.options!!.ignoreList.forEach {
                            if (message.nick == it) {
                                return null
                            }
                            if (CurrentUser.options!!.harshIgnore) {
                                if (message.data.contains(it)) {
                                    return null
                                }
                            }
                        }
                    }
                    return message
                }
                "MUTE" -> {
                    val message = Klaxon().parse<Message>(msg[1])
                    message!!.data = message.data.plus(" muted by Bot.")
                    return message
                }
            }
            return null
        }
    }
}
