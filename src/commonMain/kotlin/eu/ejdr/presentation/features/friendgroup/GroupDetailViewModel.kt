package eu.ejdr.presentation.features.friendgroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.auth.abstraction.usecase.GetCurrentUserUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.ChangeMemberRoleUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.GetGroupUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.InviteMemberUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.RemoveMemberUseCase
import eu.ejdr.application.features.realtime.abstraction.InvalidationBus
import eu.ejdr.application.features.realtime.abstraction.RealtimeSubscriptions
import eu.ejdr.application.shared.Result
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
    private val getCurrentUser: GetCurrentUserUseCase,
    private val invalidationBus: InvalidationBus,
    private val subscriptions: RealtimeSubscriptions,
) : ViewModel() {

    private val _detail = MutableStateFlow<FriendGroupDetail?>(null)
    val detail: StateFlow<FriendGroupDetail?> = _detail.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _inviteSuccess = MutableStateFlow(false)
    val inviteSuccess: StateFlow<Boolean> = _inviteSuccess.asStateFlow()

    /** Identifiant de l'utilisateur courant : sert à distinguer « ma carte » (quitter) des autres (retirer). */
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            when (val r = getCurrentUser()) {
                is Result.Success -> _currentUserId.value = r.value.id
                is Result.Failure -> Unit
            }
        }
        subscriptions.subscribe("group:$groupId")
        viewModelScope.launch {
            invalidationBus.events.collect { invalidation ->
                if (invalidation.resource == "group-members" && invalidation.scopeId == groupId) load()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        subscriptions.unsubscribe("group:$groupId")
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
