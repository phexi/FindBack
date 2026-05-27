package com.itemfinder.app

import android.app.Application
import com.itemfinder.app.data.AppDatabase

class ItemFinderApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }
}
