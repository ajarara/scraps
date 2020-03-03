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
import io.ajarara.R
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class MovieFragment : Fragment() {

    private lateinit var input: TextView
    private lateinit var listing: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.movies, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        input = view.findViewById(R.id.input)
        listing = view.findViewById(R.id.listing)

        val worker = worker()

        listing.layoutManager = LinearLayoutManager(context)
        listing.adapter = MovieAdapter(
            worker.searchRepository,
            worker.posterRepository
        )

        input.setOnEditorActionListener { v, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    with(input) {
                        worker.searchRepository.search(v.text.toString())
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(windowToken, 0)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun worker(): MovieWorkerFragment {
        return when (val found = fragmentManager!!.findFragmentByTag(omdbTag)) {
            null -> MovieWorkerFragment().also {
                fragmentManager!!.beginTransaction()
                    .add(it, omdbTag)
                    .commit()
            }
            is MovieWorkerFragment -> found
            else -> error("Fragment for tag $omdbTag not an OMDBWorkerFragment, ${found.javaClass}")
        }
    }

    companion object {
        const val tag = "MovieFragment.TAG"
        private const val omdbTag = "OMDBWorkerFragment.TAG"

        fun newInstance() = MovieFragment()
    }
}

private class MovieAdapter(
    private val movies: SearchRepository,
    private val posters: PosterRepository
) : RecyclerView.Adapter<PosterViewHolder>() {

    private val insertions = movies.insertions
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe {
            when(it) {
                is StructureUpdate.Insertion -> {
                    notifyItemRangeInserted(
                        it.positionStart,
                        it.itemCount
                    )
                }
                is StructureUpdate.Clear -> {
                    notifyItemRangeRemoved(0, it.size)
                }
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PosterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.poster, parent, false)

        return PosterViewHolder(view)
    }

    override fun getItemCount(): Int = movies.size()

    override fun onBindViewHolder(holder: PosterViewHolder, rowPos: Int) {
        val row = movies.getRow(rowPos)
        holder.bind(
            Posters(
                first = posterOrNull(row, 0),
                second = posterOrNull(row, 1),
                third = posterOrNull(row, 2)
            )
        )
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        insertions.dispose()
        movies.dispose()
    }

    private fun posterOrNull(row: List<Movie>, idx: Int): Single<ByteArray>? {
        // either we're on the last row and we're not fully populated
        // or the movie at this position doesn't have a poster (it happens)
        return row.getOrNull(idx)?.let { movie ->
            movie.poster?.let { validPoster ->
                posters.load(validPoster)
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
        clear()
        posters.first?.let { bindPoster(posterOne, it) }
        posters.second?.let { bindPoster(posterTwo, it) }
        posters.third?.let { bindPoster(posterThree, it) }
        Schedulers.trampoline()
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
                    value?.let { view.setImageBitmap(it) }
                }

                override fun onError(e: Throwable?) {
                    Log.e("POSTER_FETCH", "Could not fetch poster: $e")
                }

                override fun onSubscribe(d: Disposable?) {
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