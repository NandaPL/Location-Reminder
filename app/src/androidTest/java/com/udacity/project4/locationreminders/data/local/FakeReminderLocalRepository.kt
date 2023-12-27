package com.udacity.project4.locationreminders.data.local

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

class FakeReminderLocalRepository: ReminderDataSource {
    var reminders: MutableList<ReminderDTO> = mutableListOf()

    private var shouldReturnError = false

    fun setShouldReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Result.Error("Error occurred..")
        } else {
            reminders.let {
                return Result.Success(ArrayList(it))
            }
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        return if (shouldReturnError) {
            Result.Error("Error occurred..")
        } else {
            val reminder = reminders.find { it.id == id }
            if (reminder != null)
                Result.Success(reminder)
            else
                Result.Error("Reminder not found")
        }
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }
}