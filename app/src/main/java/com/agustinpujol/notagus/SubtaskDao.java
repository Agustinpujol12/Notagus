package com.agustinpujol.notagus;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SubtaskDao {

    // Inserta una subtarea y devuelve el id generado
    @Insert
    long insert(Subtask subtask);

    // Actualiza una subtarea existente
    @Update
    int update(Subtask subtask);

    // Borra una subtarea
    @Delete
    int delete(Subtask subtask);

    // Trae todas las subtareas que pertenecen a una tarea (taskId)
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY position ASC, id ASC")
    List<Subtask> getByTaskId(long taskId);

    // Cambia el estado done/undone rápidamente
    @Query("UPDATE subtasks SET done = :done WHERE id = :subtaskId")
    int updateDone(long subtaskId, boolean done);

    // Actualiza la posición (por si en el futuro querés reordenar subtareas)
    @Query("UPDATE subtasks SET position = :position WHERE id = :subtaskId")
    int updatePosition(long subtaskId, int position);

    // Borra todas las subtareas asociadas a una tarea (por ejemplo si se elimina la tarea padre)
    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    int deleteByTaskId(long taskId);

    // ===== NUEVO: contador de subtareas por tarea =====
    @Query("SELECT COUNT(*) FROM subtasks WHERE taskId = :taskId")
    int countByTaskId(long taskId);

    // (Opcional) Versión booleana super rápida usando EXISTS
    @Query("SELECT EXISTS(SELECT 1 FROM subtasks WHERE taskId = :taskId LIMIT 1)")
    boolean hasSubtasks(long taskId);
}
