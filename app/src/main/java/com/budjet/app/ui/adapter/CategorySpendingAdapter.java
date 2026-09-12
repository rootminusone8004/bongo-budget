package com.budjet.app.ui.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.budjet.app.data.model.Category;
import com.budjet.app.data.model.CategorySpending;
import com.budjet.app.databinding.ItemCategorySpendingBinding;
import com.budjet.app.util.CurrencyUtils;

import java.util.ArrayList;
import java.util.List;

public class CategorySpendingAdapter extends RecyclerView.Adapter<CategorySpendingAdapter.ViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(String category);
    }

    private List<CategorySpending> items = new ArrayList<>();
    private double totalExpense = 0.0;
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

    public void setData(List<CategorySpending> list, double totalExpense) {
        this.items = list != null ? list : new ArrayList<>();
        this.totalExpense = totalExpense;
        notifyDataSetChanged();
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

            String title = item.getCategory() + " (" + item.getTransactionCount() + ")";
            binding.tvCategoryTitle.setText(title);
            binding.tvCategoryAmount.setText(CurrencyUtils.formatAmount(item.getTotalSpent()));

            int percentage = totalExpense > 0 ? (int) Math.round((item.getTotalSpent() / totalExpense) * 100) : 0;
            binding.progressCategory.setProgress(Math.min(percentage, 100));

            int iconRes = Category.getIconResource(item.getCategory());
            int colorRes = Category.getColorResource(item.getCategory());
            int resolvedColor = ContextCompat.getColor(context, colorRes);

            binding.ivCatIcon.setImageResource(iconRes);
            binding.ivCatIcon.setImageTintList(ColorStateList.valueOf(resolvedColor));
            binding.progressCategory.setIndicatorColor(resolvedColor);
            binding.iconContainer.getBackground().mutate().setTint(
                    (resolvedColor & 0x00FFFFFF) | 0x22000000
            );

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryClick(item.getCategory());
                }
            });
        }
    }
}
