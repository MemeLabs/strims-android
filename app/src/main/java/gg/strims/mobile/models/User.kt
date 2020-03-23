package gg.strims.mobile.models

class User(
    val username: String,
    val stream_path: String,
    val service: String,
    val channel: String,
    val left_chat: Boolean,
    val is_admin: Boolean,
    val show_hidden: Boolean,
    val show_dgg_chat: Boolean,
    val enable_public_state: Boolean
) {
    constructor(): this("", "", "", "", false, false, false, false, false)
}