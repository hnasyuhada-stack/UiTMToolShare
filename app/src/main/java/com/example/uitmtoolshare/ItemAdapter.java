package com.example.uitmtoolshare;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

    Context context;
    ArrayList<Item> itemList;

    public ItemAdapter(Context context, ArrayList<Item> itemList) {
        this.context = context;
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_row, parent, false);
        return new ItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = itemList.get(position);
        holder.name.setText(item.name);
        holder.description.setText(item.description);

        // Check if image path exists and is not empty
        if (item.imagePath != null && !item.imagePath.isEmpty()) {
            // Load image using Glide from Firebase Storage URL
            Glide.with(context)
                    .load(item.imagePath) // Firebase Storage URL
                    .placeholder(R.drawable.placeholder) // Placeholder image
                    .into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.placeholder); // Default placeholder
        }

        // Set item click listener to open ItemDetailActivity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ItemDetailActivity.class);
            intent.putExtra("item", item); // Pass the full item object
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView name, description;
        ImageView image;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.itemNameText);
            description = itemView.findViewById(R.id.itemDescText);
            image = itemView.findViewById(R.id.itemImage);
        }
    }
}
