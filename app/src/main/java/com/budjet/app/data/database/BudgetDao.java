package com.budjet.app.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.budjet.app.data.model.Budget;

import java.util.List;

@Dao
public interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Budget budget);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Budget> budgets);

    @Update
    void update(Budget budget);

    @Delete
    void delete(Budget budget);

    @Query("DELETE FROM budgets WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM budgets")
    void deleteAll();

    @Query("SELECT * FROM budgets ORDER BY category ASC")
    LiveData<List<Budget>> getAllBudgets();

    @Query("SELECT * FROM budgets WHERE monthYear = :monthYear OR monthYear = 'MONTHLY' ORDER BY category ASC")
    LiveData<List<Budget>> getBudgetsForMonth(String monthYear);

    @Query("SELECT * FROM budgets WHERE category = :category AND (monthYear = :monthYear OR monthYear = 'MONTHLY') LIMIT 1")
    LiveData<Budget> getBudgetByCategoryLive(String category, String monthYear);

    @Query("SELECT * FROM budgets WHERE category = :category AND (monthYear = :monthYear OR monthYear = 'MONTHLY') LIMIT 1")
    Budget getBudgetByCategorySync(String category, String monthYear);

    @Query("SELECT * FROM budgets WHERE category = 'OVERALL' AND (monthYear = :monthYear OR monthYear = 'MONTHLY') LIMIT 1")
    LiveData<Budget> getOverallBudget(String monthYear);

    @Query("SELECT DISTINCT category FROM budgets WHERE category != 'OVERALL' ORDER BY category ASC")
    LiveData<List<String>> getAllDistinctCategories();

    // Synchronous for backup/restore
    @Query("SELECT * FROM budgets ORDER BY id ASC")
    List<Budget> getAllBudgetsSync();

    @Query("SELECT COUNT(*) FROM budgets")
    int getBudgetCountSync();
}
