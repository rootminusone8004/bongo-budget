package com.budjet.app.data.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.budjet.app.data.database.AppDatabase;
import com.budjet.app.data.database.BudgetDao;
import com.budjet.app.data.database.TransactionDao;
import com.budjet.app.data.model.Budget;
import com.budjet.app.data.model.CategorySpending;
import com.budjet.app.data.model.ExportData;
import com.budjet.app.data.model.Transaction;
import com.budjet.app.util.DateUtils;
import com.budjet.app.util.JsonUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BudgetRepository {

    public interface RepositoryCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public static class ImportResult {
        public final int transactionCount;
        public final int budgetCount;

        public ImportResult(int transactionCount, int budgetCount) {
            this.transactionCount = transactionCount;
            this.budgetCount = budgetCount;
        }
    }

    private final TransactionDao transactionDao;
    private final BudgetDao budgetDao;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public BudgetRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        this.transactionDao = db.transactionDao();
        this.budgetDao = db.budgetDao();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    // Transactions Queries
    public LiveData<List<Transaction>> getAllTransactions() {
        return transactionDao.getAllTransactions();
    }

    public LiveData<List<Transaction>> getTransactionsBetween(long start, long end) {
        return transactionDao.getTransactionsBetween(start, end);
    }

    public LiveData<List<Transaction>> getFilteredTransactions(long start, long end, String type, String query) {
        return transactionDao.getFilteredTransactions(start, end, type, query == null ? "" : query.trim());
    }

    public LiveData<Double> getTotalIncome(long start, long end) {
        return transactionDao.getTotalByTypeAndDateRange(Transaction.TYPE_INCOME, start, end);
    }

    public LiveData<Double> getTotalExpense(long start, long end) {
        return transactionDao.getTotalByTypeAndDateRange(Transaction.TYPE_EXPENSE, start, end);
    }

    public LiveData<List<CategorySpending>> getCategorySpending(long start, long end) {
        return transactionDao.getCategorySpending(start, end);
    }

    public LiveData<List<Transaction>> getRecentTransactions(int limit) {
        return transactionDao.getRecentTransactions(limit);
    }

    public LiveData<List<Transaction>> getTransactionsByCategory(String category, long start, long end) {
        return transactionDao.getTransactionsByCategory(category, start, end);
    }

    // Budgets Queries
    public LiveData<List<Budget>> getBudgetsForMonth(String monthYear) {
        return budgetDao.getBudgetsForMonth(monthYear);
    }

    public LiveData<Budget> getOverallBudget(String monthYear) {
        return budgetDao.getOverallBudget(monthYear);
    }

    public LiveData<List<String>> getAllDistinctCategories() {
        return budgetDao.getAllDistinctCategories();
    }

    // Async Operations
    public void insertTransaction(Transaction transaction) {
        executorService.execute(() -> transactionDao.insert(transaction));
    }

    public void updateTransaction(Transaction transaction) {
        executorService.execute(() -> transactionDao.update(transaction));
    }

    public void deleteTransaction(Transaction transaction) {
        executorService.execute(() -> transactionDao.delete(transaction));
    }

    public void insertBudget(Budget budget) {
        executorService.execute(() -> budgetDao.insert(budget));
    }

    public void updateBudget(Budget budget) {
        executorService.execute(() -> budgetDao.update(budget));
    }

    public void deleteBudget(Budget budget) {
        executorService.execute(() -> budgetDao.delete(budget));
    }

    public void clearAllData(RepositoryCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                transactionDao.deleteAll();
                budgetDao.deleteAll();
                mainHandler.post(() -> {
                    if (callback != null) callback.onSuccess(null);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e);
                });
            }
        });
    }

    // JSON Export
    public void exportDataToStream(OutputStream outputStream, RepositoryCallback<String> callback) {
        executorService.execute(() -> {
            try {
                List<Transaction> transactions = transactionDao.getAllTransactionsSync();
                List<Budget> budgets = budgetDao.getAllBudgetsSync();
                ExportData data = new ExportData(DateUtils.formatIsoNow(), budgets, transactions);
                JsonUtils.writeJsonToStream(data, outputStream);
                mainHandler.post(() -> {
                    if (callback != null) callback.onSuccess("Export completed successfully");
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e);
                });
            }
        });
    }

    public void exportDataToString(RepositoryCallback<String> callback) {
        executorService.execute(() -> {
            try {
                List<Transaction> transactions = transactionDao.getAllTransactionsSync();
                List<Budget> budgets = budgetDao.getAllBudgetsSync();
                ExportData data = new ExportData(DateUtils.formatIsoNow(), budgets, transactions);
                String json = JsonUtils.toJson(data);
                mainHandler.post(() -> {
                    if (callback != null) callback.onSuccess(json);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e);
                });
            }
        });
    }

    // JSON Import
    public void importDataFromStream(InputStream inputStream, boolean overwrite, RepositoryCallback<ImportResult> callback) {
        executorService.execute(() -> {
            try {
                ExportData data = JsonUtils.fromJsonStream(inputStream);
                if (data == null) {
                    throw new IllegalArgumentException("Invalid or empty JSON file format.");
                }

                if (overwrite) {
                    transactionDao.deleteAll();
                    budgetDao.deleteAll();
                }

                List<Transaction> transactions = data.getTransactions();
                if (transactions != null && !transactions.isEmpty()) {
                    List<Transaction> toInsert = new ArrayList<>();
                    for (Transaction t : transactions) {
                        // Reset ID so database generates new auto-increment ID to prevent collisions
                        toInsert.add(new Transaction(t.getTitle(), t.getAmount(), t.getType(), t.getCategory(), t.getDate(), t.getNote()));
                    }
                    transactionDao.insertAll(toInsert);
                }

                List<Budget> budgets = data.getBudgets();
                if (budgets != null && !budgets.isEmpty()) {
                    List<Budget> toInsertBudgets = new ArrayList<>();
                    for (Budget b : budgets) {
                        toInsertBudgets.add(new Budget(b.getCategory(), b.getAmount(), b.getMonthYear()));
                    }
                    budgetDao.insertAll(toInsertBudgets);
                }

                int transCount = transactions != null ? transactions.size() : 0;
                int bCount = budgets != null ? budgets.size() : 0;
                ImportResult result = new ImportResult(transCount, bCount);

                mainHandler.post(() -> {
                    if (callback != null) callback.onSuccess(result);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e);
                });
            }
        });
    }

    // Populate Sample Data for demonstration
    public void populateSampleData(RepositoryCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                Calendar cal = Calendar.getInstance();
                String currentMonthKey = DateUtils.formatMonthYearKey(cal);

                // Add Budgets
                List<Budget> budgets = new ArrayList<>();
                budgets.add(new Budget(Budget.CATEGORY_OVERALL, 2500.0, currentMonthKey));
                budgets.add(new Budget("Food & Dining", 400.0, currentMonthKey));
                budgets.add(new Budget("Groceries", 500.0, currentMonthKey));
                budgets.add(new Budget("Transport", 200.0, currentMonthKey));
                budgets.add(new Budget("Entertainment", 150.0, currentMonthKey));
                budgets.add(new Budget("Bills & Utilities", 350.0, currentMonthKey));
                budgets.add(new Budget("Health & Medical", 100.0, currentMonthKey));
                budgetDao.insertAll(budgets);

                // Add Transactions for current month
                List<Transaction> sampleTx = new ArrayList<>();
                long now = System.currentTimeMillis();
                long dayMs = 24L * 60 * 60 * 1000;

                sampleTx.add(new Transaction("Monthly Salary", 3800.00, Transaction.TYPE_INCOME, "Salary", now - (10 * dayMs), "Direct Deposit"));
                sampleTx.add(new Transaction("Freelance Design Project", 650.00, Transaction.TYPE_INCOME, "Freelance", now - (5 * dayMs), "Client payment"));
                sampleTx.add(new Transaction("Whole Foods Market", 134.50, Transaction.TYPE_EXPENSE, "Groceries", now - (1 * dayMs), "Weekly groceries"));
                sampleTx.add(new Transaction("Italian Bistro", 68.20, Transaction.TYPE_EXPENSE, "Food & Dining", now - (2 * dayMs), "Dinner with family"));
                sampleTx.add(new Transaction("Electric & Gas Bill", 125.00, Transaction.TYPE_EXPENSE, "Bills & Utilities", now - (4 * dayMs), "Utility bill"));
                sampleTx.add(new Transaction("Gas Station", 48.00, Transaction.TYPE_EXPENSE, "Transport", now - (6 * dayMs), "Full tank"));
                sampleTx.add(new Transaction("Cinema Tickets & Snacks", 32.50, Transaction.TYPE_EXPENSE, "Entertainment", now - (8 * dayMs), "Movie night"));
                sampleTx.add(new Transaction("Pharmacy", 24.80, Transaction.TYPE_EXPENSE, "Health & Medical", now - (9 * dayMs), "Vitamins"));

                transactionDao.insertAll(sampleTx);

                mainHandler.post(() -> {
                    if (callback != null) callback.onSuccess(null);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e);
                });
            }
        });
    }
}
