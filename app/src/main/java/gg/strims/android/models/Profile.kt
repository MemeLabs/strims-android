package gg.strims.android.models

class Profile(
    val username: String,
    var stream_path: String,
    var service: String,
    var channel: String,
    var left_chat: Boolean,
    var is_admin: Boolean,
    var show_hidden: Boolean,
    var show_dgg_chat: Boolean,
    var enable_public_state: Boolean
) {
    constructor(): this("", "", "", "", false, false, false, false, false)
}