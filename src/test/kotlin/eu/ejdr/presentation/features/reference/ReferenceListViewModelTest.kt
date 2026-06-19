package eu.ejdr.presentation.features.reference

import eu.ejdr.application.features.reference.abstraction.usecase.CreateReferenceItemUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.DeleteReferenceItemUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.ListReferenceItemsUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.UpdateReferenceItemUseCase
import eu.ejdr.application.shared.Result
import eu.ejdr.domain.features.reference.entities.ReferenceItem
import eu.ejdr.domain.features.reference.entities.ReferenceType
import eu.ejdr.domain.features.reference.error.ReferenceError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReferenceListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun item(id: String) = ReferenceItem(id, "N-$id", "2026-06-13T10:00:00.000Z")

    /** Use cases « no-op » par défaut, surchargés par test au besoin pour rester DRY. */
    private fun viewModel(
        type: ReferenceType = ReferenceType.ARME,
        activeGroupId: StateFlow<String?> = MutableStateFlow("g-1"),
        listItems: ListReferenceItemsUseCase = ListReferenceItemsUseCase { _, _ -> Result.Success(emptyList()) },
        createItem: CreateReferenceItemUseCase =
            CreateReferenceItemUseCase { _, _, _, _, _, _, _ -> Result.Success(item("a")) },
        updateItem: UpdateReferenceItemUseCase =
            UpdateReferenceItemUseCase { _, _, _, _, _, _, _, _ -> Result.Success(item("a")) },
        deleteItem: DeleteReferenceItemUseCase = DeleteReferenceItemUseCase { _, _ -> Result.Success(Unit) },
    ) = ReferenceListViewModel(type, activeGroupId, listItems, createItem, updateItem, deleteItem)

    @Test
    fun `loads the items of the given type and active group at init`() = runTest {
        var requestedType: ReferenceType? = null
        var requestedGroup: String? = null
        val vm = viewModel(
            type = ReferenceType.ARME,
            activeGroupId = MutableStateFlow("g-1"),
            listItems = ListReferenceItemsUseCase { type, groupId ->
                requestedType = type
                requestedGroup = groupId
                Result.Success(listOf(item("a")))
            },
        )
        advanceUntilIdle()

        assertEquals(ReferenceType.ARME, requestedType)
        assertEquals("g-1", requestedGroup)
        assertEquals(1, vm.items.value.size)
        assertNull(vm.error.value)
        assertEquals(false, vm.needsGroup.value)
    }

    @Test
    fun `no active group flags onboarding and does not call the use case`() = runTest {
        var called = false
        val vm = viewModel(
            type = ReferenceType.ARME,
            activeGroupId = MutableStateFlow(null),
            listItems = ListReferenceItemsUseCase { _, _ -> called = true; Result.Success(emptyList()) },
        )
        advanceUntilIdle()

        assertTrue(vm.needsGroup.value)
        assertTrue(vm.items.value.isEmpty())
        assertEquals(false, called)
    }

    @Test
    fun `reloads when the active group changes`() = runTest {
        val active = MutableStateFlow<String?>("g-1")
        val vm = viewModel(
            type = ReferenceType.ARME,
            activeGroupId = active,
            listItems = ListReferenceItemsUseCase { _, groupId -> Result.Success(listOf(item(groupId))) },
        )
        advanceUntilIdle()
        assertEquals("g-1", vm.items.value.first().id)

        active.value = "g-2"
        advanceUntilIdle()

        assertEquals("g-2", vm.items.value.first().id)
    }

    @Test
    fun `create uses the active group and reloads`() = runTest {
        var createdGroup: String? = null
        var stored = emptyList<ReferenceItem>()
        val vm = viewModel(
            type = ReferenceType.FORMATION,
            activeGroupId = MutableStateFlow("g-1"),
            listItems = ListReferenceItemsUseCase { _, _ -> Result.Success(stored) },
            createItem = CreateReferenceItemUseCase { _, _, groupId, _, _, _, _ ->
                createdGroup = groupId
                stored = listOf(item("f"))
                Result.Success(item("f"))
            },
        )
        advanceUntilIdle()

        vm.create("Rôdeur")
        advanceUntilIdle()

        assertEquals("g-1", createdGroup)
        assertEquals(1, vm.items.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `create failure exposes the error`() = runTest {
        val vm = viewModel(
            type = ReferenceType.FORMATION,
            activeGroupId = MutableStateFlow("g-1"),
            createItem = CreateReferenceItemUseCase { _, _, _, _, _, _, _ -> Result.Failure(ReferenceError.NameAlreadyUsed) },
        )
        advanceUntilIdle()

        vm.create("Doublon")
        advanceUntilIdle()

        assertEquals(ReferenceError.NameAlreadyUsed.message, vm.error.value)
    }

    @Test
    fun `delete success reloads`() = runTest {
        var stored = listOf(item("a"))
        val vm = viewModel(
            type = ReferenceType.ARME,
            activeGroupId = MutableStateFlow("g-1"),
            listItems = ListReferenceItemsUseCase { _, _ -> Result.Success(stored) },
            deleteItem = DeleteReferenceItemUseCase { _, _ ->
                stored = emptyList()
                Result.Success(Unit)
            },
        )
        advanceUntilIdle()

        vm.delete("a")
        advanceUntilIdle()

        assertEquals(0, vm.items.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `create formation forwards stat bonus and competence ids and reloads`() = runTest {
        var capturedStat: String? = "untouched"
        var capturedBonus: Int? = -999
        var capturedCompetences: List<String>? = null
        var stored = emptyList<ReferenceItem>()
        val vm = viewModel(
            type = ReferenceType.FORMATION,
            activeGroupId = MutableStateFlow("g-1"),
            listItems = ListReferenceItemsUseCase { _, _ -> Result.Success(stored) },
            createItem = CreateReferenceItemUseCase { _, _, _, stat, bonus, competenceIds, _ ->
                capturedStat = stat
                capturedBonus = bonus
                capturedCompetences = competenceIds
                stored = listOf(item("f"))
                Result.Success(item("f"))
            },
        )
        advanceUntilIdle()

        vm.create("Rôdeur", stat = "dexterite", bonus = 3, competenceIds = listOf("c-1", "c-2"))
        advanceUntilIdle()

        assertEquals("dexterite", capturedStat)
        assertEquals(3, capturedBonus)
        assertEquals(listOf("c-1", "c-2"), capturedCompetences)
        assertEquals(1, vm.items.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `create armure forwards protection points and reloads`() = runTest {
        var capturedProtection: Int? = -999
        var stored = emptyList<ReferenceItem>()
        val vm = viewModel(
            type = ReferenceType.ARMURE,
            activeGroupId = MutableStateFlow("g-1"),
            listItems = ListReferenceItemsUseCase { _, _ -> Result.Success(stored) },
            createItem = CreateReferenceItemUseCase { _, _, _, _, _, _, protectionPoints ->
                capturedProtection = protectionPoints
                stored = listOf(item("a"))
                Result.Success(item("a"))
            },
        )
        advanceUntilIdle()

        vm.create("Cotte de mailles", protectionPoints = 5)
        advanceUntilIdle()

        assertEquals(5, capturedProtection)
        assertEquals(1, vm.items.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `create simple type forwards null stat null bonus empty competences and null protection`() = runTest {
        var capturedStat: String? = "untouched"
        var capturedBonus: Int? = -999
        var capturedCompetences: List<String>? = listOf("dirty")
        var capturedProtection: Int? = -999
        val vm = viewModel(
            type = ReferenceType.ARME,
            activeGroupId = MutableStateFlow("g-1"),
            createItem = CreateReferenceItemUseCase { _, _, _, stat, bonus, competenceIds, protectionPoints ->
                capturedStat = stat
                capturedBonus = bonus
                capturedCompetences = competenceIds
                capturedProtection = protectionPoints
                Result.Success(item("a"))
            },
        )
        advanceUntilIdle()

        vm.create("Épée")
        advanceUntilIdle()

        assertNull(capturedStat)
        assertNull(capturedBonus)
        assertEquals(emptyList(), capturedCompetences)
        assertNull(capturedProtection)
    }

    @Test
    fun `update forwards item id name and fields to the use case and reloads`() = runTest {
        var capturedId: String? = null
        var capturedName: String? = null
        var capturedGroup: String? = null
        var capturedStat: String? = "untouched"
        var capturedBonus: Int? = -999
        var capturedCompetences: List<String>? = null
        var capturedProtection: Int? = -999
        var stored = listOf(item("f"))
        val vm = viewModel(
            type = ReferenceType.FORMATION,
            activeGroupId = MutableStateFlow("g-1"),
            listItems = ListReferenceItemsUseCase { _, _ -> Result.Success(stored) },
            updateItem = UpdateReferenceItemUseCase { _, itemId, name, groupId, stat, bonus, competenceIds, protectionPoints ->
                capturedId = itemId
                capturedName = name
                capturedGroup = groupId
                capturedStat = stat
                capturedBonus = bonus
                capturedCompetences = competenceIds
                capturedProtection = protectionPoints
                stored = listOf(item("g"))
                Result.Success(item("g"))
            },
        )
        advanceUntilIdle()

        vm.update("f", "Rôdeur+", stat = "vigueur", bonus = 4, competenceIds = listOf("c-1"))
        advanceUntilIdle()

        assertEquals("f", capturedId)
        assertEquals("Rôdeur+", capturedName)
        assertEquals("g-1", capturedGroup)
        assertEquals("vigueur", capturedStat)
        assertEquals(4, capturedBonus)
        assertEquals(listOf("c-1"), capturedCompetences)
        assertNull(capturedProtection)
        assertEquals("g", vm.items.value.first().id)
        assertNull(vm.error.value)
    }

    @Test
    fun `update failure exposes the error and does not reload`() = runTest {
        var listed = 0
        val vm = viewModel(
            type = ReferenceType.ARME,
            activeGroupId = MutableStateFlow("g-1"),
            listItems = ListReferenceItemsUseCase { _, _ -> listed++; Result.Success(emptyList()) },
            updateItem = UpdateReferenceItemUseCase { _, _, _, _, _, _, _, _ -> Result.Failure(ReferenceError.NameAlreadyUsed) },
        )
        advanceUntilIdle()
        val listedAfterInit = listed

        vm.update("a", "Doublon")
        advanceUntilIdle()

        assertEquals(ReferenceError.NameAlreadyUsed.message, vm.error.value)
        assertEquals(listedAfterInit, listed)
    }

    @Test
    fun `loads available competences when type is formation`() = runTest {
        val requestedTypes = mutableListOf<ReferenceType>()
        val vm = viewModel(
            type = ReferenceType.FORMATION,
            activeGroupId = MutableStateFlow("g-1"),
            listItems = ListReferenceItemsUseCase { type, _ ->
                requestedTypes += type
                if (type == ReferenceType.COMPETENCE) {
                    Result.Success(listOf(item("c")))
                } else {
                    Result.Success(emptyList())
                }
            },
        )
        advanceUntilIdle()

        assertTrue(requestedTypes.contains(ReferenceType.COMPETENCE))
        assertEquals(1, vm.availableCompetences.value.size)
        assertEquals("c", vm.availableCompetences.value.first().id)
    }

    @Test
    fun `exposes competence names index for formation`() = runTest {
        val vm = viewModel(
            type = ReferenceType.FORMATION,
            activeGroupId = MutableStateFlow("g-1"),
            listItems = ListReferenceItemsUseCase { type, _ ->
                if (type == ReferenceType.COMPETENCE) {
                    Result.Success(listOf(ReferenceItem("c-1", "Esquive", "2026-06-13T10:00:00.000Z")))
                } else {
                    Result.Success(emptyList())
                }
            },
        )
        advanceUntilIdle()

        assertEquals(mapOf("c-1" to "Esquive"), vm.competenceNames.value)
    }

    @Test
    fun `does not load available competences for simple types`() = runTest {
        val requestedTypes = mutableListOf<ReferenceType>()
        val vm = viewModel(
            type = ReferenceType.ARME,
            activeGroupId = MutableStateFlow("g-1"),
            listItems = ListReferenceItemsUseCase { type, _ ->
                requestedTypes += type
                Result.Success(emptyList())
            },
        )
        advanceUntilIdle()

        assertEquals(false, requestedTypes.contains(ReferenceType.COMPETENCE))
        assertTrue(vm.availableCompetences.value.isEmpty())
        assertTrue(vm.competenceNames.value.isEmpty())
    }
}
