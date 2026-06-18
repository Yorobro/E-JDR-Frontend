package eu.ejdr.presentation.features.friendgroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.friendgroup.abstraction.usecase.AcceptInvitationUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.DeclineInvitationUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.ListMyInvitationsUseCase
import eu.ejdr.application.shared.fold
import eu.ejdr.domain.features.friendgroup.entities.GroupInvitation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InvitationListViewModel(
    private val listMyInvitations: ListMyInvitationsUseCase,
    private val acceptInvitation: AcceptInvitationUseCase,
    private val declineInvitation: DeclineInvitationUseCase,
) : ViewModel() {

    private val _invitations = MutableStateFlow<List<GroupInvitation>>(emptyList())
    val invitations: StateFlow<List<GroupInvitation>> = _invitations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            listMyInvitations().fold(
                onSuccess = { list -> _invitations.value = list; _error.value = null },
                onFailure = { err -> _error.value = err.message },
            )
            _isLoading.value = false
        }
    }

    fun accept(invitationId: String) {
        viewModelScope.launch {
            acceptInvitation(invitationId).fold(
                onSuccess = { _error.value = null; load() },
                onFailure = { err -> _error.value = err.message },
            )
        }
    }

    fun decline(invitationId: String) {
        viewModelScope.launch {
            declineInvitation(invitationId).fold(
                onSuccess = { _error.value = null; load() },
                onFailure = { err -> _error.value = err.message },
            )
        }
    }
}
