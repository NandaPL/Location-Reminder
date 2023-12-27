package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(val listReminder: MutableList<ReminderDTO> = mutableListOf()) : ReminderDataSource {

    private var returnError = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return if (returnError) {
            Result.Error("Error retrieving reminders")
        } else {
            Result.Success(listReminder)
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        listReminder.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        return if (!returnError) {
            val reminder = listReminder.find {
                it.id == id
            }
            if (reminder != null) {
                Result.Success(reminder)
            } else {
                Result.Error("Reminder not found!")
            }

        } else {
            Result.Error("Error retrieving reminder")
        }
    }

    override suspend fun deleteAllReminders() {
        listReminder.clear()
    }

    fun setReturnError(shouldReturnError: Boolean) {
        this.returnError = shouldReturnError
    }
}