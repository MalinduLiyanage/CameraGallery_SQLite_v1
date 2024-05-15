package com.testapp.gallery;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;

public class ViewActivity extends AppCompatActivity {

    ImageView image;
    MapView mapView;
    TextView internetTxt, delBtn;
    SQLiteHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view);

        image = findViewById(R.id.img_view);
        mapView = findViewById(R.id.map_view);
        internetTxt = findViewById(R.id.internet_text);
        delBtn = findViewById(R.id.del_btn);

        dbHelper = new SQLiteHelper(this);

        String path = getIntent().getStringExtra("path");

        if (path != null){
            showImage(path);
            loadImagedata(path);
        }else{
            Toast.makeText(this, "DB Error", Toast.LENGTH_SHORT).show();
            finish();
        }

        delBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ViewActivity.this);
                builder.setMessage("Deletion cannot be undone!");
                builder.setTitle("Delete Image");
                builder.setCancelable(true);
                builder.setNegativeButton("No", (DialogInterface.OnClickListener) (dialog, which) -> {
                    dialog.cancel();
                });
                builder.setPositiveButton("Yes", (DialogInterface.OnClickListener) (dialog, which) -> {
                    deleteImage(path);
                    dialog.cancel();
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });

    }

    private void showImage(String path) {
        File imageFile = new File(path);
        Picasso.get()
                .load(imageFile)
                .placeholder(R.drawable.placeholder)
                .into(image);
    }

    private void loadImagedata(String path) {

        String timestamp, lat, lon;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {"timestamp", "lat", "lon"};
        String selection = "path = ?";
        String[] selectionArgs = {path};
        Cursor cursor = db.query("captures", projection, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                timestamp = cursor.getString(cursor.getColumnIndexOrThrow("timestamp"));
                lat = cursor.getString(cursor.getColumnIndexOrThrow("lat"));
                lon = cursor.getString(cursor.getColumnIndexOrThrow("lon"));

            } while (cursor.moveToNext());
            cursor.close();

            if(!lat.contains("No Data") || !lon.contains("No Data")){
                mapView.setVisibility(View.VISIBLE);
                double latitude = Double.parseDouble(lat);
                double longitude = Double.parseDouble(lon);

                //locTxt.setText("Location Data : " + lat + ", " + lon);

                Configuration.getInstance().setUserAgentValue("com.example.openstreet");
                mapView.setTileSource(TileSourceFactory.MAPNIK); // Set the tile source
                mapView.setMultiTouchControls(false);
                GeoPoint startPoint = new GeoPoint(latitude, longitude);
                mapView.getController().setCenter(startPoint);
                mapView.getController().setZoom(17.0);
                Marker marker = new Marker(mapView); // Replace "this" with context if needed
                marker.setPosition(startPoint);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mapView.getOverlays().add(marker);
                mapView.invalidate();
            }else{
                internetTxt.setText("This image has no recorded Location data.\nHint : Turn on GPS of your device.");
            }
            //time.setVisibility(View.VISIBLE);
            //timeTxt.setText("Timestamp : " + timeStamp);
        }else{
            //storage.setVisibility(View.VISIBLE);
        }
        db.close();

    }

    private void deleteImage(String path) {

        SQLiteHelper dbHelper = new SQLiteHelper(ViewActivity.this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        String selection = "path = ? ";
        String[] selectionArgs = {path};
        db.delete("captures", selection, selectionArgs);

        db.close();
        Toast.makeText(ViewActivity.this,"Image deleted!", Toast.LENGTH_LONG).show();
        finish();
    }



}