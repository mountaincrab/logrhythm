package com.mountaincrab.logrhythm.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun quickAddFoodItems_areOrderedProfileSpecificAndPersisted() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = UserPreferencesRepository(context)
        val suffix = java.util.UUID.randomUUID().toString()
        val firstProfile = "quick-add-profile-one-$suffix"
        val secondProfile = "quick-add-profile-two-$suffix"

        assertNull(repository.quickAddFoodItemIds(firstProfile).first())
        assertNull(repository.quickAddFoodItemIds(secondProfile).first())

        repository.setQuickAddFoodItemIds(firstProfile, listOf("porridge", "coffee", "banana"))

        val reloadedRepository = UserPreferencesRepository(context)
        assertEquals(
            listOf("porridge", "coffee", "banana"),
            reloadedRepository.quickAddFoodItemIds(firstProfile).first(),
        )
        assertNull(reloadedRepository.quickAddFoodItemIds(secondProfile).first())

        repository.setQuickAddFoodItemIds(secondProfile, emptyList())
        assertEquals(emptyList<String>(), repository.quickAddFoodItemIds(secondProfile).first())
    }

    @Test
    fun quickAddFoodItems_removeDuplicatesAndRespectTheLimit() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = UserPreferencesRepository(context)
        val profileId = "quick-add-sanitised-profile-${java.util.UUID.randomUUID()}"

        repository.setQuickAddFoodItemIds(
            profileId,
            listOf("one", "two", "one", "three", "four", "five", "six"),
        )

        assertEquals(
            listOf("one", "two", "three", "four", "five"),
            repository.quickAddFoodItemIds(profileId).first(),
        )
    }
}
