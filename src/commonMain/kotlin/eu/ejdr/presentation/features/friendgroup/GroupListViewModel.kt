package eu.ejdr.presentation.features.friendgroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.ejdr.application.features.friendgroup.abstraction.usecase.CreateGroupUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.DeleteGroupUseCase
import eu.ejdr.application.features.friendgroup.abstraction.usecase.ListMyGroupsUseCase
import eu.ejdr.application.shared.feedback.UiMessageBus
import eu.ejdr.application.shared.fold
import eu.ejdr.domain.features.friendgroup.entities.FriendGroup
import eu.ejdr.application.shared.feedback.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupListViewModel(
    private val listMyGroups: ListMyGroupsUseCase,
    private val createGroup: CreateGroupUseCase,
    private val deleteGroup: DeleteGroupUseCase,
    private val uiMessageBus: UiMessageBus,
) : ViewModel() {

    private val _groups = MutableStateFlow<List<FriendGroup>>(emptyList())
    val groups: StateFlow<List<FriendGroup>> = _groups.asStateFlow()

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
            listMyGroups().fold(
                onSuccess = { list -> _groups.value = list; _error.value = null },
                onFailure = { err -> _error.value = err.message },
            )
            _isLoading.value = false
        }
    }

    fun create(name: String) {
        viewModelScope.launch {
            createGroup(name).fold(
                onSuccess = {
                    _error.value = null
                    uiMessageBus.emit(UiMessage.success("Groupe créé"))
                    load()
                },
                onFailure = { err ->
                    _error.value = err.message
                    uiMessageBus.emit(UiMessage.error(err.message))
                },
            )
        }
    }

    fun delete(groupId: String) {
        viewModelScope.launch {
            deleteGroup(groupId).fold(
                onSuccess = {
                    _error.value = null
                    uiMessageBus.emit(UiMessage.success("Groupe supprimé"))
                    load()
                },
                onFailure = { err ->
                    _error.value = err.message
                    uiMessageBus.emit(UiMessage.error(err.message))
                },
            )
        }
    }
}
