package gg.strims.android.models

class ViewerState(
    var channel: Channel?,
    var nick: String,
    var online: Boolean
) {
    constructor(): this(Channel(), "", false)

}