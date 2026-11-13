package com.smart222tech.pashaflixbox

import android.content.Context
import android.content.Intent

object PlayerActivityStarter {
    fun open(context: Context, url: String, title: String) {
        context.startActivity(
            Intent(context, PlayerActivity::class.java)
                .putExtra(PlayerActivity.EXTRA_URL, url)
                .putExtra(PlayerActivity.EXTRA_TITLE, title)
        )
    }
}
