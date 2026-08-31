package com.mountaincrab.logrhythm.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserPreferencesRepositoryTest {

    @Test
    fun homeTimelineDensity_defaultsToStandardAndPersistsLocally() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = UserPreferencesRepository(context)

        assertEquals(HomeTimelineDensity.STANDARD, repository.homeTimelineDensity.first())

        repository.setHomeTimelineDensity(HomeTimelineDensity.COMPACT)

        val reloadedRepository = UserPreferencesRepository(context)
        assertEquals(HomeTimelineDensity.COMPACT, reloadedRepository.homeTimelineDensity.first())

        repository.setHomeTimelineDensity(HomeTimelineDensity.STANDARD)
    }

    @Test
    fun homeTimelineDensity_unknownValueFallsBackToStandard() {
        assertEquals(HomeTimelineDensity.STANDARD, HomeTimelineDensity.fromName("ROOMY"))
        assertEquals(HomeTimelineDensity.STANDARD, HomeTimelineDensity.fromName(null))
    }
}
