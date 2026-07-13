package eu.ejdr.presentation.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals

class AlphabeticalOrderTest {

    @Test
    fun `foldForSort met en minuscules et replie les accents`() {
        assertEquals("epee", foldForSort("Épée"))
        assertEquals("ca", foldForSort("ÇA"))
        assertEquals("oeuf", foldForSort("Œuf"))
        assertEquals("naive", foldForSort("naïve"))
        assertEquals("zebre", foldForSort("Zèbre"))
    }

    @Test
    fun `foldForSort laisse intact un libelle sans accent`() {
        assertEquals("dague", foldForSort("Dague"))
    }

    @Test
    fun `trie en ignorant la casse`() {
        val sorted = listOf("banane", "Abricot", "cerise").sortedAlphabeticallyBy { it }
        assertEquals(listOf("Abricot", "banane", "cerise"), sorted)
    }

    @Test
    fun `trie en ignorant les accents`() {
        val sorted = listOf("Zebre", "Élan", "Écu", "Epee").sortedAlphabeticallyBy { it }
        assertEquals(listOf("Écu", "Élan", "Epee", "Zebre"), sorted)
    }

    /**
     * Le cœur du correctif : un tri naïf compare les points de code, donc « Élan » (U+00C9)
     * atterrirait APRÈS « Zèbre ». Il doit tomber entre « Eau » et « Fer ».
     */
    @Test
    fun `un mot accentue se range a sa place phonetique et non apres Z`() {
        val sorted = listOf("Fer", "Eau", "Élan").sortedAlphabeticallyBy { it }
        assertEquals(listOf("Eau", "Élan", "Fer"), sorted)
    }

    @Test
    fun `l ordre est total et stable entre deux libelles de meme cle`() {
        val once = listOf("elfe", "Elfe").sortedAlphabeticallyBy { it }
        val twice = once.sortedAlphabeticallyBy { it }
        assertEquals(once, twice)
        assertEquals(listOf("Elfe", "elfe"), once)
    }

    @Test
    fun `trie une liste d objets par le libelle extrait`() {
        data class Item(val id: Int, val name: String)

        val sorted = listOf(Item(1, "Épée"), Item(2, "Dague"), Item(3, "arc"))
            .sortedAlphabeticallyBy { it.name }

        assertEquals(listOf(3, 2, 1), sorted.map { it.id })
    }

    @Test
    fun `gere la liste vide et la liste a un element`() {
        assertEquals(emptyList(), emptyList<String>().sortedAlphabeticallyBy { it })
        assertEquals(listOf("Seul"), listOf("Seul").sortedAlphabeticallyBy { it })
    }
}
