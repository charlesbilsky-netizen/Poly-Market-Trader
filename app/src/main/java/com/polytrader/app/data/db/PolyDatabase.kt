package com.polytrader.app.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** A market the user pinned to their watchlist (snapshot fields for offline display). */
@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val marketId: String,
    val question: String,
    val slug: String,
    val eventSlug: String?,
    val imageUrl: String?,
    val probabilityAtAdd: Double?,
    val addedAt: Long,
)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long,
)

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<WatchlistEntity>>

    @Query("SELECT marketId FROM watchlist")
    fun observeIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entity: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE marketId = :marketId")
    suspend fun remove(marketId: String)

    @Query("SELECT COUNT(*) FROM watchlist WHERE marketId = :marketId")
    suspend fun count(marketId: String): Int
}

@Dao
interface RecentSearchDao {
    @Query("SELECT * FROM recent_searches ORDER BY searchedAt DESC LIMIT 8")
    fun observeRecent(): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entity: RecentSearchEntity)

    @Query("DELETE FROM recent_searches")
    suspend fun clear()
}

@Database(
    entities = [WatchlistEntity::class, RecentSearchEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class PolyDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun recentSearchDao(): RecentSearchDao

    companion object {
        fun build(context: Context): PolyDatabase =
            Room.databaseBuilder(context, PolyDatabase::class.java, "polytrader.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
