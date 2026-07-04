package com.polytrader.app.data.remote.dataapi

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Data API — public read-only portfolio/market data
 * (https://data-api.polymarket.com). `user` params take the proxy wallet.
 */
interface DataApiService {

    @GET("positions")
    suspend fun getPositions(
        @Query("user") user: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("sizeThreshold") sizeThreshold: Double = 1.0,
        @Query("sortBy") sortBy: String = "CURRENT",
        @Query("sortDirection") sortDirection: String = "DESC",
        @Query("redeemable") redeemable: Boolean? = null,
    ): List<PositionDto>

    @GET("activity")
    suspend fun getActivity(
        @Query("user") user: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("sortBy") sortBy: String = "TIMESTAMP",
        @Query("sortDirection") sortDirection: String = "DESC",
        @Query("type") type: String? = null,
    ): List<ActivityDto>

    @GET("value")
    suspend fun getValue(@Query("user") user: String): List<ValueDto>

    /** Recent trades for a market (condition id). `filterAmount` in USD pairs with filterType=CASH. */
    @GET("trades")
    suspend fun getTrades(
        @Query("market") conditionId: String? = null,
        @Query("user") user: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("takerOnly") takerOnly: Boolean = true,
        @Query("filterType") filterType: String? = null,
        @Query("filterAmount") filterAmount: Double? = null,
    ): List<TradeDto>

    /** `market` = comma-separated condition ids; limit hard-capped at 20/token. */
    @GET("holders")
    suspend fun getHolders(
        @Query("market") conditionIds: String,
        @Query("limit") limit: Int = 20,
        @Query("minBalance") minBalance: Int = 1,
    ): List<MetaHolderDto>
}
