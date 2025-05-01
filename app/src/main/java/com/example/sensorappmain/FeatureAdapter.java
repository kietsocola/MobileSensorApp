package com.example.sensorappmain;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FeatureAdapter extends RecyclerView.Adapter<FeatureAdapter.ViewHolder> {
    private List<Feature> features;
    private OnFeatureClickListener listener;

    public interface OnFeatureClickListener {
        void onFeatureClick(Feature feature, View view);
    }

    public FeatureAdapter(List<Feature> features, OnFeatureClickListener listener) {
        this.features = features;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feature, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Feature feature = features.get(position);
        holder.featureName.setText(feature.getName());
        holder.featureIcon.setImageResource(feature.getIconResId());
        holder.itemView.setOnClickListener(v -> listener.onFeatureClick(feature, v));
    }

    @Override
    public int getItemCount() { return features.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView featureName;
        ImageView featureIcon;

        ViewHolder(View itemView) {
            super(itemView);
            featureName = itemView.findViewById(R.id.featureName);
            featureIcon = itemView.findViewById(R.id.featureIcon);
        }
    }
}
