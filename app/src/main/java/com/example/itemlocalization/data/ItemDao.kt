package com.example.itemlocalization.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ItemDao {
    @Query("SELECT * FROM item")
    fun getAllItems(): List<Item>

    @Insert
    fun insertItem(item: Item)

    @Delete
    fun deleteItem(item: Item)

    @Update
    fun updateItem(item: Item)

}