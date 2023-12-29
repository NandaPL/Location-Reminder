package com.udacity.project4

import android.Manifest
import android.app.Application
import android.os.Build
import android.widget.Toast
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.test.rule.GrantPermissionRule.grant
import atPosition
import com.google.android.material.internal.ContextUtils.getActivity
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.get
import org.koin.test.inject
import org.koin.test.junit5.AutoCloseKoinTest
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {
    // Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private val dataBindingIdlingResource = DataBindingIdlingResource()
    val saveReminderViewModel: SaveReminderViewModel by inject()

    // Thanks to Stack Overflow user CommonsWare for this helpful solution (https://stackoverflow.com/a/75330411/5264775)
    @Rule
    @JvmField
    var grantPermissionRule: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= 33) {
            grant(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            grant(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        }

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Before
    fun registerIdlingResources() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResources() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    /**
     * This test should be run on API level 29 or lower
     */
    @Test
    fun createAndSaveNewTask() {
        assumeTrue(
            Build.VERSION.SDK_INT <= 29
        )
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.saveReminderLayout)).check(matches(isDisplayed()))
        onView(withId(R.id.tvSelectLocation)).perform(click())
        onView(withId(R.id.mapFragment)).check(matches(isDisplayed()))
        Espresso.closeSoftKeyboard()
        onView(withText(R.string.select_poi))
            .inRoot(withDecorView(not(`is`(getActivity(appContext)?.window?.decorView))))
            .check(matches(isDisplayed()))
        Thread.sleep(3500)
        onView(withId(R.id.saveReminderFragment)).perform(click())
        assertNotNull(saveReminderViewModel.selectedPOI.value)
        onView(withId(R.id.tvSelectLocation)).check(matches(withText(saveReminderViewModel.selectedPOI.value.toString())))
        onView(withId(R.id.etReminderTitle)).perform(typeText("Test title"))
        onView(withId(R.id.etReminderDescription)).perform(typeText("Test description"))
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.btnSaveReminder)).perform(click())
        onView(withText(R.string.reminder_saved))
            .inRoot(withDecorView(not(`is`(getActivity(appContext)?.window?.decorView))))
            .check(matches(isDisplayed()))
        onView(withId(R.id.reminderssRecyclerView)).check(
            matches(
                atPosition(
                    0,
                    ViewMatchers.hasDescendant(
                        withText("Test title")))))
        onView(withId(R.id.reminderssRecyclerView)).check(
            matches(
                atPosition(
                    0,
                    ViewMatchers.hasDescendant(
                        withText("Test description")))))
        onView(withId(R.id.reminderssRecyclerView)).check(
            matches(
                atPosition(
                    0, ViewMatchers
                        .hasDescendant(
                            withText(appContext.resources.getString(R.string.dropped_pin))))))
        activityScenario.close()
    }
}