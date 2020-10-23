package gg.strims.android.models

class Channel(
    var channel: String,
    var path: String?,
    var service: String
) {
    constructor(): this("", "", "")
}