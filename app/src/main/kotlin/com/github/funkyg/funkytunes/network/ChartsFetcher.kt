package com.github.funkyg.funkytunes.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.github.funkyg.funkytunes.Album
import com.github.funkyg.funkytunes.FunkyApplication
import com.github.funkyg.funkytunes.Image
import com.github.salomonbrys.kotson.*
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFutureTask
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import javax.inject.Inject

class ChartsFetcher(context: Context) {

    private val AlbumFeed = "https://itunes.apple.com/us/rss/topalbums/limit=200/json"
    private val Tag = "ChartsFetcher"

    @Inject lateinit var volleyQueue: RequestQueue

    init {
        (context.applicationContext as FunkyApplication).component.inject(this)
    }

    fun fetchAppleAlbumFeed(listener: (List<Album>) -> Unit) {
        volleyQueue.add(StringRequest(Request.Method.GET, AlbumFeed, Response.Listener<String> { reply ->
            val f = ListenableFutureTask.create { parseFeed(reply) }
            Futures.addCallback(f, object : FutureCallback<List<Album>> {
                override fun onSuccess(result: List<Album>?) { listener(result!!) }
                override fun onFailure(t: Throwable?) {}
            }, { command -> Handler(Looper.getMainLooper()).post(command)
            })
            Thread(f).start()
        }, Response.ErrorListener { error ->
            Log.w(Tag, error)
        }))
    }

    private fun parseFeed(data: String): List<Album> {
        val json = JsonParser().parse(data)["feed"]["entry"].array
        return Gson.fromJson<List<Album>>(json)
    }

    private val Gson = GsonBuilder()
            .registerTypeAdapter<Album> {
                deserialize {
                    Album(it.json["im:name"]["label"].string,
                            it.json["im:artist"]["label"].string,
                            it.context.deserialize<List<Image>>(it.json["im:image"].array).last())
                }
            }
            .registerTypeAdapter<Image> {
                deserialize {
                    Image(it.json["label"].string)
                }
            }
            .create()
}
