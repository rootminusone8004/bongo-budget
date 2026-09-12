package com.budjet.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.budjet.app.data.model.Budget;
import com.budjet.app.data.model.CategorySpending;
import com.budjet.app.data.model.Transaction;
import com.budjet.app.data.repository.BudgetRepository;
import com.budjet.app.util.DateUtils;
import com.budjet.app.util.SingleLiveEvent;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.List;

public class MainViewModel extends AndroidViewModel {

    public static class FilterCriteria {
        public final long startDate;
        public final long endDate;
        public final String type;
        public final String query;
        public final String monthYearKey;
        public final String monthYearDisplay;

        public FilterCriteria(long startDate, long endDate, String type, String query, String monthYearKey, String monthYearDisplay) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.type = type;
            this.query = query;
            this.monthYearKey = monthYearKey;
            this.monthYearDisplay = monthYearDisplay;
        }
    }

    private final BudgetRepository repository;

    private final Calendar currentCalendar;
    private final MutableLiveData<Calendar> calendarLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> filterTypeLiveData = new MutableLiveData<>("ALL");
    private final MutableLiveData<String> searchQueryLiveData = new MutableLiveData<>("");

    private final MutableLiveData<FilterCriteria> filterCriteriaLiveData = new MutableLiveData<>();

    private final LiveData<List<Transaction>> filteredTransactions;
    private final LiveData<List<Transaction>> recentTransactions;
    private final LiveData<Double> monthlyIncome;
    private final LiveData<Double> monthlyExpense;
    private final LiveData<List<CategorySpending>> categorySpending;
    private final LiveData<Budget> overallBudget;
    private final LiveData<List<Budget>> monthlyBudgets;
    private final LiveData<List<String>> allCreatedCategories;

    private final SingleLiveEvent<String> toastMessage = new SingleLiveEvent<>();
    private final SingleLiveEvent<BudgetRepository.ImportResult> importSuccessEvent = new SingleLiveEvent<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        this.repository = new BudgetRepository(application);

        this.currentCalendar = Calendar.getInstance();
        this.calendarLiveData.setValue(currentCalendar);

        this.filteredTransactions = Transformations.switchMap(filterCriteriaLiveData, criteria ->
                repository.getFilteredTransactions(criteria.startDate, criteria.endDate, criteria.type, criteria.query)
        );

        this.recentTransactions = repository.getRecentTransactions(5);

        this.monthlyIncome = Transformations.switchMap(filterCriteriaLiveData, criteria ->
                repository.getTotalIncome(criteria.startDate, criteria.endDate)
        );

        this.monthlyExpense = Transformations.switchMap(filterCriteriaLiveData, criteria ->
                repository.getTotalExpense(criteria.startDate, criteria.endDate)
        );

        this.categorySpending = Transformations.switchMap(filterCriteriaLiveData, criteria ->
                repository.getCategorySpending(criteria.startDate, criteria.endDate)
        );

        this.overallBudget = Transformations.switchMap(filterCriteriaLiveData, criteria ->
                repository.getOverallBudget(criteria.monthYearKey)
        );

        this.monthlyBudgets = Transformations.switchMap(filterCriteriaLiveData, criteria ->
                repository.getBudgetsForMonth(criteria.monthYearKey)
        );

        this.allCreatedCategories = repository.getAllDistinctCategories();

        updateFilterCriteria();
    }

    private void updateFilterCriteria() {
        long start = DateUtils.getStartOfMonth(currentCalendar);
        long end = DateUtils.getEndOfMonth(currentCalendar);
        String type = filterTypeLiveData.getValue() != null ? filterTypeLiveData.getValue() : "ALL";
        String query = searchQueryLiveData.getValue() != null ? searchQueryLiveData.getValue() : "";
        String key = DateUtils.formatMonthYearKey(currentCalendar);
        String display = DateUtils.formatMonthYear(currentCalendar);

        filterCriteriaLiveData.setValue(new FilterCriteria(start, end, type, query, key, display));
    }

    // Date navigation
    public void nextMonth() {
        currentCalendar.add(Calendar.MONTH, 1);
        calendarLiveData.setValue(currentCalendar);
        updateFilterCriteria();
    }

    public void previousMonth() {
        currentCalendar.add(Calendar.MONTH, -1);
        calendarLiveData.setValue(currentCalendar);
        updateFilterCriteria();
    }

    public void setCurrentMonth() {
        currentCalendar.setTimeInMillis(System.currentTimeMillis());
        calendarLiveData.setValue(currentCalendar);
        updateFilterCriteria();
    }

    public Calendar getCurrentCalendar() {
        return (Calendar) currentCalendar.clone();
    }

    public String getCurrentMonthYearKey() {
        return DateUtils.formatMonthYearKey(currentCalendar);
    }

    // Filter and search
    public void setFilterType(String type) {
        filterTypeLiveData.setValue(type);
        updateFilterCriteria();
    }

    public void setSearchQuery(String query) {
        searchQueryLiveData.setValue(query);
        updateFilterCriteria();
    }

    // Getters for LiveData
    public LiveData<FilterCriteria> getFilterCriteria() {
        return filterCriteriaLiveData;
    }

    public LiveData<List<Transaction>> getFilteredTransactions() {
        return filteredTransactions;
    }

    public LiveData<List<Transaction>> getRecentTransactions() {
        return recentTransactions;
    }

    public LiveData<List<Transaction>> getTransactionsForCategory(String category) {
        return Transformations.switchMap(filterCriteriaLiveData, criteria ->
                repository.getTransactionsByCategory(category, criteria.startDate, criteria.endDate)
        );
    }

    public LiveData<Double> getMonthlyIncome() {
        return monthlyIncome;
    }

    public LiveData<Double> getMonthlyExpense() {
        return monthlyExpense;
    }

    public LiveData<List<CategorySpending>> getCategorySpending() {
        return categorySpending;
    }

    public LiveData<Budget> getOverallBudget() {
        return overallBudget;
    }

    public LiveData<List<Budget>> getMonthlyBudgets() {
        return monthlyBudgets;
    }

    public LiveData<List<String>> getAllCreatedCategories() {
        return allCreatedCategories;
    }

    public LiveData<List<String>> getCreatedExpenseCategoriesLive() {
        return Transformations.map(monthlyBudgets, budgets -> {
            java.util.List<String> list = new java.util.ArrayList<>();
            if (budgets != null) {
                for (Budget b : budgets) {
                    if (b != null && !b.isOverall() && !list.contains(b.getCategory())) {
                        list.add(b.getCategory());
                    }
                }
            }
            return list;
        });
    }

    public boolean hasCreatedCategoriesInCurrentMonth() {
        List<Budget> budgets = monthlyBudgets.getValue();
        if (budgets != null) {
            for (Budget b : budgets) {
                if (b != null && !b.isOverall()) {
                    return true;
                }
            }
        }
        return false;
    }

    public SingleLiveEvent<String> getToastMessage() {
        return toastMessage;
    }

    public SingleLiveEvent<BudgetRepository.ImportResult> getImportSuccessEvent() {
        return importSuccessEvent;
    }

    // Transaction Actions
    public void addTransaction(Transaction transaction) {
        repository.insertTransaction(transaction);
        toastMessage.setValue("Transaction added");
    }

    public void updateTransaction(Transaction transaction) {
        repository.updateTransaction(transaction);
        toastMessage.setValue("Transaction updated");
    }

    public void deleteTransaction(Transaction transaction) {
        repository.deleteTransaction(transaction);
        toastMessage.setValue("Transaction deleted");
    }

    // Budget Actions
    public void saveBudget(Budget budget) {
        repository.insertBudget(budget);
        toastMessage.setValue("Budget saved");
    }

    public void deleteBudget(Budget budget) {
        repository.deleteBudget(budget);
        toastMessage.setValue("Budget deleted");
    }

    // Backup Export and Import
    public void exportDataToStream(OutputStream outputStream) {
        repository.exportDataToStream(outputStream, new BudgetRepository.RepositoryCallback<String>() {
            @Override
            public void onSuccess(String result) {
                toastMessage.setValue("Backup exported successfully!");
            }

            @Override
            public void onError(Exception e) {
                toastMessage.setValue("Export failed: " + e.getMessage());
            }
        });
    }

    public void exportDataToString(BudgetRepository.RepositoryCallback<String> callback) {
        repository.exportDataToString(callback);
    }

    public void importDataFromStream(InputStream inputStream, boolean overwrite) {
        repository.importDataFromStream(inputStream, overwrite, new BudgetRepository.RepositoryCallback<BudgetRepository.ImportResult>() {
            @Override
            public void onSuccess(BudgetRepository.ImportResult result) {
                updateFilterCriteria();
                importSuccessEvent.setValue(result);
            }

            @Override
            public void onError(Exception e) {
                toastMessage.setValue("Import failed: " + e.getMessage());
            }
        });
    }

    public void populateSampleData() {
        repository.populateSampleData(new BudgetRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                updateFilterCriteria();
                toastMessage.setValue("Sample data loaded!");
            }

            @Override
            public void onError(Exception e) {
                toastMessage.setValue("Failed to load sample data: " + e.getMessage());
            }
        });
    }

    public void clearAllData() {
        repository.clearAllData(new BudgetRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                updateFilterCriteria();
                toastMessage.setValue("All data reset successfully.");
            }

            @Override
            public void onError(Exception e) {
                toastMessage.setValue("Failed to clear data: " + e.getMessage());
            }
        });
    }
}
