package http

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.withContext
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.Result
import org.eclipse.jetty.client.util.BufferingResponseListener
import org.eclipse.jetty.util.ssl.SslContextFactory
import java.io.Closeable
import kotlin.coroutines.experimental.suspendCoroutine

internal class SuspendingHttpClient : AutoCloseable, Closeable {
    private val httpClient = HttpClient(SslContextFactory())

    init {
        httpClient.isFollowRedirects = false
        httpClient.maxConnectionsPerDestination = 100_000
        httpClient.maxRequestsQueuedPerDestination = 100_000
        httpClient.start()
    }

    override fun close() {
        httpClient.stop()
    }

    suspend fun http(url: String) =
            withContext(CommonPool) {
                suspendCoroutine<ByteArray> { continuation ->
                    httpClient.newRequest(url)
                            // Buffer response content up to 8 MiB
                            .send(object : BufferingResponseListener(8 * 1024 * 1024) {
                                override fun onComplete(result: Result?) {
                                    try {
                                        if (!result!!.isFailed) {
                                            val responseContent = content
                                            continuation.resume(responseContent)
                                        } else {
                                            throw result.failure
                                        }
                                    } catch (e: Exception) {
                                        continuation.resumeWithException(e)
                                    }
                                }
                            })
                }
            }
}