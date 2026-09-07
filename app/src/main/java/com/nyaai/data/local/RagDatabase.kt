package com.nyaai.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "documents")
@Fts4
data class DocumentEntity(
    val sourcePath: String,
    val content: String
) {
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    var rowid: Int = 0
}

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["sessionId"])],
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val feedback: String? = null, // "GOOD", "AVERAGE", "POOR"
    val confidence: Double? = null 
)

@Entity(tableName = "training_examples")
data class TrainingExampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val question: String,
    val answer: String,
    val sourcePath: String,
    val legalDomain: String,
    val reasoningQuality: Int = 1 
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val source: String = "Legal Reference",
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface RagDao {
    @Query("SELECT rowid, sourcePath, content FROM documents WHERE documents MATCH :query")
    suspend fun search(query: String): List<DocumentEntity>

    @Insert
    suspend fun insert(document: DocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(documents: List<DocumentEntity>)

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun getDocumentCount(): Int

    // Chat History
    @Insert
    suspend fun createSession(session: ChatSessionEntity): Long

    @Query("SELECT * FROM chat_sessions ORDER BY timestamp DESC")
    suspend fun getAllSessions(): List<ChatSessionEntity>

    @Insert
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSession(sessionId: Long): List<MessageEntity>
    
    @Query("UPDATE chat_sessions SET title = :newTitle WHERE sessionId = :sessionId")
    suspend fun updateSessionTitle(sessionId: Long, newTitle: String)

    @Query("UPDATE chat_messages SET feedback = :feedback WHERE id = :messageId")
    suspend fun updateMessageFeedback(messageId: Long, feedback: String)

    @Query("DELETE FROM chat_sessions")
    suspend fun clearAllHistory()

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: Long)

    @Query("DELETE FROM chat_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    // Bookmarks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarksFlow(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    suspend fun getAllBookmarks(): List<BookmarkEntity>

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Long)

    @Query("DELETE FROM bookmarks WHERE content = :content")
    suspend fun deleteBookmarkByContent(content: String)

    @Query("SELECT COUNT(*) FROM bookmarks WHERE content = :content")
    suspend fun isBookmarked(content: String): Int

    // Training Logic
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrainingExample(example: TrainingExampleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTrainingExamples(examples: List<TrainingExampleEntity>)

    @Query("SELECT * FROM training_examples WHERE question LIKE '%' || :query || '%' OR answer LIKE '%' || :query || '%' LIMIT 5")
    suspend fun searchTrainingExamples(query: String): List<TrainingExampleEntity>

    // Get "Good" examples to help the LLM learn from user feedback
    @Query("SELECT * FROM chat_messages WHERE feedback = 'GOOD' ORDER BY timestamp DESC LIMIT 10")
    suspend fun getGoodExamples(): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM training_examples")
    suspend fun getTrainingCount(): Int

    @Query("SELECT COUNT(*) FROM training_examples")
    fun getTrainingCountFlow(): Flow<Int>
    
    @Query("SELECT rowid, sourcePath, content FROM documents ORDER BY rowid ASC LIMIT :limit OFFSET :offset")
    suspend fun getPagedDocuments(limit: Int, offset: Int): List<DocumentEntity>
}

val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `bookmarks` (" +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`title` TEXT NOT NULL, " +
            "`content` TEXT NOT NULL, " +
            "`source` TEXT NOT NULL, " +
            "`timestamp` INTEGER NOT NULL)"
        )
    }
}

@Database(
    entities = [
        DocumentEntity::class,
        ChatSessionEntity::class,
        MessageEntity::class,
        TrainingExampleEntity::class,
        BookmarkEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class RagDatabase : RoomDatabase() {
    abstract fun ragDao(): RagDao
}
