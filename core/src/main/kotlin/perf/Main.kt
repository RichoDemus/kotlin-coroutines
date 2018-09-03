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
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.LongAdder
import kotlin.coroutines.experimental.suspendCoroutine

private const val sleep = 10L
private const val runs = 100_000
private val finishedThreads = LongAdder()
private val finishedCoroutines = LongAdder()
private val threadPool = Executors.newFixedThreadPool(100)
private val threadpoolDispatcher = threadPool.asCoroutineDispatcher()
private val singleThreadedPool = Executors.newSingleThreadExecutor()
private val singleThreadedDispatcher = singleThreadedPool.asCoroutineDispatcher()

fun main(args: Array<String>) {
    val cWatch = Stopwatch.createStarted()
    runBlocking {
        val job = launch {
            while (finishedCoroutines.toLong() < runs) {
                println("${finishedCoroutines.toLong()} done...")
                delay(1000L)
            }
            cWatch.stop()
            println("Coroutines took: $cWatch, at ${cWatch.elapsed().nano / runs} nanos per run")
        }
        for (i in 0..runs) {
            launch { workCoroutine() }
        }
        job.join()
    }
    val pWatch = Stopwatch.createStarted()
    for (i in 0..runs) {
        threadPool.execute { workThread() }
    }

    while (finishedThreads.toLong() < runs) {
        println("${finishedThreads.toLong()} done...")
        Thread.sleep(1000L)
    }
    pWatch.stop()
    println("Processes took: $pWatch, at ${pWatch.elapsed().nano / runs} nanos per run")
    threadPool.shutdown()
    singleThreadedPool.shutdown()
}

private suspend fun workCoroutine() {
    val first = cOne()
    val second = cTwo(first)
    cThree(second)
    finishedCoroutines.increment()
}

private suspend fun cOne(): String {
    wrapDelay()
    return UUID.randomUUID().toString()
}

private suspend fun cTwo(arg: String): String {
    wrapDelay()
    return arg.toUpperCase()
}

private suspend fun cThree(arg: String) {
    wrapDelay()
}

private suspend fun wrapDelay() =
        withContext(threadpoolDispatcher) {
            suspendCoroutine<Unit> {continuation ->
                continuation.resume(someSleep())
            }
        }


private fun workThread() {
    val first = tOne()
    val second = tTwo(first)
    tThree(second)
    finishedThreads.increment()
}

private fun tOne(): String {
    someSleep()
    return UUID.randomUUID().toString()
}

private fun tTwo(arg: String): String {
    someSleep()
    return arg.toUpperCase()
}

private fun tThree(arg: String) {
    someSleep()
}

private fun someSleep() {
    sleep(sleep)
}
