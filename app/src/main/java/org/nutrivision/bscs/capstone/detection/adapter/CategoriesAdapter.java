package org.nutrivision.bscs.capstone.detection.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import org.nutrivision.bscs.capstone.detection.R;
import org.nutrivision.bscs.capstone.detection.categoriesList;

import java.util.ArrayList;

public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.AdapterAllCategoriesViewHolder> {
    ArrayList<CategoriesClass> categories;
    private Context context;
    public CategoriesAdapter(ArrayList<CategoriesClass> categories, Context context) {
        this.context = context;
        this.categories = categories;
    }

    public void setFilteredList(ArrayList<CategoriesClass> categoriesSearchList) {
        this.categories = categoriesSearchList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AdapterAllCategoriesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.categories_card_design, parent, false);
        CategoriesAdapter.AdapterAllCategoriesViewHolder categoriesViewHolder = new CategoriesAdapter.AdapterAllCategoriesViewHolder(view);
        return categoriesViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull CategoriesAdapter.AdapterAllCategoriesViewHolder holder, int position) {
        CategoriesClass categoriesClass = categories.get(position);
        holder.imageView.setImageResource(categoriesClass.getImage());
        holder.titleView.setText(categoriesClass.getTitle());
        holder.descView.setText(categoriesClass.getDesc());
        holder.relativeLayout.setBackgroundResource(categoriesClass.getBackground());

        holder.recCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, categoriesList.class);
                intent.putExtra("Title",categories.get(holder.getAdapterPosition()).getTitle());
                intent.putExtra("Image",categories.get(holder.getAdapterPosition()).getImage());
                intent.putExtra("Description",categories.get(holder.getAdapterPosition()).getDesc());
                intent.putExtra("Background",categories.get(holder.getAdapterPosition()).getBackground());
                context.startActivity(intent);
            }
        });
    }


    @Override
    public int getItemCount() {
        return categories.size();
    }

    public static class FeaturedViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, desc;

        public FeaturedViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.featured_imgView);
            title = itemView.findViewById(R.id.featured_title);
            desc = itemView.findViewById(R.id.featured_description);
        }
    }

    public class AdapterAllCategoriesViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout relativeLayout;
        ImageView imageView;
        TextView titleView, descView;
        CardView recCard;

        public AdapterAllCategoriesViewHolder(@NonNull View itemView) {
            super(itemView);
            relativeLayout = itemView.findViewById(R.id.background_gradient);
            imageView = itemView.findViewById(R.id.categories_image);
            titleView = itemView.findViewById(R.id.categories_title);
            descView = itemView.findViewById(R.id.categories_description);
            recCard = itemView.findViewById(R.id.recCard);
        }
    }
}
