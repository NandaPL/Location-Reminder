package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.CoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.hamcrest.Matchers.*
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.koin.test.inject
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class RemindersListViewModelTest: AutoCloseKoinTest() {

    val viewModel: RemindersListViewModel by inject()
    private lateinit var reminderListRepo: FakeDataSource
    private lateinit var context: Application

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = CoroutineRule()

    @Before
    fun init() {
        stopKoin()
        context = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
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
    fun loadReminders_showLoadingSetCorrectly() = runTest{
        Dispatchers.setMain(StandardTestDispatcher())
        //Given - loadReminders function is called
        viewModel.loadReminders()
        // When - the function has started
        // Then showLoading value is true
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))
        // When - the function has progressed to the end
        advanceUntilIdle()
        // Then - showLoading value is false
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun loadReminders_EmptyList_ShowNoDataTrue() = runTest{
        // Given - there are no reminders
        reminderListRepo.deleteAllReminders()
        // When - loadReminders is called
        viewModel.loadReminders()
        // Then - Show no data is set to true
        assertThat(viewModel.showNoData.getOrAwaitValue(), `is`(true))
    }

    @Test
    fun loadReminders_RepoReturnsError_SnackbarMessageSetCorrectly() = runTest{
        // Given - the repository returns an error
        reminderListRepo.returnError = true
        // When - loadReminders is called
        viewModel.loadReminders()
        // Then - showSnackBar variable is updated with the correct message
        assertThat(viewModel.showSnackBar.getOrAwaitValue(), `is`("Test exception"))
    }

    @Test
    fun loadReminders_RepoReturnsReminders_ViewModelRemindersListUpdatedCorrectly() = runTest {
        // Given - the repository has reminders
        reminderListRepo.saveReminder(ReminderDTO("Title1", "Description1", "Location1", 123.123, 456.456))
        reminderListRepo.saveReminder(ReminderDTO("Title2", "Description2", "Location2", 124.124, 457.457))
        reminderListRepo.saveReminder(ReminderDTO("Title3", "Description3", "Location3", 125.125, 458.458))
        // When - loadReminders is called
        viewModel.loadReminders()
        // Then - the remindersList is populated with the reminders from the repository
        assertThat(viewModel.remindersList.value?.size, `is`(3))
    }
}