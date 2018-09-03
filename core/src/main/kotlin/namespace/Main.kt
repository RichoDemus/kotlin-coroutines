package namespace

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import kotlinx.coroutines.experimental.withContext
import java.time.Duration
import kotlin.system.measureTimeMillis

fun main2(args: Array<String>) = runBlocking<Unit> {
    var result: Deferred<String>? = null

    var time = measureTimeMillis {
        //        result = doAsync()
        result = async { justDo() }
    }
    println("First half in $time ms")

    time = measureTimeMillis {
        println(result!!.await())
    }
    println("Second half in $time ms")
    Thread.sleep(1000L)
}

fun main(args: Array<String>) = runBlocking {
    val job = async { await(Duration.ofSeconds(5)) }

    println("Just after async")

    launch { println("async done: ${job.await()}") }


    println("sleeping")
    delay(10_000)
    println("done")
}

suspend fun doAsync(): Deferred<String> {
    val job1 = async { await(Duration.ofSeconds(2)) }
    val job2 = async { await(Duration.ofSeconds(2)) }

    return async { "Total: ${job1.await()} and ${job2.await()}" }
}

suspend fun justDo(): String {
    val job1 = async { await(Duration.ofSeconds(2)) }
    val job2 = async { await(Duration.ofSeconds(2)) }

    launch { println("1 done: ${job1.await()}") }

    return "Total: ${job1.await()} + ${job2.await()}"
}


suspend fun await(duration: Duration) =
        withContext(CommonPool) {
            suspendCancellableCoroutine<String> { continuation ->
                continuation.invokeOnCancellation { cancelBlockingFunction() }
                continuation.resume(heavyBlockingFunction(duration))
            }
        }


fun heavyBlockingFunction(duration: Duration): String {
    Thread.sleep(duration.toMillis())
    return "Slept for ${duration.seconds} seconds"
}

fun cancelBlockingFunction() {
    // todo
}
