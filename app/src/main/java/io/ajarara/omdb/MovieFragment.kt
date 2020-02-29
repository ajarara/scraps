package io.ajarara.omdb

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.ajarara.BuildConfig
import io.ajarara.R
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlin.math.min

class MovieFragment : Fragment() {

    private lateinit var adapter: MovieAdapter
    private lateinit var input: TextView
    private lateinit var listing: RecyclerView

    private var pendingSearch: OngoingSearch? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.movies, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        require(!::adapter.isInitialized)
        super.onViewCreated(view, savedInstanceState)

        input = view.findViewById(R.id.input)
        listing = view.findViewById(R.id.listing)

        val movies = mutableListOf<Movie>()

        adapter = MovieAdapter(
            movies,
            getOrCreateWorker().posterRepository
        )
        listing.adapter = adapter
        val layoutManager = LinearLayoutManager(activity)
        listing.layoutManager = layoutManager
        listing.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var isLoading = false

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                val search = pendingSearch ?: return

                val shouldLoad = !isLoading
                        // && newState == RecyclerView.SCROLL_STATE_IDLE
                        // && layoutManager.findLastVisibleItemPosition() > movies.size -
                if (shouldLoad) {
                    isLoading = true
                    loadPage(
                        movies,
                        search,
                        adapter
                    ) { nextSearch ->
                        isLoading = false
                        pendingSearch = nextSearch
                    }
                }
            }

        })

        input.setOnEditorActionListener { v, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    movies.clear()

                    val newQuery = v.text.toString()
                    adapter.notifyDataSetChanged()
                    loadPage(
                        movies,
                        OngoingSearch(newQuery, 1),
                        adapter,
                        ::pendingSearch::set
                    )

                    with(input) {
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(windowToken, 0)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun getOrCreateWorker(): MovieWorkerFragment {
        return when (val found = childFragmentManager.findFragmentByTag(omdbTag)) {
            null -> MovieWorkerFragment().also {
                childFragmentManager.beginTransaction()
                    .add(it, omdbTag)
                    .commit()
            }
            is MovieWorkerFragment -> found
            else -> error("Fragment for tag $omdbTag not an OMDBWorkerFragment, ${found.javaClass}")
        }
    }


    companion object {
        private const val omdbTag = "OMDBWorkerFragment.TAG"


        fun newInstance() = MovieFragment()

        private fun loadPage(
            movies: MutableList<Movie>,
            ongoingSearch: OngoingSearch,
            adapter: MovieAdapter,
            nextState: (OngoingSearch?) -> Unit
        ) {
            OMDB.Impl.titleSearch(BuildConfig.API_KEY, ongoingSearch.term, ongoingSearch.page)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { searchResponse ->
                    when(searchResponse) {
                        is SearchResponse.RawListing -> {
                            val amountBeforeAdd = movies.size
                            val added = Movie.from(searchResponse)
                            movies.addAll(added)
                            adapter.notifyItemRangeInserted(amountBeforeAdd, added.size)
                            nextState(ongoingSearch.next())
                        }
                        is SearchResponse.Failure -> {
                            Log.w("OMDB_ERROR", searchResponse.Error)
                            nextState(null)
                        }
                    }
                }
        }

    }

    private data class OngoingSearch(
        val term: String,
        val page: Int
    ) {
        fun next() = copy(page = page + 1)
    }
}

private class MovieAdapter(
    private val movies: List<Movie>,
    private val posterRepository: PosterRepository
) : RecyclerView.Adapter<PosterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PosterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.poster, parent, false)


        return PosterViewHolder(view)
    }

    override fun getItemCount(): Int = movies.size / 3

    override fun onBindViewHolder(holder: PosterViewHolder, rowPos: Int) {
        val row = movies.subList(3 * rowPos, min((3 * rowPos) + 3, movies.size))
        holder.bind(
            Posters(
                first = posterOrNull(row, 0),
                second = posterOrNull(row, 1),
                third = posterOrNull(row, 2)
            )
        )
    }

    override fun onViewDetachedFromWindow(holder: PosterViewHolder) = holder.clear()

    private fun posterOrNull(row: List<Movie>, idx: Int): Single<ByteArray>? {
        // either we're on the last row and we're not fully populated
        // or the movie at this position doesn't have a poster (it happens)
        return row.getOrNull(idx)?.let { movie ->
            movie.poster?.let { validPoster ->
                posterRepository.load(validPoster)
            }
        }
    }
}

private class PosterViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val posterOne = view.findViewById<ImageView>(R.id.poster_one)
    private val posterTwo = view.findViewById<ImageView>(R.id.poster_two)
    private val posterThree = view.findViewById<ImageView>(R.id.poster_three)

    private val imageUpdates = CompositeDisposable()

    fun bind(posters: Posters) {
        imageUpdates.clear()
        posters.first?.let { bindPoster(posterOne, it) }
        posters.second?.let { bindPoster(posterTwo, it) }
        posters.third?.let { bindPoster(posterThree, it) }
    }

    fun clear() {
        posterOne.setImageBitmap(null)
        posterTwo.setImageBitmap(null)
        posterThree.setImageBitmap(null)
    }

    private fun bindPoster(view: ImageView, bitmapProvider: Single<ByteArray>) {
        bitmapProvider.map { BitmapFactory.decodeByteArray(it, 0, it.size) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Bitmap> {
                override fun onSuccess(value: Bitmap?) {
                    if (value == null) {
                        Log.w("POSTER_FETCH", "Bitmap was null")
                    } else {
                        value.let { view.setImageBitmap(it) }
                    }
                }

                override fun onError(e: Throwable) {
                    Log.e("POSTER_FETCH", "Could not fetch poster: $e")
                }

                override fun onSubscribe(d: Disposable) {
                    imageUpdates.add(d)
                }
            })
    }
}

class Posters(
    val first: Single<ByteArray>?,
    val second: Single<ByteArray>?,
    val third: Single<ByteArray>?
)