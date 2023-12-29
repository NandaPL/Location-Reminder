package com.udacity.project4.locationreminders.data.local

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

class FakeReminderDataSource: ReminderDataSource {
    var reminderList: MutableList<ReminderDTO> = mutableListOf()
    var returnError = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (returnError) {
            return Result.Error("Error occurred..")
        } else {
            reminderList.let {
                return Result.Success(it.toList())
            }
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminderList.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        try {
            if (returnError)
                throw Exception("Exception in Test")
            var reminder: ReminderDTO? = null
            with(reminderList.filter { it.id == id }) {
                if (isNotEmpty()){
                    reminder = this[0]
                }
            }
            return if (reminder != null) {
                Result.Success(reminder!!)
            } else {
                Result.Error("Reminder not found")
            }
        } catch (except: Exception){
            return Result.Error(except.localizedMessage)
        }
    }

    override suspend fun deleteAllReminders() {
        reminderList.clear()
    }
}