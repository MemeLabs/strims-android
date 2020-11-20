package gg.strims.android.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class ViewerState(
    var channel: Channel?,
    var nick: String,
    var online: Boolean
) : Parcelable {
    constructor(): this(Channel(), "", false)

}

@Parcelize
class Channel(
    var channel: String,
    var path: String?,
    var service: String
) : Parcelable {
    constructor(): this("", "", "")
}