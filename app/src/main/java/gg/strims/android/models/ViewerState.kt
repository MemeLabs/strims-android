package gg.strims.android.models

class ViewerState(
    var channel: Channel?,
    var nick: String,
    var online: Boolean
) {
    constructor(): this(Channel(), "", false)

}

class Channel(
    var channel: String,
    var path: String?,
    var service: String
) {
    constructor(): this("", "", "")
}