package com.budjet.app.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.budjet.app.data.model.CategorySpending;
import com.budjet.app.data.model.Transaction;

import java.util.List;

@Dao
public interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Transaction transaction);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Transaction> transactions);

    @Update
    void update(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    @Query("DELETE FROM transactions WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM transactions")
    void deleteAll();

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    LiveData<List<Transaction>> getAllTransactions();

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsBetween(long startDate, long endDate);

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate " +
            "AND (:type = 'ALL' OR type = :type) " +
            "AND (:query = '' OR title LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%') " +
            "ORDER BY date DESC")
    LiveData<List<Transaction>> getFilteredTransactions(long startDate, long endDate, String type, String query);

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate")
    LiveData<Double> getTotalByTypeAndDateRange(String type, long startDate, long endDate);

    @Query("SELECT category, COALESCE(SUM(amount), 0.0) AS totalSpent, COUNT(*) AS transactionCount " +
            "FROM transactions WHERE type = 'EXPENSE' AND date BETWEEN :startDate AND :endDate " +
            "GROUP BY category ORDER BY totalSpent DESC")
    LiveData<List<CategorySpending>> getCategorySpending(long startDate, long endDate);

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'EXPENSE' AND category = :category AND date BETWEEN :startDate AND :endDate")
    LiveData<Double> getSpentForCategory(String category, long startDate, long endDate);

    @Query("SELECT * FROM transactions WHERE category = :category AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    LiveData<List<Transaction>> getTransactionsByCategory(String category, long startDate, long endDate);

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    LiveData<List<Transaction>> getRecentTransactions(int limit);

    // Synchronous queries for backup/restore
    @Query("SELECT * FROM transactions ORDER BY date ASC")
    List<Transaction> getAllTransactionsSync();

    @Query("SELECT COUNT(*) FROM transactions")
    int getTransactionCountSync();
}
