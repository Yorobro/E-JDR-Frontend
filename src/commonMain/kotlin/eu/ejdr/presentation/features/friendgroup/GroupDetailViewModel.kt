package eu.ejdr.presentation.features.friendgroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.friendgroup.abstraction.usecase.ChangeMemberRoleUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.GetGroupUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.InviteMemberUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.RemoveMemberUseCase
import eu.ejdr.application.shared.fold
import eu.ejdr.domain.features.friendgroup.entities.FriendGroupDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupDetailViewModel(
    private val groupId: String,
    private val getGroup: GetGroupUseCase,
    private val inviteMember: InviteMemberUseCase,
    private val removeMember: RemoveMemberUseCase,
    private val changeMemberRole: ChangeMemberRoleUseCase,
) : ViewModel() {

    private val _detail = MutableStateFlow<FriendGroupDetail?>(null)
    val detail: StateFlow<FriendGroupDetail?> = _detail.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _inviteSuccess = MutableStateFlow(false)
    val inviteSuccess: StateFlow<Boolean> = _inviteSuccess.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            getGroup(groupId).fold(
                onSuccess = { d -> _detail.value = d; _error.value = null },
                onFailure = { err -> _error.value = err.message },
            )
            _isLoading.value = false
        }
    }

    fun invite(email: String) {
        viewModelScope.launch {
            inviteMember(groupId, email).fold(
                onSuccess = { _inviteSuccess.value = true; _error.value = null },
                onFailure = { err -> _error.value = err.message },
            )
        }
    }

    fun clearInviteSuccess() {
        _inviteSuccess.value = false
    }

    fun removeMember(userId: String) {
        viewModelScope.launch {
            removeMember(groupId, userId).fold(
                onSuccess = { _error.value = null; load() },
                onFailure = { err -> _error.value = err.message },
            )
        }
    }

    fun changeRole(userId: String, role: String) {
        viewModelScope.launch {
            changeMemberRole(groupId, userId, role).fold(
                onSuccess = { _error.value = null; load() },
                onFailure = { err -> _error.value = err.message },
            )
        }
    }
}
