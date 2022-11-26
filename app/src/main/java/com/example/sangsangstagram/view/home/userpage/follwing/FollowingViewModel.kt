package com.example.sangsangstagram.view.home.userpage.follwing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import com.example.sangsangstagram.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FollowingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FollowingUiState())
    val uiState = _uiState.asStateFlow()

    private var didBind = false

    fun bind(userUuid: String, type: UserListPageType) {
        check(!didBind)
        didBind = true
        viewModelScope.launch {
            val pagingFlow = when (type) {
                UserListPageType.FOLLOWING -> UserRepository.getFollowingUsersPaging(userUuid)
                UserListPageType.FOLLOWER -> UserRepository.getFollowersPaging(userUuid)
            }
            pagingFlow.cachedIn(viewModelScope)
                .collectLatest { pagingData ->
                    _uiState.update { uiState ->
                        uiState.copy(pagingData = pagingData.map { it.toUiState() })
                    }
                }
        }
    }
}
