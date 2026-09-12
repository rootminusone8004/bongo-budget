package com.budjet.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.budjet.app.data.model.Budget;
import com.budjet.app.util.BudgetAllocationCalculator;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class BudgetAllocationCalculatorTest {

    @Test
    public void testSummaryCalculation() {
        Budget overall = new Budget(1, Budget.CATEGORY_OVERALL, 20000.0, "2026-09");
        List<Budget> budgets = new ArrayList<>();
        budgets.add(overall);
        budgets.add(new Budget(2, "Food & Dining", 5000.0, "2026-09"));
        budgets.add(new Budget(3, "Transport", 3000.0, "2026-09"));

        BudgetAllocationCalculator.AllocationSummary summary =
                BudgetAllocationCalculator.calculateSummary(overall, budgets);

        assertTrue(summary.hasOverallBudget);
        assertEquals(20000.0, summary.overallLimit, 0.001);
        assertEquals(8000.0, summary.totalCategoryAllocated, 0.001);
        assertEquals(12000.0, summary.unallocatedPool, 0.001);
        assertEquals(40, summary.allocationPercentage); // 8000 / 20000 = 40%
    }

    @Test
    public void testCategoryBudgetValidation_WithinAvailableLimit() {
        Budget overall = new Budget(1, Budget.CATEGORY_OVERALL, 20000.0, "2026-09");
        List<Budget> budgets = new ArrayList<>();
        budgets.add(overall);
        budgets.add(new Budget(2, "Food & Dining", 5000.0, "2026-09"));
        budgets.add(new Budget(3, "Transport", 3000.0, "2026-09"));

        // Other categories sum to 8000. Max available is 12000.
        // User wants to allocate 4000 to "Groceries"
        BudgetAllocationCalculator.CategoryValidationResult result =
                BudgetAllocationCalculator.validateCategoryBudget(overall, budgets, "Groceries", 0, 4000.0);

        assertTrue(result.isValid);
        assertNull(result.errorMessage);
        assertEquals(12000.0, result.maxAllowed, 0.001);
        assertEquals(8000.0, result.remainingUnallocatedAfter, 0.001);
    }

    @Test
    public void testCategoryBudgetValidation_ExceedsLimit() {
        Budget overall = new Budget(1, Budget.CATEGORY_OVERALL, 20000.0, "2026-09");
        List<Budget> budgets = new ArrayList<>();
        budgets.add(overall);
        budgets.add(new Budget(2, "Food & Dining", 5000.0, "2026-09"));
        budgets.add(new Budget(3, "Transport", 3000.0, "2026-09"));

        // Max available is 12000. User enters 13000.
        BudgetAllocationCalculator.CategoryValidationResult result =
                BudgetAllocationCalculator.validateCategoryBudget(overall, budgets, "Groceries", 0, 13000.0);

        assertFalse(result.isValid);
        assertNotNull(result.errorMessage);
        assertTrue(result.errorMessage.contains("Exceeds"));
        assertEquals(12000.0, result.maxAllowed, 0.001);
        assertEquals(-1000.0, result.remainingUnallocatedAfter, 0.001);
    }

    @Test
    public void testCategoryBudgetValidation_EditingExistingBudget() {
        Budget overall = new Budget(1, Budget.CATEGORY_OVERALL, 20000.0, "2026-09");
        Budget food = new Budget(2, "Food & Dining", 5000.0, "2026-09");
        Budget transport = new Budget(3, "Transport", 3000.0, "2026-09");

        List<Budget> budgets = new ArrayList<>();
        budgets.add(overall);
        budgets.add(food);
        budgets.add(transport);

        // User is editing Food (id = 2). Other category is Transport (3000).
        // Max available for Food should be 20000 - 3000 = 17000!
        BudgetAllocationCalculator.CategoryValidationResult result =
                BudgetAllocationCalculator.validateCategoryBudget(overall, budgets, "Food & Dining", 2, 7000.0);

        assertTrue(result.isValid);
        assertNull(result.errorMessage);
        assertEquals(17000.0, result.maxAllowed, 0.001);
        assertEquals(10000.0, result.remainingUnallocatedAfter, 0.001); // 17000 - 7000 = 10000 unallocated

        // If user tries 18000 on Food
        BudgetAllocationCalculator.CategoryValidationResult resultExceeded =
                BudgetAllocationCalculator.validateCategoryBudget(overall, budgets, "Food & Dining", 2, 18000.0);

        assertFalse(resultExceeded.isValid);
        assertTrue(resultExceeded.errorMessage.contains("Exceeds"));
    }

    @Test
    public void testCategoryBudgetValidation_NoOverallBudget() {
        List<Budget> budgets = new ArrayList<>();

        BudgetAllocationCalculator.CategoryValidationResult result =
                BudgetAllocationCalculator.validateCategoryBudget(null, budgets, "Food & Dining", 0, 1000.0);

        assertFalse(result.isValid);
        assertFalse(result.hasOverallBudget);
        assertTrue(result.errorMessage.contains("Overall Budget first"));
    }

    @Test
    public void testOverallBudgetValidation_LessThanAllocatedCategories() {
        List<Budget> budgets = new ArrayList<>();
        budgets.add(new Budget(2, "Food & Dining", 5000.0, "2026-09"));
        budgets.add(new Budget(3, "Transport", 3000.0, "2026-09"));

        // Categories sum to 8000. User enters 7000 for Overall Budget.
        BudgetAllocationCalculator.OverallValidationResult result =
                BudgetAllocationCalculator.validateOverallBudget(budgets, 7000.0);

        assertFalse(result.isValid);
        assertNotNull(result.errorMessage);
        assertTrue(result.errorMessage.contains("cannot be less"));
        assertEquals(8000.0, result.minRequired, 0.001);
    }

    @Test
    public void testOverallBudgetValidation_ValidAmount() {
        List<Budget> budgets = new ArrayList<>();
        budgets.add(new Budget(2, "Food & Dining", 5000.0, "2026-09"));
        budgets.add(new Budget(3, "Transport", 3000.0, "2026-09"));

        // User enters 10000 for Overall Budget.
        BudgetAllocationCalculator.OverallValidationResult result =
                BudgetAllocationCalculator.validateOverallBudget(budgets, 10000.0);

        assertTrue(result.isValid);
        assertNull(result.errorMessage);
        assertEquals(8000.0, result.minRequired, 0.001);
        assertEquals(2000.0, result.remainingUnallocatedAfter, 0.001);
    }

    @Test
    public void testDebitBalanceCalculation_Normal() {
        // Budget = 25,000, Income = 5,000, Expense = 8,000 -> 22,000
        double balance = BudgetAllocationCalculator.calculateDebitBalance(25000.0, 5000.0, 8000.0);
        assertEquals(22000.0, balance, 0.001);
    }

    @Test
    public void testDebitBalanceCalculation_NoBudget() {
        // Budget = 0, Income = 10,000, Expense = 3,000 -> 7,000
        double balance = BudgetAllocationCalculator.calculateDebitBalance(0.0, 10000.0, 3000.0);
        assertEquals(7000.0, balance, 0.001);
    }

    @Test
    public void testDebitBalanceCalculation_Overdrawn() {
        // Budget = 20,000, Income = 0, Expense = 25,000 -> -5,000
        double balance = BudgetAllocationCalculator.calculateDebitBalance(20000.0, 0.0, 25000.0);
        assertEquals(-5000.0, balance, 0.001);
    }
}
