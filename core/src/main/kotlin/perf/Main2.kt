package perf

import com.google.common.base.Stopwatch
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import kotlinx.coroutines.experimental.withContext
import namespace.cancelBlockingFunction
import java.lang.Thread.sleep
import java.util.concurrent.Executors
import java.util.concurrent.atomic.LongAdder

private const val sleep = 5L
private const val runs = 200_000
private val done = LongAdder()
private val singleThreadedPool = Executors.newSingleThreadExecutor()
private val singleThreadedDispatcher = singleThreadedPool.asCoroutineDispatcher()

fun main(args: Array<String>) {
    val cWatch = Stopwatch.createStarted()
    runBlocking {
        val job = launch {
            while (done.toLong() < runs) {
                println("${done.toLong()} done...")
                delay(1000L)
            }
            cWatch.stop()
            println("Coroutines took: $cWatch, at ${cWatch.elapsed().toMillis() / runs} ms per run")
        }
        for (i in 0..runs) {
            launch {
                wrapDelay(sleep / 2)
                wrapDelay(sleep / 2)
                done.increment()
            }
//            delay(1L)
        }

        println("waiting...")
        job.join()
        println("done!")
    }

    singleThreadedDispatcher.close()
}

private suspend fun wrapDelay(sleep: Long) =
        withContext(singleThreadedDispatcher) {
            //        withContext(CommonPool) {
            suspendCancellableCoroutine<Unit> { continuation ->
                continuation.invokeOnCancellation { cancelBlockingFunction() }
                continuation.resume(sleep(sleep))
            }
        }
