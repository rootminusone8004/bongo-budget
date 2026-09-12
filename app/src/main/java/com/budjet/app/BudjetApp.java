package com.budjet.app;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

public class BudjetApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }
}
