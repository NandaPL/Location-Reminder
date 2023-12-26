package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.util.CoroutineRule
import junit.framework.Assert
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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

    private lateinit var remainderRepository: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = CoroutineRule()

    @Before
    fun init() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        remainderRepository = RemindersLocalRepository(database.reminderDao())
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun saveRemainderFunctionSuccess() = runBlocking {
        val reminder = ReminderDTO(
            "Test1", "test1", "test1", 0.0, 0.0, "test1"
        )
        remainderRepository.saveReminder(
            reminder
        )
        val result = remainderRepository.getReminder(reminder.id)
        MatcherAssert.assertThat(result is Result.Success, CoreMatchers.`is`(true))
        result as Result.Success
        MatcherAssert.assertThat(result.data.id, CoreMatchers.`is`(reminder.id))
        MatcherAssert.assertThat(result.data.title, CoreMatchers.`is`(CoreMatchers.notNullValue()))
        MatcherAssert.assertThat(result.data.description, CoreMatchers.`is`(CoreMatchers.notNullValue()))
        MatcherAssert.assertThat(result.data.latitude, CoreMatchers.`is`(CoreMatchers.notNullValue()))
        MatcherAssert.assertThat(result.data.longitude, CoreMatchers.`is`(CoreMatchers.notNullValue()))
    }


    @Test
    fun getRemainders_isReminderListEmpty_false() = runBlocking {
        remainderRepository.saveReminder(
            ReminderDTO(
                "Test1", "test1", "test1", 0.0, 0.0, "test1"
            )
        )
        remainderRepository.getReminders()
        val list = remainderRepository.getReminders() as Result.Success<List<ReminderDTO>>
        MatcherAssert.assertThat(list.data.isNotEmpty(), CoreMatchers.`is`(true))
    }

    @Test
    fun getReminder_isReminderListEmpty_false() = runBlocking {
        remainderRepository.saveReminder(
            ReminderDTO(
                "Test1", "test1", "test1", 0.0, 0.0, "test1"
            )
        )
        val list = remainderRepository.getReminder("test1")
        MatcherAssert.assertThat(list, CoreMatchers.`is`(CoreMatchers.notNullValue()))
    }

    @Test
    fun deleteReminders_isReminderListEmpty_false() = runBlocking {
        remainderRepository.saveReminder(
            ReminderDTO(
                "Test1", "test1", "test1", 0.0, 0.0, "test1"
            )
        )
        val list = remainderRepository.getReminders() as Result.Success<List<ReminderDTO>>
        MatcherAssert.assertThat(list.data.isNotEmpty(), CoreMatchers.`is`(true))
        remainderRepository.deleteAllReminders()
        val savedList = remainderRepository.getReminders() as Result.Success<List<ReminderDTO>>
        MatcherAssert.assertThat(savedList.data.isEmpty(), CoreMatchers.`is`(true))
    }

    @Test
    fun reminderNotFound_isReminderStatusCodeNull_true() = runBlocking {
        remainderRepository.saveReminder(
            ReminderDTO(
                "Test1", "test1", "test1", 0.0, 0.0, "test1"
            )
        )
        val reminder = remainderRepository.getReminder("unknown id") as Result.Error
        Assert.assertNotNull(reminder)
        Assert.assertEquals("Reminder not found!", reminder.message)
        Assert.assertNull(reminder.statusCode)
    }

}