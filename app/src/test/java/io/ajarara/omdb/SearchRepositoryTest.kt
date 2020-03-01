package io.ajarara.omdb

import io.ajarara.BuildConfig
import io.reactivex.Single
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class SearchRepositoryTest {

    @Test
    fun `search repository doesn't consume very large stream`() {
        val exampleSearch = "Avengers"
        val omdb = omdbOf { apiKey, search, page ->
            require(apiKey == BuildConfig.API_KEY)
            require(search == exampleSearch)
            // notice we don't respect page here, we just get a very large stream
            // of the same chunk of movies over and over again, just like real life
            Single.just(searchResponseOf(
                search = List(10) {
                    RawMovie(
                        Title = "Avengers",
                        Year = "1999",
                        imdbID = "10101",
                        Type = "N/A",
                        Poster = "N/A"
                    )
                },
                totalResults = Int.MAX_VALUE
            ))
        }

        val repo = SearchRepository(
            search = exampleSearch,
            omdb = omdb
        )

        assert(repo.getRow(2).all {
            it.title == "Avengers"
        }) {
            repo.getRow(15).joinToString { it.title }
        }

    }
    private companion object {
        private fun omdbOf(
            titleSearch: (apiKey: String, search: String, page: Int) -> Single<SearchResponse>
        ): OMDB = object : OMDB {
            override fun titleSearch(apiKey: String, search: String, page: Int): Single<SearchResponse> =
                titleSearch(apiKey, search, page)
        }

        private fun searchResponseOf(
            search: List<RawMovie>,
            totalResults: Int
        ) = SearchResponse.RawListing(
            Search = search,
            totalResults = totalResults,
            Response = "True"
        )
    }
}