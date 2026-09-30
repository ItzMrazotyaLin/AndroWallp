package com.itzmrazotyalin.androwalp.domain.model

import androidx.annotation.StringRes
import com.itzmrazotyalin.androwalp.R

enum class AppLanguage(
    @get:StringRes val labelRes: Int,
) {
    ENGLISH(labelRes = R.string.settings_language_english),
}