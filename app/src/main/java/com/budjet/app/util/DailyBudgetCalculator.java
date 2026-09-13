package com.budjet.app.util;

import java.util.Calendar;

public class DailyBudgetCalculator {

    public static class DailyBudgetInfo {
        public final double limit;
        public final double spent;
        public final double remaining;
        public final int totalDaysInMonth;
        public final int remainingDays;
        public final double dailySpendable;
        public final double initialDailyAllowance;
        public final boolean isOverBudget;
        public final boolean isMonthEnded;

        public DailyBudgetInfo(double limit, double spent, double remaining,
                               int totalDaysInMonth, int remainingDays,
                               double dailySpendable, double initialDailyAllowance,
                               boolean isOverBudget, boolean isMonthEnded) {
            this.limit = limit;
            this.spent = spent;
            this.remaining = remaining;
            this.totalDaysInMonth = totalDaysInMonth;
            this.remainingDays = remainingDays;
            this.dailySpendable = dailySpendable;
            this.initialDailyAllowance = initialDailyAllowance;
            this.isOverBudget = isOverBudget;
            this.isMonthEnded = isMonthEnded;
        }

        public String getDailyFormatted() {
            if (isMonthEnded) {
                return "Month ended";
            }
            if (isOverBudget) {
                return CurrencyUtils.CURRENCY_SYMBOL + "0.00 / day";
            }
            return CurrencyUtils.formatAmount(dailySpendable) + " / day";
        }

        public String getDailyFormattedAmount() {
            if (isMonthEnded) {
                return "Month ended";
            }
            if (isOverBudget) {
                return CurrencyUtils.CURRENCY_SYMBOL + "0.00";
            }
            return CurrencyUtils.formatAmount(dailySpendable);
        }

        public String getDailySubtext() {
            if (isMonthEnded) {
                return "Final average: " + CurrencyUtils.formatAmount(totalDaysInMonth > 0 ? spent / totalDaysInMonth : 0) + "/day";
            }
            if (isOverBudget) {
                return "Quota exceeded by " + CurrencyUtils.formatAmount(Math.abs(remaining));
            }
            return CurrencyUtils.formatAmount(remaining) + " left over " + remainingDays + " day" + (remainingDays == 1 ? "" : "s");
        }

        public String getPlannedDailyText() {
            return "Planned: " + CurrencyUtils.formatAmount(initialDailyAllowance) + "/day";
        }
    }

    public static DailyBudgetInfo calculate(double limit, double spent, Calendar selectedMonth) {
        Calendar now = Calendar.getInstance();
        Calendar cal = selectedMonth != null ? (Calendar) selectedMonth.clone() : Calendar.getInstance();

        int selYear = cal.get(Calendar.YEAR);
        int selMonth = cal.get(Calendar.MONTH);
        int nowYear = now.get(Calendar.YEAR);
        int nowMonth = now.get(Calendar.MONTH);
        int totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        int remainingDays;
        boolean isMonthEnded = false;

        if (selYear < nowYear || (selYear == nowYear && selMonth < nowMonth)) {
            remainingDays = 0;
            isMonthEnded = true;
        } else if (selYear > nowYear || (selYear == nowYear && selMonth > nowMonth)) {
            remainingDays = totalDays;
        } else {
            int currentDay = now.get(Calendar.DAY_OF_MONTH);
            remainingDays = Math.max(1, (totalDays - currentDay) + 1);
        }

        double remaining = limit - spent;
        boolean isOverBudget = remaining < 0;
        double initialDaily = totalDays > 0 ? (limit / totalDays) : 0;
        double dailySpendable = 0.0;

        if (!isMonthEnded && remainingDays > 0) {
            if (!isOverBudget) {
                dailySpendable = remaining / remainingDays;
            } else {
                dailySpendable = 0.0;
            }
        }

        return new DailyBudgetInfo(limit, spent, remaining, totalDays, remainingDays, dailySpendable, initialDaily, isOverBudget, isMonthEnded);
    }
}
