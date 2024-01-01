package org.nutrivision.bscs.capstone.detection.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.nutrivision.bscs.capstone.detection.R;

import java.util.List;

public class NutritionAdapter extends RecyclerView.Adapter<NutritionAdapter.ViewHolder> {

    private List<NutritionItem> nutritionItemList;

    // Constructor to initialize the adapter with a list of nutrition items
    public NutritionAdapter(List<NutritionItem> nutritionItemList) {
        this.nutritionItemList = nutritionItemList;
    }

    // ViewHolder class to hold the views for each item
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nutrientNameTextView;
        TextView nutrientValueTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            nutrientNameTextView = itemView.findViewById(R.id.nutritionLabelTextView);
            nutrientValueTextView = itemView.findViewById(R.id.nutritionValueTextView);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nutrition, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NutritionItem nutritionItem = nutritionItemList.get(position);

        // Bind data to views
        holder.nutrientNameTextView.setText(nutritionItem.getLabel());
        holder.nutrientValueTextView.setText(nutritionItem.getValue());
    }

    @Override
    public int getItemCount() {
        return nutritionItemList.size();
    }
}

