package com.example.data

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

/**
 * Local persistence engine (Phase 2.2):
 *  - watchlist: markets the user starred, with a display snapshot so the tab
 *    renders instantly offline
 *  - research_reports: cached AI output (Alpha scans, per-market analyses,
 *    daily digests) so expensive LLM responses are viewable offline
 */

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val marketId: String,
    val title: String,
    val url: String,
    val probability: Int,
    val category: String,
    val volume: String,
    val tokenId: String?,
    val conditionId: String?,
    val addedAt: Long,
)

@Entity(tableName = "research_reports")
data class ResearchReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** ALPHA, DIGEST, ANALYSIS, CORRELATION */
    val type: String,
    /** Market id for per-market reports, empty for universe-wide scans. */
    val marketId: String,
    val title: String,
    val content: String,
    val provider: String,
    val createdAt: Long,
)

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<WatchlistEntity>>

    @Query("SELECT marketId FROM watchlist")
    fun observeIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE marketId = :marketId")
    suspend fun delete(marketId: String)

    @Query("SELECT COUNT(*) FROM watchlist WHERE marketId = :marketId")
    suspend fun count(marketId: String): Int

    @Query("SELECT * FROM watchlist")
    suspend fun getAllOnce(): List<WatchlistEntity>
}

@Dao
interface ResearchReportDao {
    @Query("SELECT * FROM research_reports ORDER BY createdAt DESC LIMIT 50")
    fun observeAll(): Flow<List<ResearchReportEntity>>

    @Insert
    suspend fun insert(entity: ResearchReportEntity)

    @Query("DELETE FROM research_reports WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM research_reports WHERE createdAt < :cutoff")
    suspend fun pruneOlderThan(cutoff: Long)
}

@Database(
    entities = [WatchlistEntity::class, ResearchReportEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun researchReportDao(): ResearchReportDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "polytrader.db",
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
