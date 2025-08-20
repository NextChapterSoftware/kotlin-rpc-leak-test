package com.nextchaptersoftware.trace.krpc

import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.testing.testApplication
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import kotlinx.rpc.krpc.ktor.client.rpc
import io.ktor.websocket.*
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.SAME_THREAD)
class KrpcServerMemoryLeakTests {

    @Test
    fun `server does not retain per-request objects for krpc`() = runTest {
        val leakProbe = LeakProbe()
        leakProbe.reset()

        testApplication {
            install(Krpc)
            routing {
                rpc("/rpc") {
                    rpcConfig {
                        serialization { json { ignoreUnknownKeys = true } }
                        waitForServices = false
                    }
                    registerService<LeakService> { LeakServiceImpl(call, leakProbe) }
                }
            }

            repeat(500) { i ->
                val client = createClient { installKrpc() }
                val proxy = client.rpc("/rpc") {
                    headers["TestHeader"] = "test-header"
                    rpcConfig { serialization { json() } }
                }.withService<LeakService>()

                val res = proxy.echo("ping-$i")
                assertEquals("ping-$i", res)

                client.cancel()
                client.close()
            }

            val refs = leakProbe.refsSnapshot()
            val allCleared = awaitGc(refs, leakProbe.queue(), timeoutMs = 20_000)

            assertTrue(
                allCleared,
                "Some server-side objects (ApplicationCall/service impl) were not GC'd — possible retention. " +
                        "Remaining=${refs.count { it.get() != null }}"
            )
        }
    }

    @Test
    fun `server does not retain per-request objects for regular HTTP`() = runTest {
        val leakProbe = LeakProbe()
        leakProbe.reset()

        testApplication {
            routing {
                get("/http/echo/{msg}") {
                    leakProbe.track(call, call.request, call.response)
                    val msg = call.parameters["msg"]!!
                    assertEquals("test-header", call.request.headers["TestHeader"])
                    call.respondText(msg, ContentType.Text.Plain)
                }
            }

            repeat(500) { i ->
                val client = createClient {}

                val msg = "ping-$i"
                val res = client.get("/http/echo/$msg") {
                    header("TestHeader", "test-header")
                }
                assertEquals(msg, res.bodyAsText())
                client.cancel()
                client.close()
            }


            val refs = leakProbe.refsSnapshot()
            val allCleared = awaitGc(refs, leakProbe.queue(), timeoutMs = 20_000)

            assertTrue(
                allCleared,
                "Some server-side HTTP objects (ApplicationCall/request/response) were not GC'd — possible retention. " +
                        "Remaining=${refs.count { it.get() != null }}"
            )
        }
    }

    @Test
    fun `server does not retain per-connection objects for WebSockets`() = runTest {
        val leakProbe = LeakProbe()
        leakProbe.reset()

        testApplication {
            install(io.ktor.server.websocket.WebSockets)

            routing {
                webSocket("/ws/echo") {
                    leakProbe.track(this, call)
                    val frame = incoming.receive()
                    if (frame is Frame.Text) {
                        send(Frame.Text(frame.readText()))
                    } else {
                        close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Only text"))
                    }
                }
            }

            repeat(400) { i ->
                val client = createClient { install(WebSockets) }

                client.webSocket("/ws/echo") {
                    val msg = "ping-$i"
                    send(Frame.Text(msg))
                    val echoed = (incoming.receive() as Frame.Text).readText()
                    assertEquals(msg, echoed)
                    close()
                }

                client.cancel()
                client.close()
            }

            val refs = leakProbe.refsSnapshot()
            val allCleared = awaitGc(refs, leakProbe.queue(), timeoutMs = 20_000)

            assertTrue(
                allCleared,
                "Some server-side WebSocket objects (session/call) were not GC'd — possible retention. " +
                        "Remaining=${refs.count { it.get() != null }}"
            )
        }
    }

    // ---- Helpers ----
    @Suppress("DEPRECATION")
    private fun awaitGc(
        refs: List<WeakReference<Any>>,
        queue: ReferenceQueue<Any>,
        timeoutMs: Long
    ): Boolean {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        while (System.nanoTime() < deadline) {
            repeat(3) {
                System.gc()
                System.runFinalization()
                Thread.sleep(10)
            }
            if (refs.all { it.get() == null }) return true
            while (queue.poll() != null) { /* drain */ }
        }
        return refs.all { it.get() == null }
    }
}
