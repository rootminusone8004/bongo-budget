package com.budjet.app.ui.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.budjet.app.R;
import com.budjet.app.data.model.Budget;
import com.budjet.app.data.model.Category;
import com.budjet.app.databinding.ItemBudgetBinding;
import com.budjet.app.util.CurrencyUtils;
import com.budjet.app.util.DailyBudgetCalculator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    public interface OnBudgetActionListener {
        void onEditBudget(Budget budget);
        void onDeleteBudget(Budget budget);
        void onBudgetClick(Budget budget);
    }

    private List<Budget> budgets = new ArrayList<>();
    private Map<String, Double> categorySpendingMap = new HashMap<>();
    private final Set<String> dailyModeCategories = new HashSet<>();
    private Calendar currentCalendar = Calendar.getInstance();

    private final OnBudgetActionListener listener;

    public BudgetAdapter(OnBudgetActionListener listener) {
        this.listener = listener;
    }

    public void setBudgets(List<Budget> list, Map<String, Double> spendingMap, Calendar calendar) {
        this.budgets = list != null ? list : new ArrayList<>();
        this.categorySpendingMap = spendingMap != null ? spendingMap : new HashMap<>();
        this.currentCalendar = calendar != null ? (Calendar) calendar.clone() : Calendar.getInstance();
        notifyDataSetChanged();
    }

    public void toggleAllModes() {
        if (dailyModeCategories.size() == budgets.size()) {
            dailyModeCategories.clear();
        } else {
            for (Budget b : budgets) {
                dailyModeCategories.add(b.getCategory());
            }
        }
        notifyDataSetChanged();
    }

    public boolean isAllDailyMode() {
        return !budgets.isEmpty() && dailyModeCategories.size() == budgets.size();
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBudgetBinding binding = ItemBudgetBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new BudgetViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        holder.bind(budgets.get(position));
    }

    @Override
    public int getItemCount() {
        return budgets.size();
    }

    class BudgetViewHolder extends RecyclerView.ViewHolder {
        private final ItemBudgetBinding binding;

        BudgetViewHolder(ItemBudgetBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Budget budget) {
            Context context = itemView.getContext();
            String category = budget.getCategory();

            binding.tvCategoryName.setText(category);

            Double spent = categorySpendingMap.get(category);
            double spentAmount = spent != null ? spent : 0.0;
            double limit = budget.getAmount();

            boolean isDailyMode = dailyModeCategories.contains(category);

            // Category Icon & Colors
            int iconRes = Category.getIconResource(category);
            int colorRes = Category.getColorResource(category);
            int resolvedColor = ContextCompat.getColor(context, colorRes);

            binding.ivBudgetIcon.setImageResource(iconRes);
            binding.ivBudgetIcon.setImageTintList(ColorStateList.valueOf(resolvedColor));
            binding.iconContainer.getBackground().mutate().setTint(
                    (resolvedColor & 0x00FFFFFF) | 0x22000000
            );

            // Calculate daily budget info
            DailyBudgetCalculator.DailyBudgetInfo dailyInfo = DailyBudgetCalculator.calculate(
                    limit, spentAmount, currentCalendar
            );

            // Progress bar and Overall calculations
            int percentage = limit > 0 ? (int) Math.round((spentAmount / limit) * 100) : 0;
            binding.progressBudget.setProgress(Math.min(percentage, 100));

            int progressColor;
            if (spentAmount > limit) {
                double over = spentAmount - limit;
                binding.tvRemainingStatus.setText("Over by " + CurrencyUtils.formatAmount(over));
                binding.tvRemainingStatus.setTextColor(ContextCompat.getColor(context, R.color.expense_red));
                progressColor = ContextCompat.getColor(context, R.color.expense_red);
            } else {
                double remaining = limit - spentAmount;
                binding.tvRemainingStatus.setText(CurrencyUtils.formatAmount(remaining) + " left");
                if (percentage >= 80) {
                    binding.tvRemainingStatus.setTextColor(ContextCompat.getColor(context, R.color.warning_amber));
                    progressColor = ContextCompat.getColor(context, R.color.warning_amber);
                } else {
                    binding.tvRemainingStatus.setTextColor(ContextCompat.getColor(context, R.color.income_green));
                    progressColor = ContextCompat.getColor(context, R.color.income_green);
                }
            }
            binding.progressBudget.setIndicatorColor(progressColor);
            binding.tvSpentLimit.setText("Spent: " + CurrencyUtils.formatAmount(spentAmount) + " of " + CurrencyUtils.formatAmount(limit) + " (" + percentage + "%)");

            // Daily estimated view setup
            binding.tvDailySpendable.setText(dailyInfo.getDailyFormatted());
            binding.tvDailyDetails.setText(dailyInfo.getDailySubtext());
            binding.tvDailyPlanned.setText(dailyInfo.getPlannedDailyText());

            if (dailyInfo.isOverBudget) {
                binding.tvDailySpendable.setTextColor(ContextCompat.getColor(context, R.color.expense_red));
                binding.ivDailyIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.expense_red)));
            } else {
                binding.tvDailySpendable.setTextColor(ContextCompat.getColor(context, R.color.primary));
                binding.ivDailyIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary)));
            }

            // View toggle states
            if (isDailyMode) {
                binding.layoutOverallMode.setVisibility(View.GONE);
                binding.layoutDailyMode.setVisibility(View.VISIBLE);
                binding.btnToggleMode.setText("Overall");
                binding.btnToggleMode.setIconResource(R.drawable.ic_view_agenda);
                binding.tvModeHint.setText("Tap card to view transactions");
            } else {
                binding.layoutOverallMode.setVisibility(View.VISIBLE);
                binding.layoutDailyMode.setVisibility(View.GONE);
                binding.btnToggleMode.setText("Daily");
                binding.btnToggleMode.setIconResource(R.drawable.ic_swap_horiz);
                binding.tvModeHint.setText("Tap card to view transactions");
            }

            // Click listener on card to view category transactions
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBudgetClick(budget);
                }
            });

            // Toggle listener
            View.OnClickListener toggleClickListener = v -> {
                if (dailyModeCategories.contains(category)) {
                    dailyModeCategories.remove(category);
                } else {
                    dailyModeCategories.add(category);
                }
                notifyItemChanged(getAdapterPosition());
            };

            binding.btnToggleMode.setOnClickListener(toggleClickListener);

            // Menu button
            binding.btnBudgetMenu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(context, binding.btnBudgetMenu);
                popup.inflate(R.menu.budget_item_menu);
                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.menu_edit_budget) {
                        if (listener != null) listener.onEditBudget(budget);
                        return true;
                    } else if (id == R.id.menu_delete_budget) {
                        if (listener != null) listener.onDeleteBudget(budget);
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }
    }
}
