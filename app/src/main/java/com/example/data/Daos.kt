package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY lastUpdated DESC")
    fun getAllProjectsFlow(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Long): Project?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Update
    suspend fun updateProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)
}

@Dao
interface RevisionDao {
    @Query("SELECT * FROM revisions WHERE projectId = :projectId ORDER BY orderIndex ASC")
    fun getRevisionsForProjectFlow(projectId: Long): Flow<List<Revision>>

    @Query("SELECT * FROM revisions WHERE projectId = :projectId ORDER BY orderIndex ASC")
    suspend fun getRevisionsForProject(projectId: Long): List<Revision>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevision(revision: Revision): Long

    @Update
    suspend fun updateRevision(revision: Revision)

    @Delete
    suspend fun deleteRevision(revision: Revision)
}

@Dao
interface AulaDao {
    @Query("SELECT * FROM aulas WHERE revisionId = :revisionId ORDER BY orderIndex ASC")
    fun getAulasForRevisionFlow(revisionId: Long): Flow<List<Aula>>

    @Query("SELECT * FROM aulas WHERE revisionId = :revisionId ORDER BY orderIndex ASC")
    suspend fun getAulasForRevision(revisionId: Long): List<Aula>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAula(aula: Aula): Long

    @Update
    suspend fun updateAula(aula: Aula)

    @Delete
    suspend fun deleteAula(aula: Aula)
}
