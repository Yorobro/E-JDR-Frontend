package eu.ejdr.application.shared

import eu.ejdr.domain.shared.error.DomainError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

private data class TestError(override val message: String) : DomainError

class ResultTest {

    private val success: Result<Int, TestError> = Result.Success(2)
    private val failure: Result<Int, TestError> = Result.Failure(TestError("boom"))

    @Test
    fun `map transforms the success value and leaves failure untouched`() {
        assertEquals(Result.Success(4), success.map { it * 2 })
        assertEquals(failure, failure.map { it * 2 })
    }

    @Test
    fun `mapError transforms the error and leaves success untouched`() {
        assertEquals(success, success.mapError { TestError("other") })
        val mapped = failure.mapError { TestError(it.message.uppercase()) }
        assertIs<Result.Failure<TestError>>(mapped)
        assertEquals("BOOM", mapped.error.message)
    }

    @Test
    fun `flatMap chains on success and short-circuits on failure`() {
        assertEquals(Result.Success(6), success.flatMap { Result.Success(it + 4) })
        assertEquals(failure, failure.flatMap { Result.Success(it + 4) })
        val chainedFailure: Result<Int, TestError> = success.flatMap { Result.Failure(TestError("late")) }
        assertIs<Result.Failure<TestError>>(chainedFailure)
        assertEquals("late", chainedFailure.error.message)
    }

    @Test
    fun `getOrNull returns the value on success and null on failure`() {
        assertEquals(2, success.getOrNull())
        assertNull(failure.getOrNull())
    }

    @Test
    fun `getOrElse returns the value on success and the fallback on failure`() {
        assertEquals(2, success.getOrElse { -1 })
        assertEquals(-1, failure.getOrElse { -1 })
    }

    @Test
    fun `onSuccess runs the side effect only on success`() {
        var seen: Int? = null
        success.onSuccess { seen = it }
        assertEquals(2, seen)
        seen = null
        failure.onSuccess { seen = it }
        assertNull(seen)
    }

    @Test
    fun `onFailure runs the side effect only on failure`() {
        var seen: String? = null
        failure.onFailure { seen = it.message }
        assertEquals("boom", seen)
        seen = null
        success.onFailure { seen = it.message }
        assertNull(seen)
    }
}
