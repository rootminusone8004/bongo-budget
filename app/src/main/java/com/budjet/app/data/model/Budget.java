package com.budjet.app.data.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.Objects;

@Entity(tableName = "budgets")
public class Budget implements Serializable {

    public static final String CATEGORY_OVERALL = "OVERALL";
    public static final String DEFAULT_MONTHLY = "MONTHLY";

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String category; // "OVERALL" for total budget or specific category name
    private double amount;   // Budget limit amount
    private String monthYear; // e.g. "2026-09" or "MONTHLY"
    private boolean showDailyBudget = true; // Whether category shows daily budget limit
    private long lastModified = System.currentTimeMillis();

    public Budget() {
        this.showDailyBudget = true;
        this.lastModified = System.currentTimeMillis();
    }

    @Ignore
    public Budget(String category, double amount, String monthYear) {
        this(category, amount, monthYear, true);
    }

    @Ignore
    public Budget(String category, double amount, String monthYear, boolean showDailyBudget) {
        this.category = category;
        this.amount = amount;
        this.monthYear = monthYear != null ? monthYear : DEFAULT_MONTHLY;
        this.showDailyBudget = showDailyBudget;
        this.lastModified = System.currentTimeMillis();
    }

    @Ignore
    public Budget(int id, String category, double amount, String monthYear) {
        this(id, category, amount, monthYear, true);
    }

    @Ignore
    public Budget(int id, String category, double amount, String monthYear, boolean showDailyBudget) {
        this.id = id;
        this.category = category;
        this.amount = amount;
        this.monthYear = monthYear != null ? monthYear : DEFAULT_MONTHLY;
        this.showDailyBudget = showDailyBudget;
        this.lastModified = System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getMonthYear() {
        return monthYear;
    }

    public void setMonthYear(String monthYear) {
        this.monthYear = monthYear;
    }

    public boolean isShowDailyBudget() {
        return showDailyBudget;
    }

    public void setShowDailyBudget(boolean showDailyBudget) {
        this.showDailyBudget = showDailyBudget;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public boolean isOverall() {
        return CATEGORY_OVERALL.equalsIgnoreCase(category);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Budget budget = (Budget) o;
        return id == budget.id &&
                Double.compare(budget.amount, amount) == 0 &&
                showDailyBudget == budget.showDailyBudget &&
                Objects.equals(category, budget.category) &&
                Objects.equals(monthYear, budget.monthYear);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, category, amount, monthYear, showDailyBudget);
    }
}
