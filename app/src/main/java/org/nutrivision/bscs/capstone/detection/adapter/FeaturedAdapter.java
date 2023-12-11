package org.nutrivision.bscs.capstone.detection.adapter;

import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.nutrivision.bscs.capstone.detection.R;

import java.util.ArrayList;

public class FeaturedAdapter extends RecyclerView.Adapter<FeaturedAdapter.FeaturedViewHolder> {
    ArrayList<FeaturedClass> featuredModels;
    public FeaturedAdapter(ArrayList<FeaturedClass> featuredModels) {
        this.featuredModels = featuredModels;
    }

    @NonNull
    @Override
    public FeaturedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.featured_card_design,parent,false);
        FeaturedViewHolder featuredViewHolder = new FeaturedViewHolder(view);
        return featuredViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull FeaturedViewHolder holder, int position) {
        FeaturedClass featuredClass = featuredModels.get(position);
        holder.image.setImageResource(featuredClass.getImage());
        holder.title.setText(featuredClass.getTitle());
        holder.desc.setText(featuredClass.getDescription());
    }

    @Override
    public int getItemCount() {
        return featuredModels.size();
    }

    public static  class FeaturedViewHolder extends RecyclerView.ViewHolder{
        ImageView image;
        TextView title,desc;

        public FeaturedViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.featured_imgView);
            title = itemView.findViewById(R.id.featured_title);
            desc = itemView.findViewById(R.id.featured_description);
        }
    }

}
