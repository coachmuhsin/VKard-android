package com.vkard.pro.presentation.update

import android.content.Context
import com.vkard.pro.BuildConfig

class UpdateManager(private val context: Context) {

    fun getInstalledVersionCode(): Long {
        return BuildConfig.VERSION_CODE.toLong()
    }

    fun getInstalledVersionName(): String {
        return BuildConfig.VERSION_NAME
    }
}
