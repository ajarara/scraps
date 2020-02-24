package io.ajarara.omdb

import io.reactivex.Emitter
import io.reactivex.Flowable
import io.reactivex.functions.BiConsumer
import io.reactivex.functions.BiFunction
import org.junit.jupiter.api.Test
import java.util.concurrent.Callable

internal class OMDBTest {

    @Test
    fun `just testing`() {
        val resp = OMDB.Impl.titleSearch(BuildConfig.API_KEY, "Bees", 1)
            .blockingGet() as SearchResponse.RawListing

/*
        resp.Search
            .withIndex()
            .forEach { (idx, mov) -> println("$idx: ${mov.Title}") }
*/
    }

    @Test
    fun `testing error`() {
        OMDB.Impl.titleSearch(BuildConfig.API_KEY, "Bees", 0)
            .blockingGet() as SearchResponse.Failure
    }

    @Test
    fun `paging implementation works`() {
        val resp = OMDB.Impl.pagingTitleSearch("Example")

        resp.blockingForEach { resp ->
            when(resp) {
                is SearchResponse.Failure -> error(resp.Error)
                is SearchResponse.RawListing -> resp.Search.forEach {
                    println("${it.Title}, ${it.Poster}")
                }
            }
        }
    }

    @Test
    fun `flowables work as I expect`() {
        Flowable.generate(
            Callable { 0 },
            BiFunction { t1, emitter: Emitter<Int> -> emitter.onNext(t1); t1 + 1 }
        ).take(10)
            .blockingForEach { println(it) }
    }
}