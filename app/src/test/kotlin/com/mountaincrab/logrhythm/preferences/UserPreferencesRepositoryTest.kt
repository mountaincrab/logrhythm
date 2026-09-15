package com.mountaincrab.logrhythm.preferences

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Robolectric 4.13 ships no SDK 35 sandbox, so the default (targetSdk = 35) fails the whole
// class at initialization. Pin the newest it does have; nothing here is SDK-sensitive.
//
// A plain Application, too: LogRhythmApplication starts Koin in onCreate, and Robolectric
// reinstalls the application per test in one JVM, so the second start throws. These tests
// construct the repository directly and need no graph.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
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

    @Test
    fun quickAddWidgetConfig_isPerWidgetAndSurvivesReload() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = UserPreferencesRepository(context)
        val first = 4001
        val second = 4002

        assertNull(repository.getQuickAddWidgetConfig(first))

        repository.setQuickAddWidgetConfig(
            first,
            QuickAddWidgetConfig(profileId = "default", foodItemId = "tea", quantity = 1.0),
        )

        val reloadedRepository = UserPreferencesRepository(context)
        assertEquals(
            QuickAddWidgetConfig(profileId = "default", foodItemId = "tea", quantity = 1.0),
            reloadedRepository.getQuickAddWidgetConfig(first),
        )
        // A second tile is a second shortcut, not a shared one.
        assertNull(reloadedRepository.getQuickAddWidgetConfig(second))

        repository.clearQuickAddWidget(first)
    }

    /** Widget ids are reused by the framework, so a deleted tile must not leave a config behind. */
    @Test
    fun quickAddWidgetConfig_isClearedWithTheWidget() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = UserPreferencesRepository(context)
        val appWidgetId = 4010

        repository.setQuickAddWidgetConfig(
            appWidgetId,
            QuickAddWidgetConfig(profileId = "p1", foodItemId = "coffee", quantity = 2.0),
        )
        repository.setQuickAddWidgetLoggedAt(appWidgetId, 1_700_000_000_000L)
        assertEquals(1_700_000_000_000L, repository.quickAddWidgetLoggedAt(appWidgetId).first())

        repository.clearQuickAddWidget(appWidgetId)

        assertNull(repository.getQuickAddWidgetConfig(appWidgetId))
        assertNull(repository.quickAddWidgetLoggedAt(appWidgetId).first())
    }

    @Test
    fun quickAddWidgetConfig_rejectsUnloggableShortcuts() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                UserPreferencesRepository(ApplicationProvider.getApplicationContext())
                    .setQuickAddWidgetConfig(
                        4020,
                        QuickAddWidgetConfig(profileId = "default", foodItemId = "tea", quantity = 0.0),
                    )
            }
        }
    }
}
