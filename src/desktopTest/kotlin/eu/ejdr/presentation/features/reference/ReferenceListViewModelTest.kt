package eu.ejdr.presentation.features.reference

import eu.ejdr.application.features.reference.abstraction.ReferenceItemForm
import eu.ejdr.domain.features.reference.entities.ReferenceStatBonus
import eu.ejdr.application.features.realtime.abstraction.RealtimeSubscriptions
import eu.ejdr.application.features.reference.abstraction.usecase.CreateReferenceItemUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.DeleteReferenceItemUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.ListReferenceItemsUseCase
import eu.ejdr.application.features.reference.abstraction.usecase.UpdateReferenceItemUseCase
import eu.ejdr.infrastructure.realtime.InMemoryInvalidationBus
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
            CreateReferenceItemUseCase { _, _, _ -> Result.Success(item("a")) },
        updateItem: UpdateReferenceItemUseCase =
            UpdateReferenceItemUseCase { _, _, _, _ -> Result.Success(item("a")) },
        deleteItem: DeleteReferenceItemUseCase = DeleteReferenceItemUseCase { _, _ -> Result.Success(Unit) },
    ) = ReferenceListViewModel(
        type, activeGroupId, listItems, createItem, updateItem, deleteItem,
        io.mockk.mockk(relaxed = true),
        InMemoryInvalidationBus(),
        object : RealtimeSubscriptions {
            override fun subscribe(channel: String) = Unit
            override fun unsubscribe(channel: String) = Unit
            override suspend fun resubscribeAll() = Unit
        },
    )

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
            createItem = CreateReferenceItemUseCase { _, groupId, _ ->
                createdGroup = groupId
                stored = listOf(item("f"))
                Result.Success(item("f"))
            },
        )
        advanceUntilIdle()

        vm.create(ReferenceItemForm("Rôdeur"))
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
            createItem = CreateReferenceItemUseCase { _, _, _ -> Result.Failure(ReferenceError.NameAlreadyUsed) },
        )
        advanceUntilIdle()

        vm.create(ReferenceItemForm("Doublon"))
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
        var capturedForm: ReferenceItemForm? = null
        var stored = emptyList<ReferenceItem>()
        val vm = viewModel(
            type = ReferenceType.FORMATION,
            activeGroupId = MutableStateFlow("g-1"),
            listItems = ListReferenceItemsUseCase { _, _ -> Result.Success(stored) },
            createItem = CreateReferenceItemUseCase { _, _, form ->
                capturedForm = form
                stored = listOf(item("f"))
                Result.Success(item("f"))
            },
        )
        advanceUntilIdle()

        vm.create(
            ReferenceItemForm(
                name = "Rôdeur",
                stat = "dexterite",
                bonus = 3,
                competenceIds = listOf("c-1", "c-2"),
            ),
        )
        advanceUntilIdle()

        assertEquals("dexterite", capturedForm?.stat)
        assertEquals(3, capturedForm?.bonus)
        assertEquals(listOf("c-1", "c-2"), capturedForm?.competenceIds)
        // Une formation reste mono-bonus : elle n'utilise jamais statBonuses.
        assertEquals(emptyList(), capturedForm?.statBonuses)
        assertEquals(1, vm.items.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `create peuple forwards the list of stat bonuses`() = runTest {
        var capturedForm: ReferenceItemForm? = null
        val vm = viewModel(
            type = ReferenceType.PEUPLE,
            activeGroupId = MutableStateFlow("g-1"),
            createItem = CreateReferenceItemUseCase { _, _, form ->
                capturedForm = form
                Result.Success(item("p"))
            },
        )
        advanceUntilIdle()

        vm.create(
            ReferenceItemForm(
                name = "Nain",
                statBonuses = listOf(
                    ReferenceStatBonus("vigueur", 2),
                    ReferenceStatBonus("social", 1),
                ),
            ),
        )
        advanceUntilIdle()

        assertEquals(
            listOf(ReferenceStatBonus("vigueur", 2), ReferenceStatBonus("social", 1)),
            capturedForm?.statBonuses,
        )
        // Un peuple n'utilise jamais le couple mono-bonus.
        assertNull(capturedForm?.stat)
        assertNull(capturedForm?.bonus)
    }

    @Test
    fun `create armure forwards protection points and reloads`() = runTest {
        var capturedForm: ReferenceItemForm? = null
        var stored = emptyList<ReferenceItem>()
        val vm = viewModel(
            type = ReferenceType.ARMURE,
            activeGroupId = MutableStateFlow("g-1"),
            listItems = ListReferenceItemsUseCase { _, _ -> Result.Success(stored) },
            createItem = CreateReferenceItemUseCase { _, _, form ->
                capturedForm = form
                stored = listOf(item("a"))
                Result.Success(item("a"))
            },
        )
        advanceUntilIdle()

        vm.create(ReferenceItemForm(name = "Cotte de mailles", protectionPoints = 5))
        advanceUntilIdle()

        assertEquals(5, capturedForm?.protectionPoints)
        assertEquals(1, vm.items.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `create simple type forwards null stat null bonus empty competences null protection and null description`() = runTest {
        var capturedForm: ReferenceItemForm? = null
        val vm = viewModel(
            type = ReferenceType.ARME,
            activeGroupId = MutableStateFlow("g-1"),
            createItem = CreateReferenceItemUseCase { _, _, form ->
                capturedForm = form
                Result.Success(item("a"))
            },
        )
        advanceUntilIdle()

        vm.create(ReferenceItemForm("Épée"))
        advanceUntilIdle()

        assertNull(capturedForm?.stat)
        assertNull(capturedForm?.bonus)
        assertEquals(emptyList(), capturedForm?.statBonuses)
        assertEquals(emptyList(), capturedForm?.competenceIds)
        assertNull(capturedForm?.protectionPoints)
        assertNull(capturedForm?.description)
    }

    @Test
    fun `create sort forwards description and reloads`() = runTest {
        var capturedForm: ReferenceItemForm? = null
        var stored = emptyList<ReferenceItem>()
        val vm = viewModel(
            type = ReferenceType.SORT,
            activeGroupId = MutableStateFlow("g-1"),
            listItems = ListReferenceItemsUseCase { _, _ -> Result.Success(stored) },
            createItem = CreateReferenceItemUseCase { _, _, form ->
                capturedForm = form
                stored = listOf(item("s"))
                Result.Success(item("s"))
            },
        )
        advanceUntilIdle()

        vm.create(ReferenceItemForm(name = "Boule de feu", description = "3d6 dégâts de feu."))
        advanceUntilIdle()

        assertEquals("3d6 dégâts de feu.", capturedForm?.description)
        assertEquals(1, vm.items.value.size)
        assertNull(vm.error.value)
    }

    @Test
    fun `update forwards item id name and fields to the use case and reloads`() = runTest {
        var capturedId: String? = null
        var capturedGroup: String? = null
        var capturedForm: ReferenceItemForm? = null
        var stored = listOf(item("f"))
        val vm = viewModel(
            type = ReferenceType.FORMATION,
            activeGroupId = MutableStateFlow("g-1"),
            listItems = ListReferenceItemsUseCase { _, _ -> Result.Success(stored) },
            updateItem = UpdateReferenceItemUseCase { _, itemId, groupId, form ->
                capturedId = itemId
                capturedGroup = groupId
                capturedForm = form
                stored = listOf(item("g"))
                Result.Success(item("g"))
            },
        )
        advanceUntilIdle()

        vm.update(
            "f",
            ReferenceItemForm(
                name = "Rôdeur+",
                stat = "vigueur",
                bonus = 4,
                competenceIds = listOf("c-1"),
            ),
        )
        advanceUntilIdle()

        assertEquals("f", capturedId)
        assertEquals("g-1", capturedGroup)
        assertEquals("Rôdeur+", capturedForm?.name)
        assertEquals("vigueur", capturedForm?.stat)
        assertEquals(4, capturedForm?.bonus)
        assertEquals(listOf("c-1"), capturedForm?.competenceIds)
        assertNull(capturedForm?.protectionPoints)
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
            updateItem = UpdateReferenceItemUseCase { _, _, _, _ -> Result.Failure(ReferenceError.NameAlreadyUsed) },
        )
        advanceUntilIdle()
        val listedAfterInit = listed

        vm.update("a", ReferenceItemForm("Doublon"))
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
