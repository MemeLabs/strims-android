package gg.strims.android.models

class Options(
    var showTime: Boolean,
    var ignoreList: MutableList<String>,
    var harshIgnore: Boolean,
    var hideNsfw: Boolean,
    var notifications: Boolean,
    var greentext: Boolean = true,
    var emotes: Boolean = true,
    var customHighlights: MutableList<String>,
    var showViewerState: Boolean = true
) {
    constructor(): this(false, mutableListOf(), false, false , false, true, true, mutableListOf(), true)
}