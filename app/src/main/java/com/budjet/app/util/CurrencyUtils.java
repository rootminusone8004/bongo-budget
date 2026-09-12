package com.budjet.app.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CurrencyUtils {

    public static final String CURRENCY_SYMBOL = "৳";
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.US));

    public static String formatAmount(double amount) {
        return CURRENCY_SYMBOL + NUMBER_FORMAT.format(amount);
    }

    public static String formatTransactionAmount(double amount, boolean isExpense) {
        String formatted = NUMBER_FORMAT.format(amount);
        return (isExpense ? "-" : "+") + CURRENCY_SYMBOL + formatted;
    }
}
