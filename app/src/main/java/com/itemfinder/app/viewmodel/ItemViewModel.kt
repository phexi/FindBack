package com.itemfinder.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.itemfinder.app.ItemFinderApp
import com.itemfinder.app.data.ItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import com.itemfinder.app.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ItemViewModel(application: Application) : AndroidViewModel(application) {

    private val itemDao = (application as ItemFinderApp).database.itemDao()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val items: StateFlow<List<ItemEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                itemDao.getAllItems()
            } else {
                itemDao.searchItems("%" + query + "%")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _navigateToHome = MutableStateFlow(false)
    val navigateToHome: StateFlow<Boolean> = _navigateToHome.asStateFlow()

    fun onNavigatedToHome() {
        _navigateToHome.value = false
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun saveItem(
        name: String,
        location: String,
        itemPhotoPath: String,
        locationPhotoPaths: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (name.isBlank() || itemPhotoPath.isBlank() || locationPhotoPaths.isEmpty()) {
            Log.e("ItemViewModel", "saveItem validation failed: name=$name itemPhotoPath=$itemPhotoPath locationPhotoPaths=${locationPhotoPaths.size}")
            onError(getApplication<Application>().getString(R.string.save_failed, "Missing required fields"))
            return
        }
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val photosDir = File(context.filesDir, "photos")
                if (!photosDir.exists()) photosDir.mkdirs()

                val uuid = UUID.randomUUID().toString()
                val destItemPhoto = File(photosDir, uuid + "_item.jpg")

                withContext(Dispatchers.IO) {
                    copyFile(File(itemPhotoPath), destItemPhoto)

                    val destLocationPhotos = locationPhotoPaths.mapIndexed { idx, srcPath ->
                        val dest = File(photosDir, uuid + "_location_" + idx + ".jpg")
                        copyFile(File(srcPath), dest)
                        dest.absolutePath
                    }

                    val entity = ItemEntity(
                        name = name.trim(),
                        location = location.trim(),
                        itemPhotoPath = destItemPhoto.absolutePath,
                        locationPhotoPaths = destLocationPhotos
                    )
                    itemDao.insertItem(entity)
                }

                onSuccess()
                _navigateToHome.value = true
            } catch (e: Exception) {
                Log.e("ItemViewModel", "saveItem error", e)
                onError(getApplication<Application>().getString(R.string.save_failed, e.localizedMessage ?: "Unknown error"))
            }
        }
    }

    fun deleteItem(item: ItemEntity) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    File(item.itemPhotoPath).delete()
                    item.locationPhotoPaths.forEach { File(it).delete() }
                    itemDao.deleteItem(item)
                }
            } catch (_: Exception) { }
        }
    }

    private fun copyFile(src: File, dest: File) {
        FileInputStream(src).use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
        }
    }
}