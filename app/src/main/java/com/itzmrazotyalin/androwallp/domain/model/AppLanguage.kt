package com.itzmrazotyalin.androwallp.domain.model

import androidx.annotation.StringRes
import com.itzmrazotyalin.androwallp.R

enum class AppLanguage(
    @get:StringRes val labelRes: Int,
) {
    ENGLISH(labelRes = R.string.settings_language_english),
}