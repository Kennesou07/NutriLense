package org.nutrivision.bscs.capstone.detection.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.nutrivision.bscs.capstone.detection.R;

import java.util.List;
public class ProductListAdapter extends RecyclerView.Adapter<ProductListAdapter.ProductViewHolder> {

    private List<ProductList> productList;

    public ProductListAdapter(List<ProductList> productList) {
        this.productList = productList;
    }

    public void updateData(List<ProductList> productList) {
        this.productList = productList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate your item layout here
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.product_item_layout, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        // Bind data to views
        ProductList product = productList.get(position);
        holder.productNameTextView.setText(product.getProductName());
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView productNameTextView;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize your views
            productNameTextView = itemView.findViewById(R.id.productNameTextView);
        }
    }
}
