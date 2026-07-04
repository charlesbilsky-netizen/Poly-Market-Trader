package com.example.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/*
 * All Polymarket surfaces used here are public, keyless, READ-ONLY endpoints.
 * There is deliberately no order placement, no signing and no credential
 * interceptor anywhere in this app — trading happens on polymarket.com.
 */

interface PolymarketApiService {
    @GET("events")
    suspend fun getEvents(
        @Query("limit") limit: Int = 20,
        @Query("active") active: Boolean = true,
        @Query("closed") closed: Boolean = false,
        @Query("order") order: String? = "volume24hr",
        @Query("ascending") ascending: Boolean? = false
    ): List<PolymarketEvent>

    /** Market lookup by slug — used to resolve CLOB token ids for charts. */
    @GET("markets")
    suspend fun getMarketsBySlug(
        @Query("slug") slug: String,
        @Query("limit") limit: Int = 1
    ): List<PolymarketMarket>
}

/** CLOB /markets returns an envelope, not a bare array. */
data class ClobMarketsResponse(
    val data: List<ClobMarket>? = null,
    val next_cursor: String? = null
)

interface PolymarketClobApiService {
    @GET("markets")
    suspend fun getMarkets(
        @Query("next_cursor") nextCursor: String? = null
    ): ClobMarketsResponse

    /** Single market by 0x… condition id (public). */
    @GET("markets/{conditionId}")
    suspend fun getMarket(@Path("conditionId") conditionId: String): ClobMarket

    /**
     * Probability time series. `market` takes the CLOB token id (asset id).
     * interval: 1h, 6h, 1d, 1w, 1m, max — fidelity in minutes
     * (1w needs >= 5, 1m needs >= 10).
     */
    @GET("prices-history")
    suspend fun getPriceHistory(
        @Query("market") tokenId: String,
        @Query("interval") interval: String = "1w",
        @Query("fidelity") fidelityMinutes: Int = 60
    ): PriceHistoryResponse

    /** Full order book for one token id. */
    @GET("book")
    suspend fun getBook(@Query("token_id") tokenId: String): ClobBookResponse
}

data class ClobMarket(
    val condition_id: String,
    val question: String,
    val description: String?,
    val market_slug: String?,
    val tokens: List<ClobToken>?,
    val active: Boolean,
    val closed: Boolean
)

data class ClobToken(
    val token_id: String,
    val outcome: String,
    val price: Double?
)

interface PolymarketDataApiService {
    @GET("positions")
    suspend fun getPositions(
        @Query("user") address: String
    ): List<DataApiPosition>

    /**
     * Recent trades for a market (0x… condition id). Pair filterType=CASH
     * with filterAmount to get only whale-sized fills (notional >= $amount).
     */
    @GET("trades")
    suspend fun getTrades(
        @Query("market") conditionId: String,
        @Query("limit") limit: Int = 25,
        @Query("takerOnly") takerOnly: Boolean = true,
        @Query("filterType") filterType: String? = null,
        @Query("filterAmount") filterAmount: Double? = null
    ): List<DataApiTrade>

    /** Top holders per outcome token for a market. */
    @GET("holders")
    suspend fun getHolders(
        @Query("market") conditionId: String,
        @Query("limit") limit: Int = 20
    ): List<DataApiMetaHolder>
}

data class DataApiPosition(
    val asset: String?,
    val conditionId: String?,
    val size: String?,
    val price: String?,
    val value: String?
)

data class DataApiTrade(
    val proxyWallet: String? = null,
    val side: String? = null,
    val size: Double? = null,
    val price: Double? = null,
    val timestamp: Long? = null, // unix seconds
    val title: String? = null,
    val outcome: String? = null,
    val name: String? = null,
    val pseudonym: String? = null,
    val transactionHash: String? = null
) {
    val notionalUsd: Double get() = (size ?: 0.0) * (price ?: 0.0)
    val traderLabel: String
        get() = name?.takeIf { it.isNotBlank() }
            ?: pseudonym?.takeIf { it.isNotBlank() }
            ?: proxyWallet?.let { "${it.take(6)}…${it.takeLast(4)}" } ?: "anon"
}

data class DataApiMetaHolder(
    val token: String? = null,
    val holders: List<DataApiHolder>? = null
)

data class DataApiHolder(
    val proxyWallet: String? = null,
    val name: String? = null,
    val pseudonym: String? = null,
    val amount: Double? = null,
    val outcomeIndex: Int? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

interface GrokApiService {
    @POST("v1/chat/completions")
    suspend fun getChatCompletions(
        @retrofit2.http.Header("Authorization") authHeader: String,
        @Body request: GrokChatRequest
    ): GrokChatResponse
}

interface OpenAiApiService {
    @POST("v1/chat/completions")
    suspend fun getChatCompletions(
        @retrofit2.http.Header("Authorization") authHeader: String,
        @Body request: OpenAiChatRequest
    ): OpenAiChatResponse
}

object NetworkModule {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    val polymarketApi: PolymarketApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://gamma-api.polymarket.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PolymarketApiService::class.java)
    }

    val polymarketClobApi: PolymarketClobApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://clob.polymarket.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PolymarketClobApiService::class.java)
    }

    val polymarketDataApi: PolymarketDataApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://data-api.polymarket.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PolymarketDataApiService::class.java)
    }

    val geminiApi: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    val grokApi: GrokApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.x.ai/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GrokApiService::class.java)
    }

    val openAiApi: OpenAiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenAiApiService::class.java)
    }
}
