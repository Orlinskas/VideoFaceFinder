package com.orlinskas.videofacefinder.data.dao

import androidx.room.*
import com.orlinskas.videofacefinder.data.Tables
import com.orlinskas.videofacefinder.data.model.Person

@Dao
interface PersonsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(persons: List<Person>)

    @Update
    fun update(person: Person)

    @Query("SELECT * FROM ${Tables.PERSON}")
    fun getAll(): List<Person>

    @Query("DELETE FROM ${Tables.PERSON}")
    fun removeAll()
}