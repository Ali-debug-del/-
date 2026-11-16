package com.example.data.model

enum class BlockMode {
    NONE,
    MEMORY_ONLY,
    INTERNET_ONLY,
    BOTH;

    val blocksMemory: Boolean
        get() = this == MEMORY_ONLY || this == BOTH

    val blocksInternet: Boolean
        get() = this == INTERNET_ONLY || this == BOTH

    val isBlocked: Boolean
        get() = this != NONE

    fun displayLabelAr(): String = when (this) {
        NONE -> "مسموح"
        MEMORY_ONLY -> "الذاكرة فقط"
        INTERNET_ONLY -> "الإنترنت فقط"
        BOTH -> "الذاكرة والإنترنت"
    }
}
