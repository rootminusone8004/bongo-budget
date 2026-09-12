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

    public Budget() {
    }

    @Ignore
    public Budget(String category, double amount, String monthYear) {
        this.category = category;
        this.amount = amount;
        this.monthYear = monthYear != null ? monthYear : DEFAULT_MONTHLY;
    }

    @Ignore
    public Budget(int id, String category, double amount, String monthYear) {
        this.id = id;
        this.category = category;
        this.amount = amount;
        this.monthYear = monthYear != null ? monthYear : DEFAULT_MONTHLY;
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
                Objects.equals(category, budget.category) &&
                Objects.equals(monthYear, budget.monthYear);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, category, amount, monthYear);
    }
}
