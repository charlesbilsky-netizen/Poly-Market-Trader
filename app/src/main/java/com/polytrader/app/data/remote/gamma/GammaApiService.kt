package com.polytrader.app.data.remote.gamma

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Gamma Markets API — public, no auth (https://gamma-api.polymarket.com).
 *
 * Notes from live verification (July 2026):
 *  - `active`/`archived` are NOT accepted as query params on /markets
 *    (they are response fields only); use `closed` + client-side filtering.
 *  - list endpoints return bare JSON arrays.
 */
interface GammaApiService {

    @GET("markets")
    suspend fun getMarkets(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("order") order: String? = null,
        @Query("ascending") ascending: Boolean? = null,
        @Query("closed") closed: Boolean? = null,
        @Query("tag_id") tagId: Int? = null,
        @Query("related_tags") relatedTags: Boolean? = null,
        @Query("liquidity_num_min") liquidityMin: Double? = null,
        @Query("volume_num_min") volumeMin: Double? = null,
        @Query("start_date_min") startDateMin: String? = null,
        @Query("end_date_min") endDateMin: String? = null,
        @Query("end_date_max") endDateMax: String? = null,
    ): List<GammaMarketDto>

    @GET("markets/{id}")
    suspend fun getMarket(@Path("id") id: String): GammaMarketDto

    @GET("markets/slug/{slug}")
    suspend fun getMarketBySlug(@Path("slug") slug: String): GammaMarketDto

    @GET("events")
    suspend fun getEvents(
        @Query("limit") limit: Int = 25,
        @Query("offset") offset: Int = 0,
        @Query("order") order: String? = null,
        @Query("ascending") ascending: Boolean? = null,
        @Query("closed") closed: Boolean? = null,
        @Query("active") active: Boolean? = null,
        @Query("featured") featured: Boolean? = null,
        @Query("tag_id") tagId: Int? = null,
        @Query("tag_slug") tagSlug: String? = null,
        @Query("related_tags") relatedTags: Boolean? = null,
        @Query("liquidity_min") liquidityMin: Double? = null,
        @Query("volume_min") volumeMin: Double? = null,
        @Query("end_date_min") endDateMin: String? = null,
        @Query("end_date_max") endDateMax: String? = null,
    ): List<GammaEventDto>

    @GET("events/{id}")
    suspend fun getEvent(@Path("id") id: String): GammaEventDto

    @GET("events/slug/{slug}")
    suspend fun getEventBySlug(@Path("slug") slug: String): GammaEventDto

    @GET("tags")
    suspend fun getTags(
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("is_carousel") isCarousel: Boolean? = null,
    ): List<GammaTagDto>

    @GET("public-search")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit_per_type") limitPerType: Int = 20,
        @Query("page") page: Int = 1,
        @Query("events_status") eventsStatus: String? = "active",
        @Query("search_tags") searchTags: Boolean? = null,
        @Query("search_profiles") searchProfiles: Boolean? = null,
    ): GammaSearchResponseDto

    @GET("public-profile")
    suspend fun getPublicProfile(@Query("address") address: String): GammaProfileDto
}
