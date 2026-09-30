package com.budjet.app.data.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.budjet.app.data.model.Budget;
import com.budjet.app.data.model.Transaction;

@Database(entities = {Transaction.class, Budget.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "budjet_db";
    private static volatile AppDatabase INSTANCE;

    public abstract TransactionDao transactionDao();
    public abstract BudgetDao budgetDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE budgets ADD COLUMN showDailyBudget INTEGER NOT NULL DEFAULT 1");
            database.execSQL("ALTER TABLE budgets ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
