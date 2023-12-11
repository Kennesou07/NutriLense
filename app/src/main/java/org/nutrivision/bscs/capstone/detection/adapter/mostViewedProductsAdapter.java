package org.nutrivision.bscs.capstone.detection.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.nutrivision.bscs.capstone.detection.R;

import java.util.ArrayList;

public class mostViewedProductsAdapter extends RecyclerView.Adapter<mostViewedProductsAdapter.mostViewedViewHolder> {

    ArrayList<FeaturedClass> mostViewedProducts;

    public mostViewedProductsAdapter(ArrayList<FeaturedClass> mostViewedProducts) {
        this.mostViewedProducts = mostViewedProducts;
    }

    @NonNull
    @Override
    public mostViewedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.most_viewed_card_design,parent,false);
        mostViewedProductsAdapter.mostViewedViewHolder mostViewedViewHolder = new mostViewedProductsAdapter.mostViewedViewHolder(view);
        return mostViewedViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull mostViewedViewHolder holder, int position) {
        FeaturedClass featuredClass = mostViewedProducts.get(position);
        holder.image.setImageResource(featuredClass.getImage());
        holder.title.setText(featuredClass.getTitle());
        holder.desc.setText(featuredClass.getDescription());
    }

    @Override
    public int getItemCount() {
        return mostViewedProducts.size();
    }

    public static class mostViewedViewHolder extends RecyclerView.ViewHolder{
        ImageView image;
        TextView title,desc;
        public mostViewedViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.most_viewed_img);
            title = itemView.findViewById(R.id.most_viewed_title);
            desc = itemView.findViewById(R.id.most_viewed_description);
        }
    }
}
