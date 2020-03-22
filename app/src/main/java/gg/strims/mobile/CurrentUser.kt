package gg.strims.mobile

object CurrentUser {
    var user: User? = null
    var options: Options? = null
    var tempSequentialNick: String? = null
    var tempHighlightNick: MutableList<String>? = null
    var users: MutableList<ChatUser>? = null
    var connectionCount: Int? = null
}