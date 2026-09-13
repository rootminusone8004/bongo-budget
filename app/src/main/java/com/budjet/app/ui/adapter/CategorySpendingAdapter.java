package com.budjet.app.ui.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.budjet.app.R;
import com.budjet.app.data.model.Budget;
import com.budjet.app.data.model.Category;
import com.budjet.app.data.model.CategorySpending;
import com.budjet.app.databinding.ItemCategorySpendingBinding;
import com.budjet.app.util.CurrencyUtils;
import com.budjet.app.util.DailyBudgetCalculator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategorySpendingAdapter extends RecyclerView.Adapter<CategorySpendingAdapter.ViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(String category);
    }

    private List<CategorySpending> items = new ArrayList<>();
    private Map<String, Double> categoryBudgetMap = new HashMap<>();
    private Map<String, Double> categoryTodaySpentMap = new HashMap<>();
    private double totalExpense = 0.0;
    private Calendar currentCalendar = Calendar.getInstance();
    private boolean isDailyMode = true; // Daily mode by default
    private OnCategoryClickListener listener;

    public CategorySpendingAdapter() {
        this(null);
    }

    public CategorySpendingAdapter(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<CategorySpending> list,
                        List<Budget> budgets,
                        Map<String, Double> todaySpentMap,
                        double totalExpense,
                        Calendar calendar,
                        boolean isDailyMode) {
        this.items = list != null ? list : new ArrayList<>();
        this.categoryBudgetMap = new HashMap<>();
        if (budgets != null) {
            for (Budget b : budgets) {
                if (b != null && !b.isOverall()) {
                    categoryBudgetMap.put(b.getCategory(), b.getAmount());
                }
            }
        }
        this.categoryTodaySpentMap = todaySpentMap != null ? todaySpentMap : new HashMap<>();
        this.totalExpense = totalExpense;
        this.currentCalendar = calendar != null ? (Calendar) calendar.clone() : Calendar.getInstance();
        this.isDailyMode = isDailyMode;
        notifyDataSetChanged();
    }

    public void setDailyMode(boolean dailyMode) {
        if (this.isDailyMode != dailyMode) {
            this.isDailyMode = dailyMode;
            notifyDataSetChanged();
        }
    }

    public boolean isDailyMode() {
        return isDailyMode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCategorySpendingBinding binding = ItemCategorySpendingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCategorySpendingBinding binding;

        ViewHolder(ItemCategorySpendingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CategorySpending item) {
            Context context = itemView.getContext();
            String category = item.getCategory();
            double monthlySpent = item.getTotalSpent();

            String title = category + " (" + item.getTransactionCount() + ")";
            binding.tvCategoryTitle.setText(title);

            int iconRes = Category.getIconResource(category);
            int colorRes = Category.getColorResource(category);
            int resolvedColor = ContextCompat.getColor(context, colorRes);

            binding.ivCatIcon.setImageResource(iconRes);
            binding.ivCatIcon.setImageTintList(ColorStateList.valueOf(resolvedColor));
            binding.iconContainer.getBackground().mutate().setTint(
                    (resolvedColor & 0x00FFFFFF) | 0x22000000
            );

            Double budgetLimitObj = categoryBudgetMap.get(category);
            double limit = budgetLimitObj != null ? budgetLimitObj : 0.0;

            Double todaySpentObj = categoryTodaySpentMap.get(category);
            double todaySpent = todaySpentObj != null ? todaySpentObj : 0.0;

            if (isDailyMode && limit > 0) {
                // Calculate daily budget info
                DailyBudgetCalculator.DailyBudgetInfo dailyInfo = DailyBudgetCalculator.calculate(
                        limit, monthlySpent, currentCalendar
                );

                if (dailyInfo.isMonthEnded) {
                    binding.tvCategorySubtitle.setText("Spent: " + CurrencyUtils.formatAmount(monthlySpent) + " of " + CurrencyUtils.formatAmount(limit));
                    binding.tvCategoryAmount.setText(CurrencyUtils.formatAmount(0.0));
                    binding.tvCategoryAmount.setTextColor(ContextCompat.getColor(context, R.color.text_muted));
                    binding.tvCategoryStatus.setText("Month ended");
                    binding.tvCategoryStatus.setTextColor(ContextCompat.getColor(context, R.color.text_muted));
                    binding.progressCategory.setIndicatorColor(ContextCompat.getColor(context, R.color.text_muted));
                    binding.progressCategory.setProgress(0);
                } else {
                    int remDays = dailyInfo.remainingDays > 0 ? dailyInfo.remainingDays : 1;
                    double previousSpent = Math.max(0.0, monthlySpent - todaySpent);
                    double availableBudget = limit - previousSpent;
                    double dailyAllowance = availableBudget > 0 ? (availableBudget / remDays) : 0.0;
                    double dailyRemaining = dailyAllowance - todaySpent;

                    boolean isOverLimit = (dailyRemaining < 0) || (monthlySpent > limit) || dailyInfo.isOverBudget;

                    binding.tvCategorySubtitle.setText("Today: " + CurrencyUtils.formatAmount(todaySpent) + " / " + CurrencyUtils.formatAmount(dailyAllowance) + " • Quota: " + CurrencyUtils.formatAmount(limit));

                    if (isOverLimit) {
                        double overspending = monthlySpent > limit ? (monthlySpent - limit) : Math.abs(dailyRemaining);
                        binding.tvCategoryAmount.setText("Over by " + CurrencyUtils.formatAmount(overspending));
                        binding.tvCategoryAmount.setTextColor(ContextCompat.getColor(context, R.color.expense_red));

                        binding.tvCategoryStatus.setText(monthlySpent > limit ? "Exceeded quota" : "Exceeded daily limit");
                        binding.tvCategoryStatus.setTextColor(ContextCompat.getColor(context, R.color.expense_red));

                        binding.progressCategory.setIndicatorColor(ContextCompat.getColor(context, R.color.expense_red));
                        binding.progressCategory.setProgress(100);
                    } else {
                        binding.tvCategoryAmount.setText(CurrencyUtils.formatAmount(dailyRemaining));
                        binding.tvCategoryAmount.setTextColor(ContextCompat.getColor(context, R.color.income_green));

                        binding.tvCategoryStatus.setText("Daily remaining");
                        binding.tvCategoryStatus.setTextColor(ContextCompat.getColor(context, R.color.income_green));

                        int dailyRemainingPct = dailyAllowance > 0 ? (int) Math.round((dailyRemaining / dailyAllowance) * 100) : 0;
                        dailyRemainingPct = Math.max(0, Math.min(dailyRemainingPct, 100));

                        binding.progressCategory.setIndicatorColor(ContextCompat.getColor(context, R.color.income_green));
                        binding.progressCategory.setProgress(dailyRemainingPct);
                    }
                }
            } else {
                // Total mode or category has no quota
                binding.tvCategoryAmount.setText(CurrencyUtils.formatAmount(monthlySpent));

                if (limit > 0) {
                    int spentPct = (int) Math.round((monthlySpent / limit) * 100);
                    binding.tvCategorySubtitle.setText("Spent: " + CurrencyUtils.formatAmount(monthlySpent) + " of " + CurrencyUtils.formatAmount(limit) + " (" + spentPct + "%)");

                    if (monthlySpent > limit) {
                        binding.tvCategoryAmount.setTextColor(ContextCompat.getColor(context, R.color.expense_red));
                        binding.tvCategoryStatus.setText("Exceeded quota");
                        binding.tvCategoryStatus.setTextColor(ContextCompat.getColor(context, R.color.expense_red));
                        binding.progressCategory.setIndicatorColor(ContextCompat.getColor(context, R.color.expense_red));
                        binding.progressCategory.setProgress(100);
                    } else {
                        binding.tvCategoryAmount.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
                        binding.tvCategoryStatus.setText("Total spent");
                        binding.tvCategoryStatus.setTextColor(ContextCompat.getColor(context, R.color.text_muted));
                        binding.progressCategory.setIndicatorColor(resolvedColor);
                        binding.progressCategory.setProgress(Math.min(spentPct, 100));
                    }
                } else {
                    int percentage = totalExpense > 0 ? (int) Math.round((monthlySpent / totalExpense) * 100) : 0;
                    binding.tvCategorySubtitle.setText(percentage + "% of monthly expense");

                    binding.tvCategoryAmount.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
                    binding.tvCategoryStatus.setText(isDailyMode ? "No budget set" : "Total spent");
                    binding.tvCategoryStatus.setTextColor(ContextCompat.getColor(context, R.color.text_muted));

                    binding.progressCategory.setIndicatorColor(resolvedColor);
                    binding.progressCategory.setProgress(Math.min(percentage, 100));
                }
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryClick(category);
                }
            });
        }
    }
}
