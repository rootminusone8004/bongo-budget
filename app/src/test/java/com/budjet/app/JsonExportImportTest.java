package com.budjet.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.budjet.app.data.model.Budget;
import com.budjet.app.data.model.ExportData;
import com.budjet.app.data.model.Transaction;
import com.budjet.app.util.DateUtils;
import com.budjet.app.util.JsonUtils;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class JsonExportImportTest {

    @Test
    public void testJsonExportAndImportCycle() throws Exception {
        // Prepare sample budgets
        List<Budget> budgets = new ArrayList<>();
        budgets.add(new Budget(1, Budget.CATEGORY_OVERALL, 3000.0, "2026-09"));
        budgets.add(new Budget(2, "Food & Dining", 500.0, "2026-09"));
        budgets.add(new Budget(3, "Transport", 250.0, "2026-09"));

        // Prepare sample transactions
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(new Transaction(1, "Paycheck", 4200.0, Transaction.TYPE_INCOME, "Salary", 1789123456000L, "Main job"));
        transactions.add(new Transaction(2, "Supermarket", 124.50, Transaction.TYPE_EXPENSE, "Groceries", 1789123500000L, "Weekly food"));
        transactions.add(new Transaction(3, "Gas Station", 45.00, Transaction.TYPE_EXPENSE, "Transport", 1789123600000L, null));

        ExportData originalData = new ExportData(DateUtils.formatIsoNow(), budgets, transactions);

        // Serialize to stream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonUtils.writeJsonToStream(originalData, outputStream);
        String jsonString = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);

        assertNotNull(jsonString);
        assertTrue(jsonString.contains("Bongo Budget"));
        assertTrue(jsonString.contains("Supermarket"));
        assertTrue(jsonString.contains("Paycheck"));
        assertTrue(jsonString.contains("Food & Dining"));

        // Deserialize from stream
        ByteArrayInputStream inputStream = new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8));
        ExportData importedData = JsonUtils.fromJsonStream(inputStream);

        assertNotNull(importedData);
        assertEquals("Bongo Budget", importedData.getAppName());
        assertEquals(1, importedData.getVersion());
        assertEquals(3, importedData.getBudgets().size());
        assertEquals(3, importedData.getTransactions().size());

        // Verify content integrity
        Budget b0 = importedData.getBudgets().get(0);
        assertEquals(Budget.CATEGORY_OVERALL, b0.getCategory());
        assertEquals(3000.0, b0.getAmount(), 0.001);
        assertEquals("2026-09", b0.getMonthYear());

        Transaction t0 = importedData.getTransactions().get(0);
        assertEquals("Paycheck", t0.getTitle());
        assertEquals(4200.0, t0.getAmount(), 0.001);
        assertEquals(Transaction.TYPE_INCOME, t0.getType());
        assertTrue(t0.isIncome());

        Transaction t1 = importedData.getTransactions().get(1);
        assertEquals("Supermarket", t1.getTitle());
        assertEquals(124.50, t1.getAmount(), 0.001);
        assertEquals(Transaction.TYPE_EXPENSE, t1.getType());
        assertTrue(t1.isExpense());
    }

    @Test
    public void testEmptyExportData() {
        ExportData emptyData = new ExportData("2026-09-11T00:00:00Z", new ArrayList<>(), new ArrayList<>());
        String json = JsonUtils.toJson(emptyData);
        ExportData parsed = JsonUtils.fromJsonString(json);

        assertNotNull(parsed);
        assertEquals(0, parsed.getBudgets().size());
        assertEquals(0, parsed.getTransactions().size());
    }
}
