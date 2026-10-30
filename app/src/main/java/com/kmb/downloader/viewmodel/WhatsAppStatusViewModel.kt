package com.kmb.downloader.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kmb.downloader.data.WhatsAppStatusItem
import com.kmb.downloader.whatsapp.WhatsAppStatusHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WhatsAppStatusViewModel(application: Application) : AndroidViewModel(application) {

    private val _isBusiness = MutableStateFlow(false)
    val isBusiness: StateFlow<Boolean> = _isBusiness.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: All, 1: Photos, 2: Videos
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _statuses = MutableStateFlow<List<WhatsAppStatusItem>>(emptyList())
    val statuses: StateFlow<List<WhatsAppStatusItem>> = _statuses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _isAppInstalled = MutableStateFlow(true)
    val isAppInstalled: StateFlow<Boolean> = _isAppInstalled.asStateFlow()

    private val _previewItem = MutableStateFlow<WhatsAppStatusItem?>(null)
    val previewItem: StateFlow<WhatsAppStatusItem?> = _previewItem.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        checkAndLoad(getApplication())
    }

    fun setBusiness(isBiz: Boolean, context: Context) {
        if (_isBusiness.value != isBiz) {
            _isBusiness.value = isBiz
            checkAndLoad(context)
        }
    }

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setPreview(item: WhatsAppStatusItem?) {
        _previewItem.value = item
    }

    fun clearMessage() {
        _message.value = null
    }

    fun checkAndLoad(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val biz = _isBusiness.value
            val installed = WhatsAppStatusHelper.isAppInstalled(context, biz)
            _isAppInstalled.value = installed

            val perm = WhatsAppStatusHelper.hasFolderPermission(context, biz)
            _hasPermission.value = perm

            if (perm) {
                loadStatuses(context)
            } else {
                _statuses.value = emptyList()
            }
        }
    }

    fun onFolderUriSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if not supported
            }
            WhatsAppStatusHelper.saveFolderUri(context, uri, _isBusiness.value)
            _hasPermission.value = true
            loadStatuses(context)
        }
    }

    fun loadStatuses(context: Context) {
        viewModelScope.launch {
            // Only show full loader if there are no items cached yet
            if (_statuses.value.isEmpty()) {
                _isLoading.value = true
            }
            val biz = _isBusiness.value
            val list = withContext(Dispatchers.IO) {
                WhatsAppStatusHelper.fetchStatuses(context, biz)
            }
            _statuses.value = list
            _isLoading.value = false
        }
    }

    fun saveStatus(context: Context, item: WhatsAppStatusItem) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                WhatsAppStatusHelper.saveStatusToGallery(context, item)
            }
            if (success) {
                // Show only the single official Toast notification (removed duplicate bottom snackbar message)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "✓ Saved to Gallery (Downloads/KMB Statuses)!",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                // Mark as saved in local list
                _statuses.value = _statuses.value.map {
                    if (it.uri == item.uri) it.copy(isSaved = true) else it
                }
            } else {
                _message.value = "Failed to save status"
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "Failed to save status",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun resetPermission(context: Context) {
        WhatsAppStatusHelper.clearSavedFolderUri(context, _isBusiness.value)
        _hasPermission.value = false
        _statuses.value = emptyList()
    }

    fun shareStatus(context: Context, item: WhatsAppStatusItem) {
        WhatsAppStatusHelper.shareStatus(context, item)
    }
}
