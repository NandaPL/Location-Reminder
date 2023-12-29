package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.koin.test.inject
import org.robolectric.annotation.Config
import kotlin.test.assertNull
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.CoroutineRule
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.setMain
import org.hamcrest.CoreMatchers

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class SaveReminderViewModelTest : AutoCloseKoinTest() {

    val viewModel: SaveReminderViewModel by inject()
    private lateinit var reminderListRepo: FakeDataSource
    private lateinit var context: Application

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = CoroutineRule()

    @Before
    fun initializeViewModel() {
        stopKoin()
        context = getApplicationContext()
        val myModule = module {
            viewModel {
                SaveReminderViewModel(
                    context,
                    get() as FakeDataSource
                )
            }
            single { FakeDataSource() }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }

        reminderListRepo = get()

        runBlocking {
            reminderListRepo.deleteAllReminders()
        }
    }

    @Test
    fun onClear_ReminderValuesNotNull_SetToNull() {
        // Given - Reminder values are not null
        viewModel.reminderTitle.value = "Test title"
        viewModel.reminderDescription.value = "Test description"
        viewModel.selectedPOI.value = SelectedLocation("Test location", LatLng(123.123, 456.456))
        // When - onClear is called
        viewModel.onClear()
        // Then - Reminder values are null
        assertNull(viewModel.reminderTitle.value)
        assertNull(viewModel.reminderDescription.value)
        assertNull(viewModel.selectedPOI.value)
    }

    @Test
    fun validateEnteredData_withNullTitle_UpdatesSnackbarAndReturnsFalse() {
        // Given - ReminderDataItem with null title
        val reminder = ReminderDataItem(
            null,
            "Test description",
            "Test location",
            123.123,
            456.456
        )
        // When - validateEnteredData is called
        val result = viewModel.validateEnteredData(reminder)
        // Then - showSnackBarInt value is set to "Please enter title"
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title)
        )
        //Then - method returns false
        assertThat(result, `is`(false))
    }

    @Test
    fun validateEnteredData_withNullLocation_UpdatesSnackbarAndReturnsFalse() {
        // Given - ReminderDataItem with null location
        val reminder = ReminderDataItem(
            "Test title",
            "Test description",
            null,
            123.123,
            456.456
        )
        // When - validateEnteredData is called
        val result = viewModel.validateEnteredData(reminder)
        // Then - showSnackBarInt value is set to "Please enter title"
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue(),
            CoreMatchers.`is`(R.string.err_select_location)
        )
        //Then - method returns false
        assertThat(result, CoreMatchers.`is`(false))
    }

    @Test
    fun validateEnteredData_withValidReminder_returnsTrue() {
        // Given - ReminderDataItem with valid values
        val reminder = ReminderDataItem(
            "Test title",
            "Test description",
            "Test location",
            123.123,
            456.456
        )
        // When - validateEnteredData is called
        val result = viewModel.validateEnteredData(reminder)
        // Then - method returns true
        assertThat(result, CoreMatchers.`is`(true))
    }

    @Test
    fun saveReminder_showLoadingSetCorrectly() = runTest {
        Dispatchers.setMain(StandardTestDispatcher())
        // Given - the SaveReminder function is called
        val reminder = ReminderDataItem(
            "Test title",
            "Test description",
            "Test location",
            123.123,
            456.456
        )
        viewModel.saveReminder(reminder)
        // When - the method has started
        // Then - showLoading is set to true
        assertThat(viewModel.showLoading.getOrAwaitValue(), CoreMatchers.`is`(true))
        // When - the method has finished
        advanceUntilIdle()
        // Then - showLoading is set to false
        assertThat(viewModel.showLoading.getOrAwaitValue(), CoreMatchers.`is`(false))
    }

    @Test
    fun saveReminder_showToastSetCorrectly() = runTest {
        // Given - a valid ReminderDataItem
        val reminder = ReminderDataItem(
            "Test title",
            "Test description",
            "Test location",
            123.123,
            456.456
        )
        // When - saveReminder is called
        viewModel.saveReminder(reminder)
        // Then - showToast value is set to R.string.reminderSaved
        assertThat(
            viewModel.showToast.getOrAwaitValue(),
            CoreMatchers.`is`(context.resources.getString(R.string.reminder_saved))
        )
    }

    @Test
    fun saveReminder_navigationCommandSetCorrectly() = runTest {
        // Given - a valid ReminderDataItem
        val reminder = ReminderDataItem(
            "Test title",
            "Test description",
            "Test location",
            123.123,
            456.456
        )
        // When - saveReminder is called
        viewModel.saveReminder(reminder)
        // Then - navigationCommand value is set to NavigationCommand.Back
        assertThat(viewModel.navigationCommand.getOrAwaitValue(),
            CoreMatchers.`is`(NavigationCommand.Back)
        )
    }

    @Test
    fun savereminder_validReminder_savedInRepository() = runTest {
        // Given - a valid ReminderDataItem
        val reminder = ReminderDataItem(
            "Test title",
            "Test description",
            "Test location",
            123.123,
            456.456
        )
        val asDto = ReminderDTO(
            reminder.title,
            reminder.description,
            reminder.location,
            reminder.latitude,
            reminder.longitude,
            reminder.id
        )
        // Given - repository reminders list is empty
        reminderListRepo.deleteAllReminders()
        // When - Save Reminder is called
        viewModel.saveReminder(reminder)
        // Then - reminder is saved in repository
        assertThat(reminderListRepo.listReminder.size, `is`(1))
        assertThat(reminderListRepo.listReminder.contains(asDto), `is`(true))
    }

    fun validateAndSaveReminder_invalidReminder_notSavedInRepository() = runTest {
        // Given - a reminderDataItem with missing values
        val reminder = ReminderDataItem(
            null,
            "Test description",
            "Test location",
            123.123,
            456.456
        )

        val asDto = ReminderDTO(
            reminder.title,
            reminder.description,
            reminder.location,
            reminder.latitude,
            reminder.longitude,
            reminder.id
        )
        // Given - repository reminders list is empty
        reminderListRepo.deleteAllReminders()
        // When - validateAndSaveReminder is called
        viewModel.validateEnteredData(reminder)
        // Then - reminder is not saved
        assertThat(reminderListRepo.listReminder.size, `is`(0))
        assertThat(reminderListRepo.listReminder.contains(asDto), `is`(false))
    }

    fun validateAndSaveReminder_validReminder_savedInRepository() = runTest {
        // Given - a reminderDataItem with missing values
        val reminder = ReminderDataItem(
            "Test title",
            "Test description",
            "Test location",
            123.123,
            456.456
        )

        val asDto = ReminderDTO(
            reminder.title,
            reminder.description,
            reminder.location,
            reminder.latitude,
            reminder.longitude,
            reminder.id
        )
        // Given - repository reminders list is empty
        reminderListRepo.deleteAllReminders()
        // When - validateAndSaveReminder is called
        viewModel.validateEnteredData(reminder)
        // Then - reminder is not saved
        assertThat(reminderListRepo.listReminder.size, `is`(1))
        assertThat(reminderListRepo.listReminder.contains(asDto), `is`(true))
    }

}