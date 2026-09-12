package com.budjet.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.budjet.app.util.DailyBudgetCalculator;

import org.junit.Test;

import java.util.Calendar;

public class DailyBudgetCalculatorTest {

    @Test
    public void testDailySpendableNormal() {
        Calendar cal = Calendar.getInstance();
        int totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int currentDay = cal.get(Calendar.DAY_OF_MONTH);
        int expectedRemainingDays = (totalDays - currentDay) + 1;

        double limit = 600.0;
        double spent = 150.0;
        double remaining = 450.0;

        DailyBudgetCalculator.DailyBudgetInfo info = DailyBudgetCalculator.calculate(limit, spent, cal);

        assertEquals(limit, info.limit, 0.001);
        assertEquals(spent, info.spent, 0.001);
        assertEquals(remaining, info.remaining, 0.001);
        assertEquals(expectedRemainingDays, info.remainingDays);
        assertFalse(info.isOverBudget);
        assertFalse(info.isMonthEnded);

        double expectedDaily = remaining / expectedRemainingDays;
        assertEquals(expectedDaily, info.dailySpendable, 0.001);
        assertTrue(info.getDailyFormatted().contains("/ day"));
    }

    @Test
    public void testDailySpendableExceeded() {
        Calendar cal = Calendar.getInstance();
        double limit = 300.0;
        double spent = 350.0;

        DailyBudgetCalculator.DailyBudgetInfo info = DailyBudgetCalculator.calculate(limit, spent, cal);

        assertTrue(info.isOverBudget);
        assertEquals(0.0, info.dailySpendable, 0.001);
        assertEquals(com.budjet.app.util.CurrencyUtils.CURRENCY_SYMBOL + "0.00 / day", info.getDailyFormatted());
        assertTrue(info.getDailySubtext().contains("exceeded"));
    }

    @Test
    public void testDailySpendablePastMonth() {
        Calendar past = Calendar.getInstance();
        past.add(Calendar.MONTH, -2); // 2 months ago

        DailyBudgetCalculator.DailyBudgetInfo info = DailyBudgetCalculator.calculate(500.0, 400.0, past);

        assertTrue(info.isMonthEnded);
        assertEquals(0, info.remainingDays);
        assertEquals("Month ended", info.getDailyFormatted());
    }
}
