package org.nutrivision.bscs.capstone.detection.adapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.nutrivision.bscs.capstone.detection.R;
import org.nutrivision.bscs.capstone.detection.tflite.Classifier;

import java.util.ArrayList;
import java.util.List;

public class DetectedObjectsAdapter extends RecyclerView.Adapter<DetectedObjectsAdapter.ViewHolder> {

    private static List<Classifier.Recognition> detectedObjects = new ArrayList<>();
    private OnItemClickListener itemClickListener;

    public void updateData(List<Classifier.Recognition> newDetectedObjects) {
        detectedObjects.clear();
        detectedObjects.addAll(newDetectedObjects);
        notifyDataSetChanged();
    }
    public void clearData(){
        detectedObjects.clear();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d("DetectedObjectsAdapter", "onCreateViewHolder");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_detected_object, parent, false);
        return new ViewHolder(view, itemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.d("DetectedObjectsAdapter", "onBindViewHolder: " + position);
        Classifier.Recognition object = detectedObjects.get(position);
        holder.objectNameTextView.setText(object.getTitle());
        // Add any other information you want to display for each object
    }

    @Override
    public int getItemCount() {
        return detectedObjects.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView objectNameTextView;

        public ViewHolder(@NonNull View itemView,OnItemClickListener listener) {
            super(itemView);
            objectNameTextView = itemView.findViewById(R.id.objectNameTextView);
            itemView.setOnClickListener(view -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(detectedObjects.get(position).getTitle());
                }
            });
        }
    }
    public interface OnItemClickListener{
        void onItemClick(String objectName);
    }
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }
}
