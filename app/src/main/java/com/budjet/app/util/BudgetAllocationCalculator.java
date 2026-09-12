package com.budjet.app.util;

import com.budjet.app.data.model.Budget;

import java.util.List;

public class BudgetAllocationCalculator {

    public static class AllocationSummary {
        public final double overallLimit;
        public final boolean hasOverallBudget;
        public final double totalCategoryAllocated;
        public final double unallocatedPool;
        public final int allocationPercentage;

        public AllocationSummary(double overallLimit, boolean hasOverallBudget,
                                 double totalCategoryAllocated, double unallocatedPool,
                                 int allocationPercentage) {
            this.overallLimit = overallLimit;
            this.hasOverallBudget = hasOverallBudget;
            this.totalCategoryAllocated = totalCategoryAllocated;
            this.unallocatedPool = unallocatedPool;
            this.allocationPercentage = allocationPercentage;
        }
    }

    public static class CategoryValidationResult {
        public final boolean isValid;
        public final String errorMessage;
        public final double maxAllowed;
        public final double remainingUnallocatedAfter;
        public final double otherCategoriesAllocated;
        public final double overallLimit;
        public final boolean hasOverallBudget;

        public CategoryValidationResult(boolean isValid, String errorMessage, double maxAllowed,
                                        double remainingUnallocatedAfter, double otherCategoriesAllocated,
                                        double overallLimit, boolean hasOverallBudget) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
            this.maxAllowed = maxAllowed;
            this.remainingUnallocatedAfter = remainingUnallocatedAfter;
            this.otherCategoriesAllocated = otherCategoriesAllocated;
            this.overallLimit = overallLimit;
            this.hasOverallBudget = hasOverallBudget;
        }
    }

    public static class OverallValidationResult {
        public final boolean isValid;
        public final String errorMessage;
        public final double minRequired;
        public final double remainingUnallocatedAfter;

        public OverallValidationResult(boolean isValid, String errorMessage,
                                       double minRequired, double remainingUnallocatedAfter) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
            this.minRequired = minRequired;
            this.remainingUnallocatedAfter = remainingUnallocatedAfter;
        }
    }

    public static AllocationSummary calculateSummary(Budget overallBudget, List<Budget> allBudgets) {
        boolean hasOverall = overallBudget != null && overallBudget.getAmount() > 0;
        double overallLimit = hasOverall ? overallBudget.getAmount() : 0.0;

        double categoryTotal = 0.0;
        if (allBudgets != null) {
            for (Budget b : allBudgets) {
                if (b != null && !b.isOverall()) {
                    categoryTotal += b.getAmount();
                }
            }
        }

        double unallocated = Math.max(0.0, overallLimit - categoryTotal);
        int percentage = overallLimit > 0 ? (int) Math.min(100, Math.round((categoryTotal / overallLimit) * 100)) : 0;

        return new AllocationSummary(overallLimit, hasOverall, categoryTotal, unallocated, percentage);
    }

    /**
     * Calculates the debit card style total balance:
     * (Monthly Budget + Total Income) - Total Expense
     */
    public static double calculateDebitBalance(double budgetAmount, double income, double expense) {
        return (budgetAmount + income) - expense;
    }

    public static CategoryValidationResult validateCategoryBudget(
            Budget overallBudget,
            List<Budget> allBudgets,
            String targetCategory,
            int currentBudgetId,
            double enteredAmount
    ) {
        if (overallBudget == null || overallBudget.getAmount() <= 0) {
            return new CategoryValidationResult(
                    false,
                    "Please set an Overall Budget first before allocating category quotas.",
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    false
            );
        }

        double overallLimit = overallBudget.getAmount();
        double otherCategoriesAllocated = 0.0;

        if (allBudgets != null) {
            for (Budget b : allBudgets) {
                if (b == null || b.isOverall()) continue;

                boolean isSame = false;
                if (currentBudgetId > 0 && b.getId() == currentBudgetId) {
                    isSame = true;
                } else if (targetCategory != null && targetCategory.equalsIgnoreCase(b.getCategory())) {
                    isSame = true;
                }

                if (!isSame) {
                    otherCategoriesAllocated += b.getAmount();
                }
            }
        }

        double maxAllowed = Math.max(0.0, overallLimit - otherCategoriesAllocated);

        if (enteredAmount <= 0) {
            return new CategoryValidationResult(
                    false,
                    "Amount must be greater than 0",
                    maxAllowed,
                    maxAllowed,
                    otherCategoriesAllocated,
                    overallLimit,
                    true
            );
        }

        if (enteredAmount - maxAllowed > 0.0001) {
            double exceeded = enteredAmount - maxAllowed;
            return new CategoryValidationResult(
                    false,
                    "Exceeds overall budget limit! Max available is " + CurrencyUtils.formatAmount(maxAllowed) + " (exceeded by " + CurrencyUtils.formatAmount(exceeded) + ")",
                    maxAllowed,
                    maxAllowed - enteredAmount,
                    otherCategoriesAllocated,
                    overallLimit,
                    true
            );
        }

        return new CategoryValidationResult(
                true,
                null,
                maxAllowed,
                maxAllowed - enteredAmount,
                otherCategoriesAllocated,
                overallLimit,
                true
        );
    }

    public static OverallValidationResult validateOverallBudget(
            List<Budget> allBudgets,
            double enteredAmount
    ) {
        double categoryTotal = 0.0;
        if (allBudgets != null) {
            for (Budget b : allBudgets) {
                if (b != null && !b.isOverall()) {
                    categoryTotal += b.getAmount();
                }
            }
        }

        if (enteredAmount <= 0) {
            return new OverallValidationResult(
                    false,
                    "Amount must be greater than 0",
                    categoryTotal,
                    0.0
            );
        }

        if (categoryTotal - enteredAmount > 0.0001) {
            double deficit = categoryTotal - enteredAmount;
            return new OverallValidationResult(
                    false,
                    "Overall budget cannot be less than already allocated category quotas (" + CurrencyUtils.formatAmount(categoryTotal) + "). Short by " + CurrencyUtils.formatAmount(deficit),
                    categoryTotal,
                    enteredAmount - categoryTotal
            );
        }

        return new OverallValidationResult(
                true,
                null,
                categoryTotal,
                enteredAmount - categoryTotal
        );
    }
}
