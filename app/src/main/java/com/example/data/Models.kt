package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class InlineRow(
    val entrada: String = "",
    val saida: String = "",
    val comentario: String = ""
)

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val responsibleNotes: String = "",
    val responsibleEditor: String = "",
    val course: String = "",
    val phase: String = "",
    val discipline: String = "",
    val globalPrompt: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "revisions")
data class Revision(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val orderIndex: Int = 0
)

@Entity(tableName = "aulas")
data class Aula(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val revisionId: Long,
    val title: String,
    val textInputsJson: String = "[]", // List<String> serialized
    val inlineInputsJson: String = "[]", // List<List<InlineRow>> serialized (one list of InlineRow per part)
    val output: String = "",
    val inlineMode: Boolean = true,
    val lessThanOneHour: Boolean = true,
    val multipart: Boolean = false,
    val orderIndex: Int = 0
)

object JsonUtils {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    
    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)
    
    private val inlineRowListType = Types.newParameterizedType(List::class.java, InlineRow::class.java)
    private val nestedInlineRowListType = Types.newParameterizedType(List::class.java, inlineRowListType)
    private val nestedInlineRowAdapter = moshi.adapter<List<List<InlineRow>>>(nestedInlineRowListType)

    fun serializeStringList(list: List<String>): String {
        return try {
            stringListAdapter.toJson(list)
        } catch (e: Exception) {
            "[]"
        }
    }

    fun deserializeStringList(json: String): List<String> {
        return try {
            stringListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeInlineRows(rows: List<List<InlineRow>>): String {
        return try {
            nestedInlineRowAdapter.toJson(rows)
        } catch (e: Exception) {
            "[]"
        }
    }

    fun deserializeInlineRows(json: String): List<List<InlineRow>> {
        return try {
            nestedInlineRowAdapter.fromJson(json) ?: listOf(emptyList())
        } catch (e: Exception) {
            listOf(emptyList())
        }
    }
}
