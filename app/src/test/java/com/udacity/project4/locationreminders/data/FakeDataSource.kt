package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource : ReminderDataSource {
    var returnError = false
    val listReminder: ArrayList<ReminderDTO> = arrayListOf()

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
        try {
            if(returnError){
                throw Exception("Test exception")
            }
            var reminder: ReminderDTO? = null
            with(listReminder.filter { it.id == id }){
                if(isNotEmpty()){
                    reminder = this[0]
                }
            }
            return if (reminder == null) {
                Result.Error("Error retrieving reminder")
            } else {
                return Result.Success(reminder!!)
            }
        } catch (except: Exception){
            return Result.Error(except.localizedMessage)
        }
    }

    override suspend fun deleteAllReminders() {
        listReminder.clear()
    }
}