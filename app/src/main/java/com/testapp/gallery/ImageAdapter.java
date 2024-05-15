package com.testapp.gallery;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder>{

    private List<Image> imageList;
    private Context context;

    public ImageAdapter(List<Image> imageList, Context context) {
        this.imageList = imageList;
        this.context = context;
    }

    @NonNull
    @Override
    public ImageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.placeholder_image, parent, false);
        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull ImageAdapter.ViewHolder holder, int position) {

        Image image = imageList.get(position);
        File imageFile = new File(image.getImgPath());
        Picasso.get()
                .load(imageFile)
                .fit()
                .placeholder(R.drawable.placeholder)
                .into(holder.objImg);

        holder.titleTxt.setText(image.getTimestamp());
        holder.timeTxt.setText(image.getImgPath());
        holder.locTxt.setText("Lat: " + image.getLat() + ", Lon: " + image.getLon());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ViewActivity.class);
                intent.putExtra("path", image.getImgPath());
                context.startActivity(intent);

            }
        });

    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }



    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView objImg;
        TextView titleTxt, timeTxt, locTxt;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            objImg = itemView.findViewById(R.id.image_pic);
            titleTxt = itemView.findViewById(R.id.image_title);
            timeTxt = itemView.findViewById(R.id.image_timestamp);
            locTxt = itemView.findViewById(R.id.image_location);

        }
    }
}

