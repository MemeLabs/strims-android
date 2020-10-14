package gg.strims.android.customspans

import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance
import java.lang.reflect.Method

class ColouredUnderlineSpan(mColor: Int) : CharacterStyle(), UpdateAppearance {

    var color = mColor

    override fun updateDrawState(tp: TextPaint) {
        try {
            val method: Method = TextPaint::class.java.getMethod(
                "setUnderlineText",
                Integer.TYPE,
                java.lang.Float.TYPE
            )
            method.invoke(tp, color, 8.0f)
        } catch (e: Exception) {
            tp.isUnderlineText = true
        }
    }
}