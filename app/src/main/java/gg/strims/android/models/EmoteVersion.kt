package gg.strims.android.models

class EmoteVersion(val path: String, val animated: Boolean, val dimensions: Dimension, val size: String) {

    constructor(): this("", false, Dimension(), "")
}