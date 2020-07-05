package gg.strims.android.customspans

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.text.style.ImageSpan

// https://stackoverflow.com/a/60763554
// https://stackoverflow.com/questions/25628258/align-text-around-imagespan-center-vertical
class CenteredImageSpan(
    context: Context,
    bitmap: Bitmap
) : ImageSpan(context, bitmap) {
    private var initialDescent: Int = 0
    private var extraSpace: Int = 0
    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        val rect = drawable.bounds
        if (fm != null) {
            // Centers the text with the ImageSpan
            if (rect.bottom - (fm.descent - fm.ascent) >= 0) {
                // Stores the initial descent and computes the margin available
                initialDescent = fm.descent;
                extraSpace = rect.bottom - (fm.descent - fm.ascent);
            }

            fm.descent = extraSpace / 2 + initialDescent;
            fm.bottom = fm.descent;

            fm.ascent = -rect.bottom + fm.descent;
            fm.top = fm.ascent;
        }

        return rect.right;
    }
}