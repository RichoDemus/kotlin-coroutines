package http

import com.google.common.base.Stopwatch
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.withContext
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.Result
import org.eclipse.jetty.client.util.BufferingResponseListener
import org.eclipse.jetty.util.ssl.SslContextFactory
import java.util.concurrent.atomic.LongAdder
import kotlin.coroutines.experimental.suspendCoroutine


lateinit var httpClient: HttpClient

fun main(args: Array<String>) {
    httpClient = HttpClient(SslContextFactory())
    try {
        httpClient.isFollowRedirects = false
        httpClient.maxConnectionsPerDestination = 100_000
        httpClient.maxRequestsQueuedPerDestination = 100_000
        httpClient.start()

        val started = LongAdder()
        val done = LongAdder()
        var running = true
        runBlocking {
            launch {
                while (running) {
                    println("${started.toLong()} started and ${done.toLong()} done")
                    delay(1000L)
                }
            }

            val watch = Stopwatch.createStarted()
            val results = List(10_000) {
                async {
                    val result = http("http://localhost:8080")
                    started.increment()
                    result
                }
            }

            val asdf = results.map {
                val res = it.await()
                done.increment()
                res
            }.map { String(it) }
            watch.stop()
            println("Got ${asdf.size} responses in $watch")
            running = false
            println("${started.toLong()} started and ${done.toLong()} done")
        }


    } finally {
        httpClient.stop()
    }
}

private suspend fun http(url: String) =
        withContext(CommonPool) {
            suspendCoroutine<ByteArray> { continuation ->
                httpClient.newRequest(url)
                        // Buffer response content up to 8 MiB
                        .send(object : BufferingResponseListener(8 * 1024 * 1024) {
                            override fun onComplete(result: Result?) {
                                if (!result!!.isFailed) {
                                    val responseContent = content
                                    continuation.resume(responseContent)
                                } else {
                                    continuation.resumeWithException(RuntimeException(result.failure))
                                }
                            }
                        })
            }
        }
