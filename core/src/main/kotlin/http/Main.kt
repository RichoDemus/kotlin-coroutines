package http

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.withContext
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.Result
import org.eclipse.jetty.client.util.BufferingResponseListener
import org.eclipse.jetty.util.ssl.SslContextFactory
import kotlin.coroutines.experimental.suspendCoroutine


lateinit var httpClient: HttpClient

fun main(args: Array<String>) {
    httpClient = HttpClient(SslContextFactory())
    try {
        httpClient.isFollowRedirects = false
        httpClient.start()

        runBlocking {
            val results = List(1000) {
                async { http("https://richodemus.com")}
            }

            val asdf = results.map { it.await() }.map { String(it) }
            println("Got ${asdf.size} responses")
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
                                }
                            }
                        })
            }
        }