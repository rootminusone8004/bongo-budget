package com.budjet.app.ui.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.budjet.app.R;
import com.budjet.app.data.model.Category;
import com.budjet.app.data.model.Transaction;
import com.budjet.app.databinding.ItemTransactionBinding;
import com.budjet.app.util.CurrencyUtils;
import com.budjet.app.util.DateUtils;

public class TransactionAdapter extends ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder> {

    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
        void onTransactionEdit(Transaction transaction);
        void onTransactionDelete(Transaction transaction);
    }

    private final OnTransactionClickListener listener;

    private static final DiffUtil.ItemCallback<Transaction> DIFF_CALLBACK = new DiffUtil.ItemCallback<Transaction>() {
        @Override
        public boolean areItemsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
            return oldItem.equals(newItem);
        }
    };

    public TransactionAdapter(OnTransactionClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTransactionBinding binding = ItemTransactionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new TransactionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final ItemTransactionBinding binding;

        TransactionViewHolder(ItemTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Transaction transaction) {
            Context context = itemView.getContext();

            binding.tvTitle.setText(transaction.getTitle());
            String subtitle = transaction.getCategory() + " • " + DateUtils.formatDate(transaction.getDate());
            binding.tvCategoryDate.setText(subtitle);

            if (transaction.getNote() != null && !transaction.getNote().trim().isEmpty()) {
                binding.tvNote.setVisibility(View.VISIBLE);
                binding.tvNote.setText(transaction.getNote());
            } else {
                binding.tvNote.setVisibility(View.GONE);
            }

            // Amount formatting and color
            if (transaction.isExpense()) {
                binding.tvAmount.setText(CurrencyUtils.formatTransactionAmount(transaction.getAmount(), true));
                binding.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.expense_red));
            } else {
                binding.tvAmount.setText(CurrencyUtils.formatTransactionAmount(transaction.getAmount(), false));
                binding.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.income_green));
            }

            // Icon and icon tint
            int iconRes = Category.getIconResource(transaction.getCategory());
            int colorRes = Category.getColorResource(transaction.getCategory());
            int resolvedColor = ContextCompat.getColor(context, colorRes);

            binding.ivCategoryIcon.setImageResource(iconRes);
            binding.ivCategoryIcon.setImageTintList(ColorStateList.valueOf(resolvedColor));
            binding.iconContainer.getBackground().mutate().setTint(
                    (resolvedColor & 0x00FFFFFF) | 0x22000000 // 13% opacity background
            );

            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTransactionClick(transaction);
                }
            });

            // Menu button
            binding.btnMenu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(context, binding.btnMenu);
                popup.inflate(R.menu.transaction_item_menu);
                popup.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.menu_edit) {
                        if (listener != null) listener.onTransactionEdit(transaction);
                        return true;
                    } else if (itemId == R.id.menu_delete) {
                        if (listener != null) listener.onTransactionDelete(transaction);
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }
    }
}
