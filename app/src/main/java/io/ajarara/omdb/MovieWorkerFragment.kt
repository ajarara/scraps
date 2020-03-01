package io.ajarara.omdb

import android.graphics.Bitmap
import android.util.Log
import androidx.fragment.app.Fragment
import io.ajarara.BuildConfig
import io.reactivex.Emitter
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subscribers.DisposableSubscriber
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer
import kotlin.math.min

class MovieWorkerFragment : Fragment() {
    val posterRepository = PosterRepository()
    var searchRepository: SearchRepository? = null
        private set

    init {
        retainInstance = true
    }

    fun search(search: String): SearchRepository {
        val repo = searchRepository
        if (repo?.search == search) {
            return repo
        }
        repo?.dispose()

        return SearchRepository(search, OMDB.Impl).also {
            searchRepository = it
        }
    }
}

class SearchRepository(
    val search: String,
    private val omdb: OMDB
) {
    private val insertionRelay = PublishSubject.create<Insertion>()
    private val movies = mutableListOf<Movie>()
    private lateinit var yank: () -> Unit
    private val disposables = CompositeDisposable()

    init {
        omdb.titleSearch(BuildConfig.API_KEY, search, 1)
            .flatMapPublisher { initialResponse ->
                Flowable.generate(Callable { Search(initialResponse, 1) }, pagerOf(search))
                    .map { it.response }
                    // .delay(100, TimeUnit.MILLISECONDS)
                    .startWith(initialResponse)
            }
            .subscribeWith(object : DisposableSubscriber<SearchResponse>() {
                override fun onStart() {
                    request(1L)
                    yank = { request(1L) }
                }

                override fun onComplete() {
                    yank = {}
                }

                override fun onNext(searchResponse: SearchResponse) {
                    when (searchResponse) {
                        is SearchResponse.Failure -> Log.e(
                            "OMDB_FAILURE",
                            "Error when pulling ${searchResponse.Error}"
                        )
                        is SearchResponse.RawListing -> {
                            val beforeAdd = movies.size
                            movies.addAll(Movie.from(searchResponse))
                            insertionRelay.onNext(
                                Insertion(beforeAdd, movies.size - beforeAdd)
                            )
                        }
                    }
                }

                override fun onError(t: Throwable) = throw t
            })
            .let { disposables.add(it) }
    }

    val insertions: Observable<Insertion> = insertionRelay

    fun size() = movies.size / 3

    fun dispose() = disposables.dispose()

    fun getRow(rowPosition: Int): List<Movie> {
        require(rowPosition >= 0 && rowPosition < size()) {
            "Row position out of bounds! $rowPosition for size: ${size()}"
        }
        if (size() - rowPosition < 15) {
            yank()
        }
        return movies.subList(
            3 * rowPosition,
            min((3 * rowPosition) + 3, movies.size)
        )

    }

    private fun pagerOf(title: String) = BiFunction { emitted: Search, emitter: Emitter<Search> ->
        when (emitted.response) {
            is SearchResponse.Failure -> {
                emitter.onComplete()
                emitted // swallowed
            }
            is SearchResponse.RawListing -> {
                require(emitted.response.Search.size <= 10) {
                    "Found unexpected page size of ${emitted.response.Search.size}!"
                }
                if (emitted.page * 10 > emitted.response.totalResults) {
                    emitter.onComplete()
                    emitted  // swallowed
                } else {
                    val next = Search(
                        response = omdb.titleSearch(
                            BuildConfig.API_KEY,
                            title,
                            emitted.page + 1
                        ).blockingGet(),
                        page = emitted.page + 1
                    )
                    emitter.onNext(next)
                    next
                }
            }
        }
    }

    class Insertion(val positionStart: Int, val itemCount: Int) {
        init {
            require(itemCount >= 0)
            require(positionStart >= 0)
        }
    }

    private class Search(
        val response: SearchResponse,
        val page: Int
    )

}

class PosterRepository {
    private val loaded: MutableMap<HttpUrl, ByteArray> = mutableMapOf()

    private val okhttp = OkHttpClient()

    fun load(url: HttpUrl): Single<ByteArray> {
        val cached = loaded[url]
        if (cached != null) {
            return Single.just(cached)
        }

        val req = Request.Builder()
            .url(url)
            .build()

        return Single.fromCallable { okhttp.newCall(req).execute().body()!!.use { body -> body.bytes() } }
            .subscribeOn(Schedulers.io())
            .doOnSuccess { loaded[url] = it }
    }
}

