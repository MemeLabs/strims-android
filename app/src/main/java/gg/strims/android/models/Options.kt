package gg.strims.android.models

class Options(
    var showTime: Boolean,
    var ignoreList: MutableList<String>,
    var harshIgnore: Boolean,
    var hideNsfw: Boolean,
    var notifications: Boolean,
    var greentext: Boolean,
    var emotes: Boolean,
    var customHighlights: MutableList<String>
) {
    constructor(): this(false, mutableListOf(), false, false , false, false, false, mutableListOf())
}