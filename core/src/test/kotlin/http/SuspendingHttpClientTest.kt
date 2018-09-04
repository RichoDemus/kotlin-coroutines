package http

import com.google.common.util.concurrent.RateLimiter
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Ignore
import org.junit.Test
import org.mockserver.client.MockServerClient
import org.mockserver.model.Delay.seconds
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import java.time.Duration
import java.util.concurrent.atomic.LongAdder
import kotlin.system.measureNanoTime


class SuspendingHttpClientTest {
    @Ignore
    @Test
    fun `Run requests with delay`() {
        // docker run -it --rm --name mockserver -p 1080:1080 -p 1090:1090 jamesdbloom/mockserver:mockserver-5.4.1 /opt/mockserver/run_mockserver.sh -logLevel OFF -serverPort 1080 -proxyPort 1090

        val mockServerClient = MockServerClient("localhost", 1080)
        mockServerClient.reset()
        mockServerClient
                .`when`(
                        request()
                                .withMethod("GET")
                                .withPath("/test")
                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withDelay(seconds(1))
                                .withBody("Hello World")
                )



        SuspendingHttpClient().use { httpClient ->
            val started = LongAdder()
            val done = LongAdder()
            var running = true

            val time = measureNanoTime {
                runBlocking {
                    launch {
                        while (running) {
                            println("${started.toLong()} started and ${done.toLong()} done")
                            delay(1000L)
                        }
                    }

                    val results = List(500) {
                        async {
                            started.increment()
                            val result = httpClient.http("http://localhost:1080/test")
                            done.increment()
                            result
                        }
                    }

                    val asdf = results.map { it.await() }.map { String(it) }
                    running = false
                }
            }

            val duration = Duration.ofNanos(time)

            println("${started.toLong()} started and ${done.toLong()} done")
            println("Took ${duration.pretty()}")

        }
    }

    @Test
    fun `Run requests`() {
        SuspendingHttpClient().use { httpClient ->
            val started = LongAdder()
            val done = LongAdder()
            var running = true
            val rateLimiter = RateLimiter.create(100.0)

            val time = measureNanoTime {
                runBlocking {
                    launch {
                        while (running) {
                            println("${started.toLong()} started and ${done.toLong()} done")
                            delay(1000L)
                        }
                    }

                    val results = List(10_000) {
                        while ((started.toLong() - done.toLong()) > 500) {
                            delay(10L)
                        }
                        async {
                            started.increment()
                            val result = httpClient.http("http://www.mocky.io/v2/x?mocky-delay=1s")
                            done.increment()
                            result
                        }
                    }

                    val asdf = results.map { it.await() }.map { String(it) }
                    running = false
                }
            }

            val duration = Duration.ofNanos(time)

            println("${started.toLong()} started and ${done.toLong()} done")
            println("Took ${duration.pretty()}")

        }
    }

    private fun Duration.pretty(): String {
        val minutes = this.toMinutes()
        val withoutMinutes = this.minusMinutes(minutes)
        val seconds = withoutMinutes.seconds
        val withoutSeconds = withoutMinutes.minusSeconds(seconds)
        val milis = withoutSeconds.toMillis()

        return "$minutes minutes, $seconds seconds, $milis milis"
    }
}