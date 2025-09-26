package com.agustinpujol.notagus;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class Note {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull public String title = "";
    @NonNull public String body  = "";

    public long createdAt;  // epoch millis
    public long updatedAt;  // epoch millis

    // Índice de orden persistente
    @ColumnInfo(name = "sortIndex")
    public int sortIndex = 0;

    // ---- Campo de UI (NO se guarda en DB) ----
    @Ignore public boolean placeholder = false;

    public Note() {}

    @Ignore
    public static Note placeholder() {
        Note n = new Note();
        n.placeholder = true;
        return n;
    }
}
