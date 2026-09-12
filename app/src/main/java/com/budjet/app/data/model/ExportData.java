package com.budjet.app.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ExportData implements Serializable {

    @SerializedName("appName")
    private String appName = "Bongo Budget";

    @SerializedName("version")
    private int version = 1;

    @SerializedName("exportDate")
    private String exportDate;

    @SerializedName("budgets")
    private List<Budget> budgets = new ArrayList<>();

    @SerializedName("transactions")
    private List<Transaction> transactions = new ArrayList<>();

    public ExportData() {
    }

    public ExportData(String exportDate, List<Budget> budgets, List<Transaction> transactions) {
        this.appName = "Bongo Budget";
        this.version = 1;
        this.exportDate = exportDate;
        this.budgets = budgets != null ? budgets : new ArrayList<>();
        this.transactions = transactions != null ? transactions : new ArrayList<>();
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getExportDate() {
        return exportDate;
    }

    public void setExportDate(String exportDate) {
        this.exportDate = exportDate;
    }

    public List<Budget> getBudgets() {
        return budgets;
    }

    public void setBudgets(List<Budget> budgets) {
        this.budgets = budgets;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }
}
