package gg.strims.android.callbacks

import android.graphics.drawable.Drawable
import android.widget.TextView
import java.lang.ref.WeakReference

class DrawableCallback(textView: TextView?) :
    Drawable.Callback {
    private val textViewWeakReference: WeakReference<TextView> = WeakReference(textView!!)
    override fun invalidateDrawable(who: Drawable) {
        if (textViewWeakReference.get() != null) {
            textViewWeakReference.get()!!.invalidate()
        }
    }

    override fun scheduleDrawable(
        who: Drawable,
        what: Runnable,
        `when`: Long
    ) {
        if (textViewWeakReference.get() != null) {
            textViewWeakReference.get()!!.postDelayed(what, `when`)
        }
    }

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
        if (textViewWeakReference.get() != null) {
            textViewWeakReference.get()!!.removeCallbacks(what)
        }
    }
}