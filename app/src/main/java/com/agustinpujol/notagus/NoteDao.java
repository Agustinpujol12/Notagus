package com.agustinpujol.notagus;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface NoteDao {

    // Cargar SIEMPRE en el orden persistido
    @Query("SELECT * FROM notes ORDER BY sortIndex ASC, id ASC")
    List<Note> getAllOrdered();

    // Para migración inicial de orden (o debug)
    @Query("SELECT COALESCE(MAX(sortIndex), -1) FROM notes")
    int getMaxSortIndex();

    // Actualizar un índice específico
    @Query("UPDATE notes SET sortIndex = :index WHERE id = :id")
    void updateSortIndex(long id, int index);

    @Insert
    long insert(Note n);

    @Update
    int update(Note n);

    @Delete
    int delete(Note n);
}
