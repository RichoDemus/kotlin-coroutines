package http

import com.google.common.base.Stopwatch
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.eclipse.jetty.client.HttpClient
import java.util.concurrent.atomic.LongAdder


private lateinit var httpClient: HttpClient

fun main(args: Array<String>) {

    SuspendingHttpClient().use { httpClient ->

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
            val results = List(1_000) {
                async {
                    val result = httpClient.http("http://localhost:8080")
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


    }
}
