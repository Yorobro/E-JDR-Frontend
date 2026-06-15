package eu.ejdr.infrastructure.config

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppConfigTest {

    private val tmpDir = Files.createTempDirectory("ejdr-config").toFile()
    private val osEnv = mapOf("APPDATA" to tmpDir.absolutePath)

    @AfterTest
    fun cleanup() {
        tmpDir.deleteRecursively()
    }

    @Test
    fun `defaults to prod url when nothing is configured`() {
        val config = AppConfig.load(osEnv = osEnv, dotenv = emptyMap())

        assertEquals(AppConfig.PROD_API_URL, config.baseUrl)
        assertFalse(config.enableHttpLogging)
    }

    @Test
    fun `dotenv overrides the default url`() {
        val config = AppConfig.load(
            osEnv = osEnv,
            dotenv = mapOf("EJDR_API_URL" to AppConfig.DEV_API_URL, "EJDR_HTTP_LOG" to "true"),
        )

        assertEquals(AppConfig.DEV_API_URL, config.baseUrl)
        assertTrue(config.enableHttpLogging)
    }

    @Test
    fun `os env takes priority over dotenv`() {
        val config = AppConfig.load(
            osEnv = osEnv + ("EJDR_API_URL" to "http://os-wins:9000"),
            dotenv = mapOf("EJDR_API_URL" to AppConfig.DEV_API_URL),
        )

        assertEquals("http://os-wins:9000", config.baseUrl)
    }

    @Test
    fun `EnvFile parses keys ignoring comments quotes and export prefix`() {
        val file = File(tmpDir, ".env").apply {
            writeText(
                """
                # commentaire
                EJDR_API_URL=http://localhost:3000

                export EJDR_HTTP_LOG="true"
                QUOTED='value'
                """.trimIndent()
            )
        }

        val env = EnvFile.load(file)

        assertEquals("http://localhost:3000", env["EJDR_API_URL"])
        assertEquals("true", env["EJDR_HTTP_LOG"])
        assertEquals("value", env["QUOTED"])
    }

    @Test
    fun `EnvFile returns empty map when file is absent`() {
        assertEquals(emptyMap(), EnvFile.load(File(tmpDir, "does-not-exist.env")))
    }
}
