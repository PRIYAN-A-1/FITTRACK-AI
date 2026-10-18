package com.fittrack.ai

import android.app.Application
import com.fittrack.ai.data.local.FitTrackDatabase
import com.fittrack.ai.data.local.datastore.SessionManager
import com.fittrack.ai.data.repository.*

class FitTrackApp : Application() {

    lateinit var database: FitTrackDatabase
        private set

    lateinit var sessionManager: SessionManager
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var mealRepository: MealRepository
        private set

    lateinit var scanRepository: ScanRepository
        private set

    lateinit var trackingRepository: TrackingRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = FitTrackDatabase.getDatabase(this)
        sessionManager = SessionManager(this)

        authRepository = AuthRepository(this)
        mealRepository = MealRepository(this)
        scanRepository = ScanRepository(this)
        trackingRepository = TrackingRepository(this)
    }

    companion object {
        lateinit var instance: FitTrackApp
            private set
    }
}
