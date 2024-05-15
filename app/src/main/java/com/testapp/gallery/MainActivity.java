package com.testapp.gallery;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ImageButton captureBtn;
    SQLiteHelper dbHelper;
    private List<Image> imageList;
    private ImageAdapter imageAdapter;
    private RecyclerView imageContainer;
    TextView noimages;
    EditText searchTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        if (!allPermissionsGranted()) {
            Intent intent = new Intent(MainActivity.this, PermissionActivity.class);
            startActivity(intent);
            finish();
        }

        dbHelper = new SQLiteHelper(MainActivity.this);

        captureBtn = findViewById(R.id.capture_btn);
        noimages = findViewById(R.id.empty_text);

        searchTxt = findViewById(R.id.search_txt);

        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                startActivity(intent);
            }
        });

        imagesLoader();
        setupSearchListener();

    }

    @Override
    protected void onResume() {
        super.onResume();
        imagesLoader();
        setupSearchListener();
    }

    private void imagesLoader() {

        imageContainer = findViewById(R.id.image_container);
        dbHelper = new SQLiteHelper(this);
        imageList = new ArrayList<>();
        imageAdapter = new ImageAdapter(imageList, this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        imageContainer.setLayoutManager(layoutManager);

        imageContainer.setAdapter(imageAdapter);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {"path", "timestamp", "lat", "lon"};
        Cursor cursor = db.query("captures", projection, null, null, null, null, "timestamp DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {

                String path = cursor.getString(cursor.getColumnIndexOrThrow("path"));
                String timestamp = cursor.getString(cursor.getColumnIndexOrThrow("timestamp"));
                String lat = cursor.getString(cursor.getColumnIndexOrThrow("lat"));
                String lon = cursor.getString(cursor.getColumnIndexOrThrow("lon"));

                Image image = new Image(path, timestamp, lat, lon);
                imageList.add(image);
            } while (cursor.moveToNext());
            noimages.setVisibility(View.GONE);
            cursor.close();
            imageContainer.setVisibility(View.VISIBLE);
            searchTxt.setEnabled(true);
            imageAdapter.notifyDataSetChanged();
        }else{
            imageContainer.setVisibility(View.GONE);
            noimages.setVisibility(View.VISIBLE);
            searchTxt.setEnabled(false);
        }

        db.close();

    }

    private void setupSearchListener() {

        searchTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchText = s.toString().trim();
                searchImages(searchText);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void searchImages(String searchText) {

        imageList.clear();

        if (searchText.isEmpty()) {
            imagesLoader();
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {"path", "timestamp", "lat", "lon"};
        String selection = "timestamp LIKE ? ";
        String[] selectionArgs = new String[]{"%" + searchText + "%"};

        Cursor cursor = db.query("captures", projection, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String path = cursor.getString(cursor.getColumnIndexOrThrow("path"));
                String timestamp = cursor.getString(cursor.getColumnIndexOrThrow("timestamp"));
                String lat = cursor.getString(cursor.getColumnIndexOrThrow("lat"));
                String lon = cursor.getString(cursor.getColumnIndexOrThrow("lon"));


                Image image = new Image(path, timestamp, lat, lon);
                imageList.add(image);

            } while (cursor.moveToNext());

            cursor.close();
            imageAdapter.notifyDataSetChanged();
        }else{
            noimages.setVisibility(View.VISIBLE);
            noimages.setText("No images found");
            imageContainer.setVisibility(View.GONE);
            imageList.clear();
            imageAdapter.notifyDataSetChanged();
        }

        db.close();
    }

    private boolean allPermissionsGranted() {
        String[] REQUIRED_PERMISSIONS = new String[]{
                android.Manifest.permission.INTERNET,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA
        };

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

}