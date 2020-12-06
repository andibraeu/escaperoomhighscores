package de.andi95.escaperoom

import io.ktor.config.MapApplicationConfig
import io.ktor.http.*
import kotlin.test.*
import io.ktor.server.testing.*
import module

class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({
            (environment.config as MapApplicationConfig).apply {
                // Set here the properties
                put("database.uri", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
                put("database.driver", "org.h2.Driver")
            }
            module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertTrue { response.content?.contains("<h1>Escape Room High Scores</h1>")!! }
            }
        }
    }
}
