package com.budjet.app.data.model;

public class CategorySpending {
    private String category;
    private double totalSpent;
    private int transactionCount;

    public CategorySpending(String category, double totalSpent, int transactionCount) {
        this.category = category;
        this.totalSpent = totalSpent;
        this.transactionCount = transactionCount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(double totalSpent) {
        this.totalSpent = totalSpent;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }
}
