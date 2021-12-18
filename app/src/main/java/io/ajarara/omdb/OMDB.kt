package io.ajarara.omdb

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ajarara.BuildConfig
import io.reactivex.Emitter
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import okhttp3.HttpUrl
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

interface OMDB {

    @GET("/")
    fun titleSearch(
        @Query("apiKey") apiKey: String,
        @Query("s") search: String,
        @Query("page") page: Int
    ): Single<SearchResponse>

    object Impl : OMDB by Retrofit.Builder()
        .baseUrl("http://www.omdbapi.com/")
        .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
        .addConverterFactory(JacksonConverterFactory.create(jacksonObjectMapper()))
        .build()
        .create(OMDB::class.java) {


        fun pagingTitleSearch(title: String): Flowable<SearchResponse> =
            titleSearch(BuildConfig.OMDB_KEY, title, 1)
                .flatMapPublisher { initialResponse ->
                    Flowable.generate(Callable { Search(initialResponse, 1) }, pagerOf(title))
                        .map { it.response }
                        .delay(500, TimeUnit.MILLISECONDS)
                        .startWith(initialResponse)
                }


        private fun pagerOf(title: String) = BiFunction { search: Search, emitter: Emitter<Search> ->
            when (search.response) {
                is SearchResponse.Failure -> {
                    emitter.onComplete()
                    search // swallowed
                }
                is SearchResponse.RawListing -> {
                    if (search.page * 10 > search.response.totalResults) {
                        emitter.onComplete()
                        search  // swallowed
                    } else {
                        val next = Search(
                            response = titleSearch(BuildConfig.OMDB_KEY, title, search.page + 1).blockingGet(),
                            page = search.page + 1
                        )
                        emitter.onNext(next)
                        next
                    }
                }
            }
        }

        private class Search(
            val response: SearchResponse,
            val page: Int
        )
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "Response", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(SearchResponse.RawListing::class, name = "True"),
    JsonSubTypes.Type(SearchResponse.Failure::class, name = "False")
)
sealed class SearchResponse {
    class RawListing(
        val Search: List<RawMovie>,
        val totalResults: Int,
        val Response: String
    ) : SearchResponse()

    // for _some_ reason jackson cannot handle a constructor with just one argument
    // to get around this, mark the Response field as visible. Funky.
    class Failure(
        val Error: String,
        val Response: String
    ) : SearchResponse()
}


class RawMovie(
    val Title: String,
    val Year: String,
    val imdbID: String,
    val Type: String,
    val Poster: String
)

data class Movie(
    val title: String,
    val year: String,
    val imdbID: String,
    val type: String,
    val poster: HttpUrl?
) {
    companion object {
        fun from(listing: SearchResponse.RawListing): List<Movie> {
            return listing.Search
                .map { rawMovie ->
                    with(rawMovie) {
                        Movie(
                            title = Title,
                            year = Year,
                            imdbID = imdbID,
                            type = Type,
                            poster = HttpUrl.parse(Poster)
                        )
                    }
                }
        }
    }
}