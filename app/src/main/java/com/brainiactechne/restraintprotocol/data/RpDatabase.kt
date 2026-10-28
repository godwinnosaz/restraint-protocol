package com.brainiactechne.restraintprotocol.data

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
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "commitments")
data class CommitmentEntity(
    @PrimaryKey val id: Int = 1,
    val selectedDurationMinutes: Int = 15,
    val activeDeadlineEpochMillis: Long = 0L,
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_outbox")
data class SyncOutboxEntity(
    @PrimaryKey val id: String,
    val eventType: String,
    val payloadJson: String,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val syncedAtEpochMillis: Long? = null
)

@Dao
interface RpDao {
    @Query("SELECT * FROM commitments WHERE id = 1 LIMIT 1")
    fun observeCommitment(): Flow<CommitmentEntity?>

    @Query("SELECT * FROM commitments WHERE id = 1 LIMIT 1")
    suspend fun getCommitment(): CommitmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCommitment(commitment: CommitmentEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOutbox(event: SyncOutboxEntity)

    @Query("SELECT * FROM sync_outbox WHERE syncedAtEpochMillis IS NULL ORDER BY createdAtEpochMillis ASC LIMIT :limit")
    suspend fun pendingOutbox(limit: Int = 50): List<SyncOutboxEntity>

    @Query("UPDATE sync_outbox SET syncedAtEpochMillis = :syncedAt WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>, syncedAt: Long)

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE syncedAtEpochMillis IS NULL")
    fun observePendingCount(): Flow<Int>

    @Transaction
    suspend fun saveCommitmentAndQueue(commitment: CommitmentEntity, outbox: SyncOutboxEntity) {
        upsertCommitment(commitment)
        insertOutbox(outbox)
    }
}

@Database(entities = [CommitmentEntity::class, SyncOutboxEntity::class], version = 1, exportSchema = false)
abstract class RpDatabase : RoomDatabase() {
    abstract fun dao(): RpDao

    companion object {
        @Volatile private var instance: RpDatabase? = null
        fun get(context: Context): RpDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, RpDatabase::class.java, "restraint_protocol.db"
            ).build().also { instance = it }
        }
    }
}
