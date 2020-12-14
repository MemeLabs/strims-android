package gg.strims.android.utils

import android.graphics.Bitmap
import android.graphics.Matrix

fun Bitmap.flip(degrees: Float): Bitmap {
    val matrix = Matrix()
    matrix.preScale(-1f, 1f)
    matrix.apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Bitmap.mirror(): Bitmap {
    val matrix = Matrix()
    matrix.preScale(-1f, 1f)
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}