package com.budjet.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.budjet.app.data.model.Budget;
import com.budjet.app.data.model.Transaction;
import com.budjet.app.util.DateUtils;

import org.junit.Test;

import java.util.Calendar;

public class ModelTest {

    @Test
    public void testTransactionTypes() {
        Transaction expense = new Transaction("Coffee", 4.50, Transaction.TYPE_EXPENSE, "Food & Dining", System.currentTimeMillis(), "Morning latte");
        assertTrue(expense.isExpense());
        assertFalse(expense.isIncome());

        Transaction income = new Transaction("Dividends", 75.00, Transaction.TYPE_INCOME, "Investments", System.currentTimeMillis(), "Q3 dividend");
        assertTrue(income.isIncome());
        assertFalse(income.isExpense());
    }

    @Test
    public void testBudgetProperties() {
        Budget overall = new Budget(Budget.CATEGORY_OVERALL, 2000.0, "2026-09");
        assertTrue(overall.isOverall());
        assertEquals(2000.0, overall.getAmount(), 0.001);

        Budget category = new Budget("Groceries", 400.0, "2026-09");
        assertFalse(category.isOverall());
        assertEquals("Groceries", category.getCategory());
    }

    @Test
    public void testDateUtilsMonthRange() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.SEPTEMBER, 11, 14, 30, 0);

        long start = DateUtils.getStartOfMonth(cal);
        long end = DateUtils.getEndOfMonth(cal);

        assertTrue(start < end);

        Calendar startCal = Calendar.getInstance();
        startCal.setTimeInMillis(start);
        assertEquals(1, startCal.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, startCal.get(Calendar.MINUTE));

        Calendar endCal = Calendar.getInstance();
        endCal.setTimeInMillis(end);
        assertEquals(30, endCal.get(Calendar.DAY_OF_MONTH)); // September has 30 days
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, endCal.get(Calendar.MINUTE));
    }

    @Test
    public void testCategoryListDoesNotContainOverall() {
        assertFalse(com.budjet.app.data.model.Category.EXPENSE_PRESET_CANDIDATES.contains(Budget.CATEGORY_OVERALL));
        assertFalse(com.budjet.app.data.model.Category.EXPENSE_CATEGORIES.contains(Budget.CATEGORY_OVERALL));
        assertFalse(com.budjet.app.data.model.Category.INCOME_CATEGORIES.contains(Budget.CATEGORY_OVERALL));
    }

    @Test
    public void testCustomCategoryResolution() {
        assertEquals("+ Custom Category...", com.budjet.app.data.model.Category.CUSTOM_CATEGORY_OPTION);

        // Keyword matching for custom categories
        int gymIcon = com.budjet.app.data.model.Category.getIconResource("Gym & Fitness");
        assertEquals(R.drawable.ic_trending_up, gymIcon);

        int gamingIcon = com.budjet.app.data.model.Category.getIconResource("Gaming & Esports");
        assertEquals(R.drawable.ic_movie, gamingIcon);

        int healthIcon = com.budjet.app.data.model.Category.getIconResource("Health & Medical");
        assertEquals(R.drawable.ic_medical, healthIcon);

        int randomIcon = com.budjet.app.data.model.Category.getIconResource("My Unique Custom Category");
        assertEquals(R.drawable.ic_category, randomIcon);
    }

    @Test
    public void testDateUtilsDayRange() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.SEPTEMBER, 13, 15, 45, 30);

        long start = DateUtils.getStartOfDay(cal);
        long end = DateUtils.getEndOfDay(cal);

        assertTrue(start < end);

        Calendar startCal = Calendar.getInstance();
        startCal.setTimeInMillis(start);
        assertEquals(13, startCal.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, startCal.get(Calendar.MINUTE));
        assertEquals(0, startCal.get(Calendar.SECOND));

        Calendar endCal = Calendar.getInstance();
        endCal.setTimeInMillis(end);
        assertEquals(13, endCal.get(Calendar.DAY_OF_MONTH));
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, endCal.get(Calendar.MINUTE));
        assertEquals(59, endCal.get(Calendar.SECOND));
    }

    @Test
    public void testDateUtilsFormatDayHeader() {
        Calendar today = Calendar.getInstance();
        assertTrue(DateUtils.formatDayHeader(today).startsWith("Today, "));

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        assertTrue(DateUtils.formatDayHeader(yesterday).startsWith("Yesterday, "));

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        assertTrue(DateUtils.formatDayHeader(tomorrow).startsWith("Tomorrow, "));
    }
}
