package com.orlinskas.videofacefinder.data.repository

import com.orlinskas.videofacefinder.data.AppDatabase
import com.orlinskas.videofacefinder.data.model.Person
import javax.inject.Inject

class PersonsRepository @Inject constructor(
        appDatabase: AppDatabase
) {

    private val personsDao = appDatabase.personDao()

    fun insertPersons(persons: List<Person>) {
        personsDao.removeAll()
        personsDao.insert(persons)
    }

    fun updatePerson(person: Person) {
        personsDao.update(person)
    }

    fun getAllPersons(): List<Person> {
        return personsDao.getAll()
    }

    fun removeAllPersons() {
        return personsDao.removeAll()
    }
}