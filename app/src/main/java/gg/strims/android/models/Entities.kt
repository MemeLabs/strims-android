package gg.strims.android.models

class Entities(
    var emotes: List<ChatEmote>?,
    var spoilers: List<Bounds>?,
    var codes: List<Bounds>?,
    var greentext: Bounds?,
    var me: Bounds?,
    var links: List<Link>?,
    var nicks: List<Nick>?,
    var tags: List<Tag>?
) {

    constructor(): this(listOf(), listOf(), listOf(), Bounds(), Bounds(), listOf(), listOf(), listOf())
}