package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var localDataSource: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        localDataSource =
            RemindersLocalRepository(
                database.reminderDao(),
                Dispatchers.Main
            )
    }

    @Test
    fun insertReminderAndGetById() = runTest {
        // Given - a reminder is inserted into the db
        val reminder = ReminderDTO(
            "Test title", "Test description",
            "Test location", 123.123, 456.456
        )
        localDataSource.saveReminder(reminder)
        // When - the reminder is loaded by id
        val loaded = localDataSource.getReminder(reminder.id)
        // Then - the returned reminder contains the expected values
        MatcherAssert.assertThat(loaded as Result.Success, CoreMatchers.notNullValue())
        MatcherAssert.assertThat(loaded.data.id, CoreMatchers.`is`(reminder.id))
        MatcherAssert.assertThat(loaded.data.title, CoreMatchers.`is`(reminder.title))
        MatcherAssert.assertThat(loaded.data.description, CoreMatchers.`is`(reminder.description))
        MatcherAssert.assertThat(loaded.data.location, CoreMatchers.`is`(reminder.location))
        MatcherAssert.assertThat(loaded.data.latitude, CoreMatchers.`is`(reminder.latitude))
        MatcherAssert.assertThat(loaded.data.longitude, CoreMatchers.`is`(reminder.longitude))
    }

    @Test
    fun getReminderById_idDoesNotExist() = runTest {
        // Given - no reminder with id = 1 exists in the db
        // When - the getReminder method is called with the non-existent id
        val loaded = localDataSource.getReminder("1")
        // Then - an error is returned
        MatcherAssert.assertThat(loaded as Result.Error, CoreMatchers.notNullValue())
        MatcherAssert.assertThat(loaded.message, CoreMatchers.`is`("Reminder not found!"))
    }

    @Test
    fun saveRemindersAndGetAll() = runTest {
        // Given - Reminders exist in the db
        val reminderOne = ReminderDTO("Test title", "Test description",
            "Test location", 123.123, 456.456)
        val reminderTwo = ReminderDTO("Test title 2", "Test description 2",
            "Test location 2", 124.124, 457.457)
        val reminderThree = ReminderDTO("Test title 3", "Test description 3",
            "Test location 3", 125.125, 458.458)
        localDataSource.saveReminder(reminderOne)
        localDataSource.saveReminder(reminderTwo)
        localDataSource.saveReminder(reminderThree)
        // When - getReminders is called
        val retrievedList = localDataSource.getReminders()
        // Then - all existing reminders are returned
        MatcherAssert.assertThat(retrievedList as Result.Success, CoreMatchers.notNullValue())
        MatcherAssert.assertThat(retrievedList.data.size, CoreMatchers.`is`(3))
        MatcherAssert.assertThat(retrievedList.data.contains(reminderOne), CoreMatchers.`is`(true))
        MatcherAssert.assertThat(retrievedList.data.contains(reminderTwo), CoreMatchers.`is`(true))
        MatcherAssert.assertThat(retrievedList.data.contains(reminderThree), CoreMatchers.`is`(true))
    }

    @Test
    fun deleteAllReminders() = runTest {
        // Given - Reminders exist in the db
        val reminderOne = ReminderDTO("Test title", "Test description",
            "Test location", 123.123, 456.456)
        val reminderTwo = ReminderDTO("Test title 2", "Test description 2",
            "Test location 2", 124.124, 457.457)
        val reminderThree = ReminderDTO("Test title 3", "Test description 3",
            "Test location 3", 125.125, 458.458)
        localDataSource.saveReminder(reminderOne)
        localDataSource.saveReminder(reminderTwo)
        localDataSource.saveReminder(reminderThree)
        // When - the deleteAllReminders method is called
        localDataSource.deleteAllReminders()
        // Then - all reminders are removed from the db
        val loaded = localDataSource.getReminders()
        MatcherAssert.assertThat((loaded as Result.Success).data.size, CoreMatchers.`is`(0))
    }

    @After
    fun cleanUp() {
        database.close()
    }

}