package com.polytrader.app.data.remote.clob

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * CLOB API, read-only subset (https://clob.polymarket.com). Public / no auth.
 * Order placement endpoints are intentionally absent — this is a research
 * companion; trading happens on polymarket.com.
 */
interface ClobApiService {

    /** `token_id` is the ERC-1155 asset id (huge decimal string). */
    @GET("book")
    suspend fun getBook(@Query("token_id") tokenId: String): ClobBookDto

    @GET("midpoint")
    suspend fun getMidpoint(@Query("token_id") tokenId: String): ClobMidpointDto

    @GET("spread")
    suspend fun getSpread(@Query("token_id") tokenId: String): ClobSpreadDto

    @GET("price")
    suspend fun getPrice(
        @Query("token_id") tokenId: String,
        @Query("side") side: String, // "BUY" (best bid) | "SELL" (best ask)
    ): ClobPriceDto

    /**
     * Probability time series. `market` takes the token id (despite the name);
     * either [interval] (`1h,6h,1d,1w,1m,max`) or startTs/endTs, with
     * [fidelity] in minutes.
     */
    @GET("prices-history")
    suspend fun getPriceHistory(
        @Query("market") tokenId: String,
        @Query("interval") interval: String? = null,
        @Query("startTs") startTs: Long? = null,
        @Query("endTs") endTs: Long? = null,
        @Query("fidelity") fidelityMinutes: Int? = null,
    ): PriceHistoryDto
}
