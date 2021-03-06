package gg.strims.android.viewholders

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.util.DisplayMetrics
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.TextView
import com.xwray.groupie.viewbinding.BindableItem
import gg.strims.android.singletons.CurrentUser
import gg.strims.android.R
import gg.strims.android.adapters.CustomAdapter
import gg.strims.android.utils.createMessageTextView
import gg.strims.android.databinding.ChatMessageItemEmoteComboBinding
import gg.strims.android.models.Message
import io.ktor.util.*
import java.text.SimpleDateFormat
import java.util.*

@KtorExperimentalAPI
class ChatMessageCombo(
    private val messageData: Message,
    private val adapter: CustomAdapter,
    private var count: Int = 2
) :
    BindableItem<ChatMessageItemEmoteComboBinding>() {
    var state: Int = 0 // 0 hit animation // 1 ccccombo animation // 2 combo static
    private var comboCountInitialSize = -1f
    private var xInitialSize = -1f
    private var hitsInitialSize = -1f

    override fun getLayout(): Int = R.layout.chat_message_item_emote_combo

    override fun bind(viewBinding: ChatMessageItemEmoteComboBinding, position: Int) {
        with (viewBinding) {
            if (comboCountInitialSize == -1f) {
                comboCountInitialSize =
                    comboCountChatMessageCombo.textSize / root.context.resources.displayMetrics.scaledDensity
            }
            if (xInitialSize == -1f) {
                xInitialSize =
                    xChatMessageCombo.textSize / root.context.resources.displayMetrics.scaledDensity
            }
            if (hitsInitialSize == -1f) {
                hitsInitialSize =
                    hitsComboChatMessageCombo.textSize / root.context.resources.displayMetrics.scaledDensity
            }
            if (CurrentUser.optionsLiveData.value?.showTime!!) {
                val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val time = dateFormat.format(messageData.timestamp)
                timestampChatMessageCombo.visibility = View.VISIBLE
                timestampChatMessageCombo.text = time
            }
            if (adapter.tempHighlightNick != null) {
                root.alpha = 0.5f
            } else {
                root.alpha = 1f
            }
            root.setOnClickListener {
                adapter.tempHighlightNick = null
                adapter.notifyDataSetChanged()
            }

            comboCountChatMessageCombo.text = "$count"

            if (count >= 10) {
                hitsComboChatMessageCombo.setTypeface(
                    hitsComboChatMessageCombo.typeface,
                    Typeface.BOLD_ITALIC
                )
            }
            createMessageTextView(
                root.context,
                messageData,
                messageChatMessageCombo,
                emotes = CurrentUser.optionsLiveData.value?.emotes!!,
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
//                    comboCountChatMessageCombo.textSize =
//                        (comboCountInitialSize * scaleValue).toFloat()
//                    xChatMessageCombo.textSize =
//                        (xInitialSize * scaleValue).toFloat()
//                    hitsComboChatMessageCombo.textSize =
//                        (hitsInitialSize * scaleValue).toFloat()
                    if (count >= 10) {
                        redSplatChatMessageCombo.visibility = View.VISIBLE
                        graySplatChatMessageCombo.visibility = View.VISIBLE
                    }
                    fun TextView.hitsAnimation() {
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
                    hitsComboChatMessageCombo.text =
                        root.context.resources.getString(R.string.combo_hits)
                    hitsComboChatMessageCombo.hitsAnimation()
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
//                    hitsComboChatMessageCombo.textSize =
//                        (hitsInitialSize * 1.25).toFloat()
//                    xChatMessageCombo.textSize =
//                        (xInitialSize * 1.25).toFloat()
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
                                this@comboAnimation.text =
                                    context.resources.getString(R.string.combo_ccccombo)
                                this@comboAnimation.typeface = Typeface.DEFAULT
                            }

                            override fun onAnimationRepeat(animation: Animation?) {
                                comboColorAnimation.start()
                                this@comboAnimation.startAnimation(slideLeft)
                            }
                        })
                        this.startAnimation(anim)
                    }
                    hitsComboChatMessageCombo.comboAnimation()
                    if (count >= 10) {
                        splatFade(
                            redSplatChatMessageCombo,
                            graySplatChatMessageCombo
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
    }

    override fun initializeViewBinding(view: View): ChatMessageItemEmoteComboBinding {
        return ChatMessageItemEmoteComboBinding.bind(view)
    }

    fun setCombo(comboCount: Int) {
        count = comboCount
    }
}