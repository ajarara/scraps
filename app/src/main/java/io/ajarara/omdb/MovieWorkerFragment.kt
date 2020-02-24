package io.ajarara.omdb

import android.graphics.Bitmap
import androidx.fragment.app.Fragment
import io.reactivex.Emitter
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.net.URL
import java.util.concurrent.Callable
import java.util.function.BiConsumer

class MovieWorkerFragment : Fragment() {
    val posterRepository = PosterRepository()

    init {
        retainInstance = true
    }
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
