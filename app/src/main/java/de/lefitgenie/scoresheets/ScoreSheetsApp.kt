package de.lefitgenie.scoresheets

import android.app.Application
import de.lefitgenie.scoresheets.data.AppDatabase
import de.lefitgenie.scoresheets.data.ScoreRepository

class ScoreSheetsApp : Application() {
    val repository: ScoreRepository by lazy {
        ScoreRepository(AppDatabase.build(this).scoreDao())
    }
}
