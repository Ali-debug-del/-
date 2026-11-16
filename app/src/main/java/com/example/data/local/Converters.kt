package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.BlockMode

class Converters {
    @TypeConverter
    fun fromBlockMode(mode: BlockMode): String = mode.name

    @TypeConverter
    fun toBlockMode(value: String): BlockMode =
        runCatching { BlockMode.valueOf(value) }.getOrDefault(BlockMode.NONE)
}
