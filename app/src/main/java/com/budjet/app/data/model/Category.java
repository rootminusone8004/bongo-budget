package com.budjet.app.data.model;

import com.budjet.app.R;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Category {

    public static final String CUSTOM_CATEGORY_OPTION = "+ Custom Category...";

    public static final List<String> EXPENSE_PRESET_CANDIDATES = Arrays.asList(
            "Food",
            "Travel",
            "Grocery",
            "Shopping",
            "Entertainment",
            "Transport",
            "Bills & Utilities",
            "Health & Medical",
            "Education",
            "Personal Care",
            "Housing & Rent",
            "Subscriptions",
            "Gifts & Donations",
            "Other Expense"
    );

    public static final List<String> EXPENSE_CATEGORIES = EXPENSE_PRESET_CANDIDATES;

    public static final List<String> INCOME_CATEGORIES = Arrays.asList(
            "Salary",
            "Business",
            "Freelance",
            "Investments",
            "Gifts",
            "Other Income"
    );

    public static int getIconResource(String categoryName) {
        if (categoryName == null) return R.drawable.ic_category;
        String lower = categoryName.toLowerCase(Locale.ROOT);
        if (lower.contains("food") || lower.contains("dining") || lower.contains("restaurant") || lower.contains("cafe")) {
            return R.drawable.ic_restaurant;
        } else if (lower.contains("travel") || lower.contains("flight") || lower.contains("hotel") || lower.contains("vacation")) {
            return R.drawable.ic_flight;
        } else if (lower.contains("groc") || lower.contains("supermarket") || lower.contains("market")) {
            return R.drawable.ic_shopping_cart;
        } else if (lower.contains("transport") || lower.contains("car") || lower.contains("fuel") || lower.contains("gas") || lower.contains("taxi")) {
            return R.drawable.ic_directions_car;
        } else if (lower.contains("shop") || lower.contains("cloth") || lower.contains("mall")) {
            return R.drawable.ic_shopping_bag;
        } else if (lower.contains("entertain") || lower.contains("movie") || lower.contains("cinema") || lower.contains("game") || lower.contains("gaming")) {
            return R.drawable.ic_movie;
        } else if (lower.contains("gym") || lower.contains("fit") || lower.contains("sport") || lower.contains("workout")) {
            return R.drawable.ic_trending_up;
        } else if (lower.contains("bill") || lower.contains("utilit") || lower.contains("electri") || lower.contains("water") || lower.contains("rent") || lower.contains("hous")) {
            return R.drawable.ic_receipt;
        } else if (lower.contains("health") || lower.contains("medic") || lower.contains("doctor") || lower.contains("pharm")) {
            return R.drawable.ic_medical;
        } else if (lower.contains("educat") || lower.contains("school") || lower.contains("course") || lower.contains("book")) {
            return R.drawable.ic_school;
        } else if (lower.contains("salar") || lower.contains("paycheck") || lower.contains("wage")) {
            return R.drawable.ic_attach_money;
        } else if (lower.contains("invest") || lower.contains("stock") || lower.contains("crypto")) {
            return R.drawable.ic_trending_up;
        } else if (lower.contains("business") || lower.contains("client")) {
            return R.drawable.ic_business;
        }
        return R.drawable.ic_category;
    }

    public static int getColorResource(String categoryName) {
        if (categoryName == null) return R.color.cat_other;
        String lower = categoryName.toLowerCase(Locale.ROOT);
        if (lower.contains("food") || lower.contains("dining") || lower.contains("cafe")) {
            return R.color.cat_food;
        } else if (lower.contains("travel")) {
            return R.color.cat_transport;
        } else if (lower.contains("groc")) {
            return R.color.cat_groceries;
        } else if (lower.contains("transport") || lower.contains("car") || lower.contains("fuel") || lower.contains("gas")) {
            return R.color.cat_transport;
        } else if (lower.contains("shop")) {
            return R.color.cat_shopping;
        } else if (lower.contains("entertain")) {
            return R.color.cat_entertainment;
        } else if (lower.contains("bill") || lower.contains("utilit") || lower.contains("rent")) {
            return R.color.cat_bills;
        } else if (lower.contains("health") || lower.contains("medic")) {
            return R.color.cat_health;
        } else if (lower.contains("educat")) {
            return R.color.cat_education;
        } else if (lower.contains("salar")) {
            return R.color.cat_salary;
        } else if (lower.contains("invest") || lower.contains("business")) {
            return R.color.cat_investment;
        }
        return R.color.cat_other;
    }
}
