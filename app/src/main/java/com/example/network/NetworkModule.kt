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
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import okio.Buffer
import java.nio.charset.StandardCharsets

interface PolymarketApiService {
    @GET("events")
    suspend fun getEvents(
        @Query("limit") limit: Int = 20,
        @Query("active") active: Boolean = true,
        @Query("closed") closed: Boolean = false
    ): List<PolymarketEvent>
}

interface PolymarketClobApiService {
    @GET("markets")
    suspend fun getMarkets(
        @Query("next_cursor") nextCursor: String? = null
    ): List<ClobMarket>

    @POST("order")
    suspend fun placeOrder(
        @Body request: ClobOrderRequest
    ): ClobOrderResponse
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
    class ClobAuthInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val timestamp = (System.currentTimeMillis() / 1000).toString()
            val method = originalRequest.method
            val path = originalRequest.url.encodedPath + (originalRequest.url.encodedQuery?.let { "?$it" } ?: "")
            
            var bodyString = ""
            originalRequest.body?.let { requestBody ->
                val buffer = Buffer()
                requestBody.writeTo(buffer)
                bodyString = buffer.readString(StandardCharsets.UTF_8)
            }

            val message = timestamp + method + path + bodyString
            
            // Generate HMAC SHA256 Signature
            val secret = BuildConfig.POLYMARKET_API_SECRET
            val signature = try {
                if (secret.isNotBlank() && secret != "MY_POLYMARKET_API_SECRET") {
                    val secretDecoded = Base64.decode(secret, Base64.NO_WRAP)
                    val mac = Mac.getInstance("HmacSHA256")
                    mac.init(SecretKeySpec(secretDecoded, "HmacSHA256"))
                    val hash = mac.doFinal(message.toByteArray(StandardCharsets.UTF_8))
                    Base64.encodeToString(hash, Base64.NO_WRAP)
                } else ""
            } catch (e: Exception) {
                ""
            }

            val newRequest = originalRequest.newBuilder()
                .header("POLY_API_KEY", BuildConfig.POLYMARKET_API_KEY)
                .header("POLY_PASSPHRASE", BuildConfig.POLYMARKET_API_PASSPHRASE)
                .header("POLY_TIMESTAMP", timestamp)
                .header("POLY_SIGNATURE", signature)
                .header("POLY_ADDRESS", BuildConfig.POLYMARKET_ADDRESS)
                .build()

            return chain.proceed(newRequest)
        }
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
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

    private val clobOkHttpClient = okHttpClient.newBuilder()
        .addInterceptor(ClobAuthInterceptor())
        .build()

    val polymarketClobApi: PolymarketClobApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://clob.polymarket.com/")
            .client(clobOkHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PolymarketClobApiService::class.java)
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
