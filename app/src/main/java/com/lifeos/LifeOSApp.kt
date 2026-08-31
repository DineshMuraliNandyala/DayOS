package com.lifeos

import android.app.Application
import com.lifeos.data.db.LifeOSDatabase

/**
 * Application class. Holds the Room database singleton and any other
 * process-scoped resources.
 *
 * Registered in AndroidManifest.xml via android:name=".LifeOSApp".
 */
class LifeOSApp : Application() {

    /**
     * Room database — lazily created on first access, lives for the
     * process lifetime.
     */
    val database: LifeOSDatabase by lazy {
        LifeOSDatabase.getInstance(this)
    }
}
