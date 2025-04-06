package com.example.hectoclashapp.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.hectoclash.data.models.*
import com.example.hectoclash.data.network.GenericResponse
import com.example.hectoclash.data.repository.FriendRepository
import com.example.hectoclash.utils.SocketManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.HttpException

class FriendsListViewModel(application: Application) : AndroidViewModel(application) {

    private val friendRepository = FriendRepository.getInstance(application)

    private val _friendsList = MutableStateFlow<List<FriendListItem>>(emptyList())
    val friendsList: StateFlow<List<FriendListItem>> = _friendsList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Feedback for actions like accept/reject/remove
    private val _actionFeedback = MutableStateFlow<String?>(null)
    val actionFeedback: StateFlow<String?> = _actionFeedback.asStateFlow()

    // Track online status separately, updated by SocketManager
    private val _onlineUserIds = MutableStateFlow<Set<String>>(emptySet())

    private var socketListenerJob: Job? = null

    init {
        fetchFriendsList()
        startListeningToSocketEvents()
    }

    fun fetchFriendsList() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            friendRepository.getFriendsList().fold(
                onSuccess = { updateFriendsList(it) }, // Use helper to merge with online status
                onFailure = { handleApiError("Failed to fetch friends", it) }
            )
            _isLoading.value = false
        }
    }

    // Accept friend request from the list
    fun acceptRequest(userId: String) = performFriendAction(userId, "Accepting...") {
        friendRepository.acceptFriendRequest(userId)
    }

    // Reject friend request or remove accepted friend
    fun removeOrRejectFriend(userId: String, isPending: Boolean) = performFriendAction(userId, if(isPending) "Rejecting..." else "Removing...") {
        friendRepository.removeFriend(userId)
    }

    private fun performFriendAction(userId: String, loadingMessage: String, action: suspend () -> Result<GenericResponse>) {
        viewModelScope.launch {
            // Optional: Indicate loading specifically for this item?
            _actionFeedback.value = loadingMessage

            action().fold(
                onSuccess = {
                    _actionFeedback.value = it.message
                    // Refresh the entire list for simplicity, or update locally
                    fetchFriendsList()
                },
                onFailure = { handleApiError("Action failed for $userId", it) }
            )
            // Optional: Clear feedback after delay
            kotlinx.coroutines.delay(3000)
            if (_actionFeedback.value?.startsWith(loadingMessage.substringBefore("...")) == true) {
                _actionFeedback.value = null
            }
        }
    }

    private fun startListeningToSocketEvents() {
        if (socketListenerJob?.isActive == true) return
        socketListenerJob = viewModelScope.launch {

            // Listen to general online user updates to update isOnline status
            launch {
                SocketManager.onlineUsersFlow
                    .catch { e -> Log.e("FriendsListVM", "Error onlineUsersFlow: ${e.message}") }
                    .collect { onlineUsers ->
                        val currentOnlineIds = onlineUsers.map { it._id }.toSet()
                        _onlineUserIds.value = currentOnlineIds
                        // Trigger list update to reflect online status changes
                        updateFriendsList(_friendsList.value) // Re-apply online status
                    }
            }

            // Listen for events that change friendship status and refresh list
            launch {
                merge(
                    SocketManager.friendRequestAcceptedFlow,
                    SocketManager.friendRequestRejectedFlow,
                    SocketManager.friendRemovedFlow
                    // Don't need friendRequestReceivedFlow here, handled elsewhere maybe
                )
                    .catch { e -> Log.e("FriendsListVM", "Error in friend status flows: ${e.message}") }
                    .collect {
                        Log.d("FriendsListVM", "Friend status changed via socket, refreshing list.")
                        // Optionally show a message based on event type before refresh
                        when(it) {
                            is FriendRequestAcceptedData -> _actionFeedback.value = "${it.acceptorName} accepted your request!"
                            is FriendRequestRejectedData -> _actionFeedback.value = "${it.rejectorName} rejected your request."
                            is FriendRemovedData -> _actionFeedback.value = "${it.removerName} removed you as a friend."
                        }
                        fetchFriendsList() // Refresh list on any status change
                        kotlinx.coroutines.delay(3500) // Clear feedback
                        _actionFeedback.value = null
                    }
            }
        }
    }

    // Helper to update the list and merge with current online status
    private fun updateFriendsList(newList: List<FriendListItem>) {
        val onlineIds = _onlineUserIds.value
        _friendsList.value = newList.map { friend ->
            friend.copy(isOnline = onlineIds.contains(friend._id))
        }
    }


    private fun handleApiError(defaultMessage: String, throwable: Throwable) {
        val errorMsg = if (throwable is HttpException) {
            try {
                val errorJson = throwable.response()?.errorBody()?.string()
                errorJson?.let { org.json.JSONObject(it).getString("message") } ?: throwable.message()
            } catch (e: Exception) { throwable.message() ?: defaultMessage }
        } else { throwable.message ?: defaultMessage }
        _error.value = errorMsg
        Log.e("FriendsListViewModel", "$defaultMessage: $errorMsg", throwable)
    }

    fun clearError() { _error.value = null }
    fun clearActionFeedback() { _actionFeedback.value = null }

    override fun onCleared() {
        super.onCleared()
        socketListenerJob?.cancel()
    }
}

// Factory (optional, if not using Hilt)
class FriendsListViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FriendsListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FriendsListViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}