package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    private lateinit var database: RemindersDatabase

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @Test
    fun insertReminderAndGetById() = runTest {
        // Given - a reminder is inserted into the db
        val reminder = ReminderDTO("Test title", "Test description",
            "Test location", 123.123, 456.456)
        database.reminderDao().saveReminder(reminder)
        // When - the reminder is loaded by id from the database
        val loaded = database.reminderDao().getReminderById(reminder.id)
        // Then the returned reminder contains the expected values
        assertThat(loaded as ReminderDTO, notNullValue())
        assertThat(loaded.id, `is`(reminder.id))
        assertThat(loaded.title, `is`(reminder.title))
        assertThat(loaded.description, `is`(reminder.description))
        assertThat(loaded.location, `is`(reminder.location))
        assertThat(loaded.latitude, `is`(reminder.latitude))
        assertThat(loaded.longitude, `is`(reminder.longitude))
    }

    @Test
    fun saveRemindersAndGetAll() = runTest {
        // Given - Reminders are inserted into DB
        val reminderOne = ReminderDTO("Test title", "Test description",
            "Test location", 123.123, 456.456)
        val reminderTwo = ReminderDTO("Test title 2", "Test description 2",
            "Test location 2", 124.124, 457.457)
        val reminderThree = ReminderDTO("Test title 3", "Test description 3",
            "Test location 3", 125.125, 458.458)
        database.reminderDao().saveReminder(reminderOne)
        database.reminderDao().saveReminder(reminderTwo)
        database.reminderDao().saveReminder(reminderThree)
        // When - getReminder is called
        val retrievedList = database.reminderDao().getReminders()
        // Then - all of the existing reminders are returned
        assertThat(retrievedList.size, `is`(3))
        assertThat(retrievedList.contains(reminderOne), `is`(true))
        assertThat(retrievedList.contains(reminderTwo), `is`(true))
        assertThat(retrievedList.contains(reminderThree), `is`(true))
    }

    @Test
    fun deleteAllReminders() = runTest {
        // Given - Reminders are inserted into DB
        val reminderOne = ReminderDTO("Test title", "Test description",
            "Test location", 123.123, 456.456)
        val reminderTwo = ReminderDTO("Test title 2", "Test description 2",
            "Test location 2", 124.124, 457.457)
        val reminderThree = ReminderDTO("Test title 3", "Test description 3",
            "Test location 3", 125.125, 458.458)
        database.reminderDao().saveReminder(reminderOne)
        database.reminderDao().saveReminder(reminderTwo)
        database.reminderDao().saveReminder(reminderThree)
        // When - deleteReminders is called
        database.reminderDao().deleteAllReminders()
        // Then - all the reminders are removed from the db
        assertThat(database.reminderDao().getReminders().size, `is`(0))
    }

    @Test
    fun updateExistingReminder() = runTest {
        // Given - a reminder is saved in the db
        val reminder = ReminderDTO("Test title", "Test description",
            "Test location", 123.123, 456.456)
        database.reminderDao().saveReminder(reminder)
        // When - the the save function is called using the same reminder id
        val updatedReminder = ReminderDTO("Updated title", "Updated description",
            "Updated location", 124.124, 457.457, reminder.id)
        database.reminderDao().saveReminder(updatedReminder)
        // Then - the existing reminder was updated and the updated values are returned
        val retrievedReminders = database.reminderDao().getReminders()
        assertThat(retrievedReminders.size, `is`(1))
        val retrievedReminder = retrievedReminders[0]
        assertThat(retrievedReminder.title, `is`("Updated title"))
        assertThat(retrievedReminder.description, `is`("Updated description"))
        assertThat(retrievedReminder.location, `is`("Updated location"))
        assertThat(retrievedReminder.latitude, `is`(124.124))
        assertThat(retrievedReminder.longitude, `is`(457.457))
    }


    @After
    fun closeDb(){
        database.close()
    }
}