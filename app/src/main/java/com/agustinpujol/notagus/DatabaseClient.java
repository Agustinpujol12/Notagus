package com.agustinpujol.notagus;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;

/**
 * Singleton para exponer la instancia de Room (AppDatabase).
 */
public class DatabaseClient {

    private static volatile DatabaseClient instance;
    private final AppDatabase appDatabase;

    private DatabaseClient(@NonNull Context context) {
        appDatabase = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "notagus.db" // nombre del archivo de la BD
                )
                // 🔁 Migraciones conocidas:
                // 2→3: agrega 'pinned' a tasks
                // 3→4: crea tabla 'notes'
                // 4→5: agrega repeatMode y dayKey a tasks
                // 5→6: agrega sortIndex a notes e inicializa por createdAt
                // 6→7: agrega completedAtDayKey a tasks
                .addMigrations(
                        AppDatabase.MIGRATION_2_3,
                        AppDatabase.MIGRATION_3_4,
                        AppDatabase.MIGRATION_4_5,
                        AppDatabase.MIGRATION_5_6,
                        AppDatabase.MIGRATION_6_7
                )
                // .fallbackToDestructiveMigration() // Solo dev
                .build();
    }

    public static DatabaseClient getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (DatabaseClient.class) {
                if (instance == null) {
                    instance = new DatabaseClient(context);
                }
            }
        }
        return instance;
    }

    public AppDatabase getAppDatabase() {
        return appDatabase;
    }
}
