package com.example.itemlocalization.data

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch

class ItemViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).itemDao()
    private val repository = ItemRepository(dao)

    private val _items = MutableLiveData<List<Item>>()
    val items: LiveData<List<Item>> = _items

    init {
        loadItems()
    }

    private fun loadItems() {
        viewModelScope.launch {
            repository.getAllItems().collect {
                _items.value = it
            }
        }
    }

    fun addItem(item: Item) {
        viewModelScope.launch {
            repository.insert(item)
            loadItems()
        }
    }

    fun updateItem(item: Item) {
        viewModelScope.launch {
            repository.update(item)
            loadItems()
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            repository.delete(item)
            loadItems()
        }
    }
}
