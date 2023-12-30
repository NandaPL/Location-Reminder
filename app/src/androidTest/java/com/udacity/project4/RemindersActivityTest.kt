package com.udacity.project4

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.core.app.ActivityScenario
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.get
import org.koin.test.junit5.AutoCloseKoinTest

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {
    // Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private var dataBindingIdlingResource = DataBindingIdlingResource()

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
    //This function use to test reminderListData
    @Test
    fun reminderList_DataNotFound() {
        //start up taken screen
        val activity = ActivityScenario.launch(RemindersActivity::class.java)
        // add screen in binding
        dataBindingIdlingResource.monitorActivity(activity)

        //vreify item is displayed
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))

        activity.close()
    }

    //this function use to now buttonSnackBar
    @Test
    fun saveReminder_showSnackBar_noTitleFound() = runBlocking {
        //start up taken screen
        val activity = ActivityScenario.launch(RemindersActivity::class.java)
        // add screen in binding
        dataBindingIdlingResource.monitorActivity(activity)

        // click  to navgation to saveReminder screen
        onView(withId(R.id.addReminderFAB)).perform(click())
        //click saveReminder button to check from snakBat
        onView(withId(R.id.btnSaveReminder)).perform(click())

        //verify snackBar value  is displayed
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(isDisplayed()))
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText(R.string.err_enter_title)))

        //make sure activity is close after finished test
        activity.close()
    }

    @Test
    fun saveReminder_showSnackBar_nolocationFound() = runBlocking {

        //start up taken screen
        val activity = ActivityScenario.launch(RemindersActivity::class.java)
        // add screen in binding
        dataBindingIdlingResource.monitorActivity(activity)

        // click  to navgation to saveReminder screen
        onView(withId(R.id.addReminderFAB)).perform(click())

        // Add title to saveReminder to display in list
        onView(withId(R.id.etReminderTitle)).perform(
            typeText("newReminder"),
            closeSoftKeyboard()
        )
        // Add description to saveReminder to display in list
        onView(withId(R.id.etReminderDescription)).perform(
            typeText("new"),
            closeSoftKeyboard()
        )
        // add to Nav to reminderList
        onView(withId(R.id.btnSaveReminder)).perform(click())
        // check snackbar is display
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(isDisplayed()))
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText(R.string.err_select_location)))
        //make sure activity is close after finished test
        activity.close()

    }


    @Test
    fun selectLocationAndEnterTitle_NavigateToListFragment_DataFound() {
        //start up taken screen
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        val activity = activityScenario.getActivity()

        dataBindingIdlingResource.monitorActivity(activityScenario)
        activityScenario.moveToState(Lifecycle.State.RESUMED)

        // click  to navgation to saveReminder screen
        onView(withId(R.id.addReminderFAB)).perform(click())
        // Add title to saveReminder to display in list
        onView(withId(R.id.etReminderTitle)).perform(
            typeText("newReminder"),
            closeSoftKeyboard()
        )
        // Add description to saveReminder to display in list
        onView(withId(R.id.etReminderDescription)).perform(
            typeText("new"),
            closeSoftKeyboard()
        )

        // click  to navgation to select Location screen and add location
        onView(withId(R.id.tvSelectLocation)).perform(click())
        onView(withId(R.id.mapFragment)).perform(longClick())
        onView(withId(R.id.btnSaveReminderLocation)).perform(click())
        onView(withId(R.id.btnSaveReminder)).perform(click())
        //check the data is displayed
        onView(withId(R.id.noDataTextView)).check(matches( not(isDisplayed())))

        //check is toast is displayed reminder saved
        onView(withText(R.string.reminder_saved)).inRoot(withDecorView(not(`is`(activity?.window?.decorView)))).check(
            matches(isDisplayed())
        )
        //make sure activity is close after finished test
        activityScenario.close()
    }
}