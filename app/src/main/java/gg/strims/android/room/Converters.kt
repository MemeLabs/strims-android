package gg.strims.android.room

import androidx.room.TypeConverter
import com.beust.klaxon.Klaxon
import com.google.gson.Gson
import gg.strims.android.models.Entities

class Converters {
    @TypeConverter
    fun entitiesToString(entities: Entities): String {
        return Gson().toJson(entities)
    }

    @TypeConverter
    fun stringToEntities(string: String): Entities? {
        return Klaxon().parse<Entities>(string)
    }

    @TypeConverter
    fun featuresToString(array: Array<String>): String {
        return Gson().toJson(array)
    }

    @TypeConverter
    fun stringToFeatures(string: String): Array<String>? {
        if (string != "[]") {
            return Klaxon().parse(string)
        }
        return arrayOf()
    }
}