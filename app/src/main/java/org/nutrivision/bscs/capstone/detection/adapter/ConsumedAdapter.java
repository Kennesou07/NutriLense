package org.nutrivision.bscs.capstone.detection.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.nutrivision.bscs.capstone.detection.R;

import java.util.List;

public class ConsumedAdapter extends RecyclerView.Adapter<ConsumedAdapter.ViewHolder> {
    private List<ConsumedItem> consumedItemList;
    public ConsumedAdapter(List<ConsumedItem> consumedItemList){
        this.consumedItemList = consumedItemList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.consumed_list, parent, false);
        return new ViewHolder(view);    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConsumedItem consumedItem = consumedItemList.get(position);

        // Bind data to views
        holder.dateTextView.setText(consumedItem.getDate());
        holder.foodTextView.setText(consumedItem.getFood());
    }

    @Override
    public int getItemCount() {
        return consumedItemList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateTextView;
        TextView foodTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            foodTextView = itemView.findViewById(R.id.foodTextView);
        }
    }

}
