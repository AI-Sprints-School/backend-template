package com.learning.services

import org.junit.jupiter.api.Tag
import com.learning.config.RedisConfig
import io.lettuce.core.api.sync.RedisCommands
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.AfterEach

/**
 * Unit тесты для CacheService
 */
@Tag("extension")
@Tag("chapter7")
class CacheServiceTest {

    private lateinit var cacheService: CacheService
    private lateinit var mockCommands: RedisCommands<String, String>

    @BeforeEach
    fun setUp() {
        cacheService = CacheService()
        mockCommands = mockk(relaxed = true)
        
        // Mock RedisConfig static methods
        mockkObject(RedisConfig)
        every { RedisConfig.getCommands() } returns mockCommands
        every { RedisConfig.isConnected() } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(RedisConfig)
    }

    // ==================== Availability Tests ====================

    @Test
    fun `isAvailable should return true when Redis is connected`() {
        every { RedisConfig.isConnected() } returns true

        val result = cacheService.isAvailable()

        assertTrue(result)
    }

    @Test
    fun `isAvailable should return false when Redis is not connected`() {
        every { RedisConfig.isConnected() } returns false

        val result = cacheService.isAvailable()

        assertFalse(result)
    }

    // ==================== Get Tests ====================

    @Test
    fun `get should return value when key exists`() {
        val key = "test-key"
        val expectedValue = "test-value"
        every { mockCommands.get(key) } returns expectedValue

        val result = cacheService.get(key)

        assertEquals(expectedValue, result)
    }

    @Test
    fun `get should return null when key does not exist`() {
        val key = "non-existent-key"
        every { mockCommands.get(key) } returns null

        val result = cacheService.get(key)

        assertNull(result)
    }

    @Test
    fun `get should return null when Redis commands are null`() {
        every { RedisConfig.getCommands() } returns null

        val result = cacheService.get("any-key")

        assertNull(result)
    }

    @Test
    fun `get should return null on exception`() {
        every { mockCommands.get(any()) } throws RuntimeException("Connection error")

        val result = cacheService.get("any-key")

        assertNull(result)
    }

    // ==================== Set Tests ====================

    @Test
    fun `set should return true on successful set`() {
        val key = "test-key"
        val value = "test-value"
        every { mockCommands.setex(key, CacheService.DEFAULT_TTL, value) } returns "OK"

        val result = cacheService.set(key, value)

        assertTrue(result)
        verify { mockCommands.setex(key, CacheService.DEFAULT_TTL, value) }
    }

    @Test
    fun `set should use custom TTL when provided`() {
        val key = "test-key"
        val value = "test-value"
        val customTtl = 120L
        every { mockCommands.setex(key, customTtl, value) } returns "OK"

        val result = cacheService.set(key, value, customTtl)

        assertTrue(result)
        verify { mockCommands.setex(key, customTtl, value) }
    }

    @Test
    fun `set should return false when Redis commands are null`() {
        every { RedisConfig.getCommands() } returns null

        val result = cacheService.set("key", "value")

        assertFalse(result)
    }

    @Test
    fun `set should return false on exception`() {
        every { mockCommands.setex(any(), any(), any()) } throws RuntimeException("Error")

        val result = cacheService.set("key", "value")

        assertFalse(result)
    }

    // ==================== Delete Tests ====================

    @Test
    fun `delete should return true when key deleted`() {
        val key = "test-key"
        every { mockCommands.del(key) } returns 1L

        val result = cacheService.delete(key)

        assertTrue(result)
    }

    @Test
    fun `delete should return false when key not found`() {
        val key = "non-existent-key"
        every { mockCommands.del(key) } returns 0L

        val result = cacheService.delete(key)

        assertFalse(result)
    }

    @Test
    fun `delete should return false when Redis commands are null`() {
        every { RedisConfig.getCommands() } returns null

        val result = cacheService.delete("any-key")

        assertFalse(result)
    }

    // ==================== Delete By Pattern Tests ====================

    @Test
    fun `deleteByPattern should delete matching keys`() {
        val pattern = "course:*"
        val matchingKeys = listOf("course:1", "course:2", "course:3")
        every { mockCommands.keys(pattern) } returns matchingKeys
        every { mockCommands.del(*matchingKeys.toTypedArray()) } returns 3L

        val result = cacheService.deleteByPattern(pattern)

        assertEquals(3L, result)
    }

    @Test
    fun `deleteByPattern should return 0 when no matching keys`() {
        val pattern = "non-existent:*"
        every { mockCommands.keys(pattern) } returns emptyList()

        val result = cacheService.deleteByPattern(pattern)

        assertEquals(0L, result)
    }

    // ==================== Exists Tests ====================

    @Test
    fun `exists should return true when key exists`() {
        val key = "existing-key"
        every { mockCommands.exists(key) } returns 1L

        val result = cacheService.exists(key)

        assertTrue(result)
    }

    @Test
    fun `exists should return false when key does not exist`() {
        val key = "non-existent-key"
        every { mockCommands.exists(key) } returns 0L

        val result = cacheService.exists(key)

        assertFalse(result)
    }

    // ==================== Expire Tests ====================

    @Test
    fun `expire should return true when TTL set successfully`() {
        val key = "test-key"
        val seconds = 300L
        every { mockCommands.expire(key, seconds) } returns true

        val result = cacheService.expire(key, seconds)

        assertTrue(result)
    }

    @Test
    fun `expire should return false when key not found`() {
        val key = "non-existent-key"
        every { mockCommands.expire(key, any<Long>()) } returns false

        val result = cacheService.expire(key, 300L)

        assertFalse(result)
    }

    // ==================== Rate Limiting Tests ====================

    @Test
    fun `incrementRateLimit should increment counter`() {
        val key = "user:123"
        val fullKey = "${CacheService.PREFIX_RATE_LIMIT}$key"
        every { mockCommands.incr(fullKey) } returns 1L
        every { mockCommands.expire(fullKey, CacheService.RATE_LIMIT_TTL) } returns true

        val result = cacheService.incrementRateLimit(key)

        assertEquals(1L, result)
    }

    @Test
    fun `incrementRateLimit should set expiry on first increment`() {
        val key = "new-user"
        val fullKey = "${CacheService.PREFIX_RATE_LIMIT}$key"
        every { mockCommands.incr(fullKey) } returns 1L
        every { mockCommands.expire(fullKey, any<Long>()) } returns true

        cacheService.incrementRateLimit(key)

        verify { mockCommands.expire(fullKey, CacheService.RATE_LIMIT_TTL) }
    }

    @Test
    fun `incrementRateLimit should not set expiry on subsequent increments`() {
        val key = "existing-user"
        val fullKey = "${CacheService.PREFIX_RATE_LIMIT}$key"
        every { mockCommands.incr(fullKey) } returns 5L

        cacheService.incrementRateLimit(key)

        verify(exactly = 0) { mockCommands.expire(any(), any<Long>()) }
    }

    @Test
    fun `getRateLimit should return current count`() {
        val key = "user:123"
        every { mockCommands.get("${CacheService.PREFIX_RATE_LIMIT}$key") } returns "42"

        val result = cacheService.getRateLimit(key)

        assertEquals(42L, result)
    }

    @Test
    fun `getRateLimit should return 0 when key not found`() {
        val key = "new-user"
        every { mockCommands.get("${CacheService.PREFIX_RATE_LIMIT}$key") } returns null

        val result = cacheService.getRateLimit(key)

        assertEquals(0L, result)
    }

    @Test
    fun `isRateLimitExceeded should return true when limit exceeded`() {
        val key = "user:123"
        every { mockCommands.get("${CacheService.PREFIX_RATE_LIMIT}$key") } returns "100"

        val result = cacheService.isRateLimitExceeded(key, 100)

        assertTrue(result)
    }

    @Test
    fun `isRateLimitExceeded should return false when under limit`() {
        val key = "user:123"
        every { mockCommands.get("${CacheService.PREFIX_RATE_LIMIT}$key") } returns "50"

        val result = cacheService.isRateLimitExceeded(key, 100)

        assertFalse(result)
    }

    // ==================== Session Management Tests ====================

    @Test
    fun `saveSession should save session data with TTL`() {
        val sessionId = "session-123"
        val data = """{"userId": "user-1"}"""
        val fullKey = "${CacheService.PREFIX_USER_SESSION}$sessionId"
        every { mockCommands.setex(fullKey, CacheService.SESSION_TTL, data) } returns "OK"

        val result = cacheService.saveSession(sessionId, data)

        assertTrue(result)
        verify { mockCommands.setex(fullKey, CacheService.SESSION_TTL, data) }
    }

    @Test
    fun `getSession should return session data`() {
        val sessionId = "session-123"
        val expectedData = """{"userId": "user-1"}"""
        every { mockCommands.get("${CacheService.PREFIX_USER_SESSION}$sessionId") } returns expectedData

        val result = cacheService.getSession(sessionId)

        assertEquals(expectedData, result)
    }

    @Test
    fun `deleteSession should delete session`() {
        val sessionId = "session-123"
        val fullKey = "${CacheService.PREFIX_USER_SESSION}$sessionId"
        every { mockCommands.del(fullKey) } returns 1L

        val result = cacheService.deleteSession(sessionId)

        assertTrue(result)
    }

    @Test
    fun `extendSession should extend session TTL`() {
        val sessionId = "session-123"
        val fullKey = "${CacheService.PREFIX_USER_SESSION}$sessionId"
        every { mockCommands.expire(fullKey, CacheService.SESSION_TTL) } returns true

        val result = cacheService.extendSession(sessionId)

        assertTrue(result)
    }

    // ==================== Course Caching Tests ====================

    @Test
    fun `invalidateCourse should delete course from cache`() {
        val courseId = "course-123"
        val fullKey = "${CacheService.PREFIX_COURSE}$courseId"
        every { mockCommands.del(fullKey) } returns 1L

        val result = cacheService.invalidateCourse(courseId)

        assertTrue(result)
    }

    @Test
    fun `invalidateAllCourses should delete all course related keys`() {
        every { mockCommands.keys("${CacheService.PREFIX_COURSE}*") } returns listOf("course:1", "course:2")
        every { mockCommands.keys("${CacheService.PREFIX_COURSES_LIST}*") } returns listOf("courses:list:all")
        every { mockCommands.del(*anyVararg()) } returns 3L

        val result = cacheService.invalidateAllCourses()

        assertTrue(result > 0)
    }
}
