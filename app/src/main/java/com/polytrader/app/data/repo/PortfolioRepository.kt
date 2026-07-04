package com.polytrader.app.data.repo

import com.polytrader.app.core.net.ApiResult
import com.polytrader.app.core.net.safeApiCall
import com.polytrader.app.core.util.Format
import com.polytrader.app.data.remote.dataapi.DataApiService
import com.polytrader.app.data.remote.dataapi.toDomain
import com.polytrader.app.data.remote.gamma.GammaApiService
import com.polytrader.app.domain.model.ActivityItem
import com.polytrader.app.domain.model.PortfolioSnapshot
import com.polytrader.app.domain.model.TraderProfile
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async

/**
 * Read-only portfolio access by public proxy-wallet address. There is no
 * signing, no keys and no way to move funds from this app.
 */
class PortfolioRepository(
    private val dataApi: DataApiService,
    private val gamma: GammaApiService,
) {

    fun isValidAddress(address: String): Boolean =
        Regex("^0x[a-fA-F0-9]{40}$").matches(address.trim())

    suspend fun getSnapshot(wallet: String): ApiResult<PortfolioSnapshot> = safeApiCall {
        coroutineScope {
            val positionsDeferred = async { dataApi.getPositions(user = wallet, limit = 200) }
            val valueDeferred = async { runCatching { dataApi.getValue(wallet) }.getOrDefault(emptyList()) }
            val positions = positionsDeferred.await().mapNotNull { it.toDomain() }
            val total = valueDeferred.await().firstOrNull()?.value
                ?: positions.sumOf { it.currentValue }
            PortfolioSnapshot(wallet = wallet, totalValue = total, positions = positions)
        }
    }

    suspend fun getActivity(wallet: String, limit: Int = 60): ApiResult<List<ActivityItem>> =
        safeApiCall {
            dataApi.getActivity(user = wallet, limit = limit).mapNotNull { it.toDomain() }
        }

    suspend fun getProfile(wallet: String): ApiResult<TraderProfile> = safeApiCall {
        val dto = gamma.getPublicProfile(wallet)
        TraderProfile(
            proxyWallet = dto.proxyWallet ?: wallet,
            name = dto.name,
            pseudonym = dto.pseudonym,
            bio = dto.bio,
            profileImage = dto.profileImage,
            xUsername = dto.xUsername,
            verifiedBadge = dto.verifiedBadge ?: false,
            createdAt = Format.parseInstant(dto.createdAt),
        )
    }
}
