package com.example.itemlocalization.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val address: String
)
