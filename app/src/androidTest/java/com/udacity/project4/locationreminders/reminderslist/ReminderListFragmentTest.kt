package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Build
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.annotation.Config
import atPosition
import com.udacity.project4.locationreminders.data.local.FakeReminderDataSource
import org.junit.Rule
import org.koin.test.junit5.AutoCloseKoinTest

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest: AutoCloseKoinTest() {

    private lateinit var fakeRepository: FakeReminderDataSource
    private lateinit var appContext: Application

    @Before
    fun init() {
        stopKoin()
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as FakeReminderDataSource
                )
            }
            single { FakeReminderDataSource() }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }

        fakeRepository = get()

        runBlocking {
            fakeRepository.deleteAllReminders()
        }
    }

    // Execute each task concurrently using architecture components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Test
    fun remindersInRepository_displayedInList() = runTest {
        // Given - Reminders saved in Repo
        val reminderOne = ReminderDTO("Test title 1", "Test description 1",
            "Test location 1", 123.123, 456.456)
        val reminderTwo = ReminderDTO("Test title 2", "Test description 2",
            "Test location 2", 124.124, 457.457)
        val reminderThree = ReminderDTO("Test title 3", "Test description 3",
            "Test location 3", 125.125, 458.458)
        fakeRepository.saveReminder(reminderOne)
        fakeRepository.saveReminder(reminderTwo)
        fakeRepository.saveReminder(reminderThree)
        // When - Reminders fragment is launched
        launchFragmentInContainer<ReminderListFragment>(themeResId =  R.style.AppTheme)
        // Then - reminders are displayed on the screen
        onView(withId(R.id.reminderssRecyclerView)).check(
            matches(atPosition(0, hasDescendant(withText("Test title 1"))))
        )
        onView(withId(R.id.reminderssRecyclerView)).check(
            matches(atPosition(0, hasDescendant(withText("Test description 1"))))
        )
        onView(withId(R.id.reminderssRecyclerView)).check(
            matches(atPosition(0, hasDescendant(withText("Test location 1"))))
        )

        onView(withId(R.id.reminderssRecyclerView)).check(
            matches(atPosition(1, hasDescendant(withText("Test title 2"))))
        )
        onView(withId(R.id.reminderssRecyclerView)).check(
            matches(atPosition(1, hasDescendant(withText("Test description 2"))))
        )
        onView(withId(R.id.reminderssRecyclerView)).check(
            matches(atPosition(1, hasDescendant(withText("Test location 2"))))
        )

        onView(withId(R.id.reminderssRecyclerView)).check(
            matches(atPosition(2, hasDescendant(withText("Test title 3"))))
        )
        onView(withId(R.id.reminderssRecyclerView)).check(
            matches(atPosition(2, hasDescendant(withText("Test description 3"))))
        )
        onView(withId(R.id.reminderssRecyclerView)).check(
            matches(atPosition(2, hasDescendant(withText("Test location 3"))))
        )
    }

    @Test
    fun noReminders_displaysNoDataView(){
        // Given - no reminders in the repository
        // When - Reminders fragment is launched
        launchFragmentInContainer<ReminderListFragment>(themeResId = R.style.AppTheme)
        // Then - the no data text view is displayed
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun fabPressed_navigatesToSaveReminder(){
        // Given - Reminders fragment is launched
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        // When - The fab is pressed
        onView(withId(R.id.addReminderFAB)).perform(click())
        // Then - navigates to save
        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder())
    }

    @Test
    fun repositoryReturnsError_snackbarIsDisplayed(){
        // Given - the repository returns an error
        fakeRepository.returnError = true
        // When - the Reminders fragment is launched
        launchFragmentInContainer<ReminderListFragment>(themeResId = R.style.AppTheme)
        // A snackbar is displayed
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText("Error occurred..")))
    }
}