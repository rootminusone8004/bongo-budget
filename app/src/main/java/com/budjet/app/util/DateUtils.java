package com.budjet.app.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("MMM dd, yy", Locale.getDefault());
    private static final SimpleDateFormat MONTH_YEAR_FORMAT = new SimpleDateFormat("MMM yy", Locale.getDefault());
    private static final SimpleDateFormat MONTH_YEAR_KEY_FORMAT = new SimpleDateFormat("yyyy-MM", Locale.US);
    private static final SimpleDateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

    public static long getStartOfMonth(Calendar cal) {
        Calendar copy = (Calendar) cal.clone();
        copy.set(Calendar.DAY_OF_MONTH, 1);
        copy.set(Calendar.HOUR_OF_DAY, 0);
        copy.set(Calendar.MINUTE, 0);
        copy.set(Calendar.SECOND, 0);
        copy.set(Calendar.MILLISECOND, 0);
        return copy.getTimeInMillis();
    }

    public static long getEndOfMonth(Calendar cal) {
        Calendar copy = (Calendar) cal.clone();
        copy.set(Calendar.DAY_OF_MONTH, copy.getActualMaximum(Calendar.DAY_OF_MONTH));
        copy.set(Calendar.HOUR_OF_DAY, 23);
        copy.set(Calendar.MINUTE, 59);
        copy.set(Calendar.SECOND, 59);
        copy.set(Calendar.MILLISECOND, 999);
        return copy.getTimeInMillis();
    }

    public static String formatMonthYear(Calendar cal) {
        return MONTH_YEAR_FORMAT.format(cal.getTime());
    }

    public static String formatMonthYearKey(Calendar cal) {
        return MONTH_YEAR_KEY_FORMAT.format(cal.getTime());
    }

    public static String formatDate(long timestamp) {
        return DISPLAY_DATE_FORMAT.format(new Date(timestamp));
    }

    public static long getStartOfDay(Calendar cal) {
        Calendar copy = (Calendar) cal.clone();
        copy.set(Calendar.HOUR_OF_DAY, 0);
        copy.set(Calendar.MINUTE, 0);
        copy.set(Calendar.SECOND, 0);
        copy.set(Calendar.MILLISECOND, 0);
        return copy.getTimeInMillis();
    }

    public static long getEndOfDay(Calendar cal) {
        Calendar copy = (Calendar) cal.clone();
        copy.set(Calendar.HOUR_OF_DAY, 23);
        copy.set(Calendar.MINUTE, 59);
        copy.set(Calendar.SECOND, 59);
        copy.set(Calendar.MILLISECOND, 999);
        return copy.getTimeInMillis();
    }

    public static boolean isSameDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) return false;
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    public static String formatDayHeader(Calendar cal) {
        if (cal == null) return "";
        Calendar today = Calendar.getInstance();
        SimpleDateFormat dayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        if (isSameDay(cal, today)) {
            return "Today, " + dayFormat.format(cal.getTime());
        }
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(cal, yesterday)) {
            return "Yesterday, " + dayFormat.format(cal.getTime());
        }
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        if (isSameDay(cal, tomorrow)) {
            return "Tomorrow, " + dayFormat.format(cal.getTime());
        }
        return dayFormat.format(cal.getTime());
    }

    public static String formatIsoNow() {
        return ISO_DATE_FORMAT.format(new Date());
    }
}
