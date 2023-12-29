package com.udacity.project4.locationreminders.data.local

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

class FakeReminderLocalRepository: ReminderDataSource {
    var reminderList: MutableList<ReminderDTO> = mutableListOf()
    var shouldReturnError = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Result.Error("Error occurred..")
        } else {
            reminderList.let {
                return Result.Success(ArrayList(it))
            }
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminderList.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        return if (shouldReturnError) {
            Result.Error("Error occurred..")
        } else {
            val reminder = reminderList.find { it.id == id }
            if (reminder != null)
                Result.Success(reminder)
            else
                Result.Error("Reminder not found")
        }
    }

    override suspend fun deleteAllReminders() {
        reminderList.clear()
    }
}