package com.agustinpujol.notagus;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * v7: agrega completedAtDayKey en tasks (nullable).
 */
@Database(
        entities = {
                Task.class,
                Subtask.class,
                Note.class
        },
        version = 7,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract TaskDao taskDao();
    public abstract SubtaskDao subtaskDao();
    public abstract NoteDao noteDao();

    // v2 -> v3: agrega pinned en tasks
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0");
        }
    };

    // v3 -> v4: crea tabla notes
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS notes (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "title TEXT NOT NULL, " +
                            "body TEXT NOT NULL, " +
                            "createdAt INTEGER NOT NULL, " +
                            "updatedAt INTEGER NOT NULL)"
            );
        }
    };

    // v4 -> v5: agrega repeatMode y dayKey en tasks
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN repeatMode INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE tasks ADD COLUMN dayKey INTEGER");
        }
    };

    // v5 -> v6: agrega sortIndex a notes y lo inicializa usando createdAt
    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE notes ADD COLUMN sortIndex INTEGER NOT NULL DEFAULT 0");
            db.execSQL("UPDATE notes SET sortIndex = createdAt");
        }
    };

    // ✅ v6 -> v7: agrega completedAtDayKey a tasks
    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN completedAtDayKey INTEGER");
            // Sin backfill: las permanentes ya tildadas quedarán visibles solo si se vuelven a tildar.
            // (Si querés backfill, se podría copiar dayKey donde aplique, pero no es el caso).
        }
    };
}
