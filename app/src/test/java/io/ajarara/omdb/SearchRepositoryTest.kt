package io.ajarara.omdb

import io.reactivex.Single
import org.junit.jupiter.api.Assertions.*

internal class SearchRepositoryTest {

    companion object {
        private fun omdbOf(
            titleSearch: (apiKey: String, search: String, page: Int) -> Single<SearchResponse>
        ): OMDB = object : OMDB {
            override fun titleSearch(apiKey: String, search: String, page: Int): Single<SearchResponse> =
                titleSearch(apiKey, search, page)
        }
    }
}