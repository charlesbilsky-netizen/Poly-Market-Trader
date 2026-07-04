package com.polytrader.app.data.remote

import com.polytrader.app.data.remote.clob.ClobApiService
import com.polytrader.app.data.remote.dataapi.DataApiService
import com.polytrader.app.data.remote.gamma.GammaApiService
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Network wiring. Every REST surface here is public and keyless — there are
 * deliberately no signing interceptors or credentials anywhere in this app.
 */
object NetworkModule {

    val moshi: Moshi = Moshi.Builder().build()

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private fun retrofit(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    val gammaApi: GammaApiService by lazy {
        retrofit("https://gamma-api.polymarket.com/").create(GammaApiService::class.java)
    }

    val clobApi: ClobApiService by lazy {
        retrofit("https://clob.polymarket.com/").create(ClobApiService::class.java)
    }

    val dataApi: DataApiService by lazy {
        retrofit("https://data-api.polymarket.com/").create(DataApiService::class.java)
    }
}
