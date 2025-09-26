package com.agustinpujol.notagus;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad Room que representa una fila de la tabla "tasks".
 * - 'repeatMode' controla si la tarea es de Único día o Siempre.
 * - 'dayKey' guarda la fecha a la que pertenece (solo si repeatMode = ONE_DAY).
 * - 'completedAtDayKey' guarda el día (AAAAMMDD) en que se tildó una permanente.
 *
 * Convención de dayKey/completedAtDayKey: entero AAAAMMDD (ej: 20250916).
 */
@Entity(tableName = "tasks")
public class Task {

    public static final int REPEAT_ONE_DAY = 0;
    public static final int REPEAT_ALWAYS  = 1;

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String title;
    private boolean done;

    @ColumnInfo(name = "pinned")
    private boolean pinned = false;

    /** 0 = Único día, 1 = Siempre (default) */
    @ColumnInfo(name = "repeatMode")
    private int repeatMode = REPEAT_ALWAYS;

    /** Clave AAAAMMDD. Solo aplica si repeatMode = ONE_DAY. Puede ser null si es "Siempre". */
    @ColumnInfo(name = "dayKey")
    private Integer dayKey; // Integer para permitir null

    /** Día AAAAMMDD en que se completó una tarea permanente (null si pendiente o si es Único día). */
    @ColumnInfo(name = "completedAtDayKey")
    private Integer completedAtDayKey;

    // Constructor vacío requerido por Room
    public Task() { }

    // Conveniencia para crear desde la UI
    public Task(String title) {
        this.title = title;
        this.done = false;
        this.pinned = false;
        this.repeatMode = REPEAT_ALWAYS;
        this.dayKey = null;
        this.completedAtDayKey = null;
    }

    // Conveniencia para crear Único día
    public Task(String title, int repeatMode, Integer dayKey) {
        this.title = title;
        this.done = false;
        this.pinned = false;
        this.repeatMode = repeatMode;
        this.dayKey = dayKey;
        this.completedAtDayKey = null;
    }

    // Getters / Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }

    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }

    public int getRepeatMode() { return repeatMode; }
    public void setRepeatMode(int repeatMode) { this.repeatMode = repeatMode; }

    public Integer getDayKey() { return dayKey; }
    public void setDayKey(Integer dayKey) { this.dayKey = dayKey; }

    public Integer getCompletedAtDayKey() { return completedAtDayKey; }
    public void setCompletedAtDayKey(Integer completedAtDayKey) { this.completedAtDayKey = completedAtDayKey; }
}
