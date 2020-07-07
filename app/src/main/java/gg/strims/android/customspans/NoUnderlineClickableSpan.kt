package gg.strims.android.customspans

import android.text.TextPaint
import android.text.style.ClickableSpan

abstract class NoUnderlineClickableSpan : ClickableSpan() {
    override fun updateDrawState(ds: TextPaint) {
        ds.isUnderlineText = false
    }
}