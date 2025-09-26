package com.agustinpujol.notagus;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TaskDao {

    // === Listado por fecha seleccionada (Siempre + Único día) ===
    // Regla: Permanentes (repeatAlways)
    // - Mostrar si NO están done
    // - O si están done y completedAtDayKey == dayKey (para que aparezcan tachadas SOLO ese día)
    @Query("SELECT * FROM tasks " +
            "WHERE (" +
            "       repeatMode = :repeatAlways AND (" +
            "           done = 0 OR (done = 1 AND completedAtDayKey = :dayKey)" +
            "       )" +
            "     ) " +
            "   OR (repeatMode = :repeatOneDay AND dayKey = :dayKey) " +
            "ORDER BY pinned DESC, id ASC")
    List<Task> getForDay(int dayKey, int repeatOneDay, int repeatAlways);

    // === SOLO Único día para una fecha dada ===
    @Query("SELECT * FROM tasks " +
            "WHERE repeatMode = :repeatOneDay " +
            "  AND dayKey = :dayKey " +
            "ORDER BY pinned DESC, id ASC")
    List<Task> getOnlyOneDayForDay(int dayKey, int repeatOneDay);

    // === Puntos del calendario: dayKey con tareas ÚNICO DÍA en un rango ===
    @Query("SELECT DISTINCT dayKey FROM tasks " +
            "WHERE repeatMode = :repeatOneDay " +
            "  AND dayKey IS NOT NULL " +
            "  AND dayKey BETWEEN :startDayKey AND :endDayKey")
    List<Integer> getOneDayKeysInRange(int startDayKey, int endDayKey, int repeatOneDay);

    // === Mini-tilde: días en que se completaron PERMANENTES ===
    @Query("SELECT DISTINCT completedAtDayKey FROM tasks " +
            "WHERE repeatMode = :repeatAlways " +
            "  AND done = 1 " +
            "  AND completedAtDayKey IS NOT NULL " +
            "  AND completedAtDayKey BETWEEN :startDayKey AND :endDayKey")
    List<Integer> getCompletedAlwaysKeysInRange(int startDayKey, int endDayKey, int repeatAlways);

    // === CRUD básico ===
    @Query("SELECT * FROM tasks ORDER BY pinned DESC, id ASC")
    List<Task> getAll();

    @Insert
    long insert(Task task);

    @Delete
    void delete(Task task);

    @Query("DELETE FROM tasks")
    void deleteAll();

    @Update
    int update(Task task);

    // === Pines ===
    @Query("UPDATE tasks SET pinned = 0")
    void clearAllPins();

    @Query("UPDATE tasks SET pinned = 1 WHERE id = :taskId")
    void setPinned(long taskId);

    @Query("UPDATE tasks SET pinned = 0 WHERE id = :taskId")
    void unpin(long taskId);

    // === Estado done (usado por el Adapter para el tachado) ===
    @Query("UPDATE tasks SET done = :done WHERE id = :id")
    void updateDone(long id, boolean done);

    // ✅ Nuevo: actualizar done + completedAtDayKey en una sola llamada
    @Query("UPDATE tasks SET done = :done, completedAtDayKey = :completedAt WHERE id = :id")
    void updateDoneAndCompletion(long id, boolean done, Integer completedAt);

    // === BORRAR SOLO tareas ÚNICO DÍA anteriores a hoy ===
    @Query("DELETE FROM tasks " +
            "WHERE repeatMode = :repeatOneDay " +
            "  AND dayKey IS NOT NULL " +
            "  AND dayKey < :todayDayKey")
    int deletePastOneDay(int todayDayKey, int repeatOneDay);

    // (Opcional) contar cuántas se eliminarán
    @Query("SELECT COUNT(*) FROM tasks " +
            "WHERE repeatMode = :repeatOneDay " +
            "  AND dayKey IS NOT NULL " +
            "  AND dayKey < :todayDayKey")
    int countPastOneDay(int todayDayKey, int repeatOneDay);

    // === Siempre + fijadas ===
    @Query("SELECT * FROM tasks " +
            "WHERE repeatMode = :repeatAlways " +
            "  AND pinned = 1 " +
            "ORDER BY pinned DESC, id ASC")
    List<Task> getPinnedAlways(int repeatAlways);

    // === BORRAR tareas PERMANENTES completadas en días pasados (< hoy) ===
    @Query("DELETE FROM tasks " +
            "WHERE repeatMode = :repeatAlways " +
            "  AND done = 1 " +
            "  AND completedAtDayKey IS NOT NULL " +
            "  AND completedAtDayKey < :todayDayKey")
    int deletePastCompletedAlways(int todayDayKey, int repeatAlways);

    // (Opcional) contar cuántas permanentes se eliminarán
    @Query("SELECT COUNT(*) FROM tasks " +
            "WHERE repeatMode = :repeatAlways " +
            "  AND done = 1 " +
            "  AND completedAtDayKey IS NOT NULL " +
            "  AND completedAtDayKey < :todayDayKey")
    int countPastCompletedAlways(int todayDayKey, int repeatAlways);
}
