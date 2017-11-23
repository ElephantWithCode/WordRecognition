package com.example.wordrecognition;

import android.graphics.BitmapFactory;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import me.pqpo.smartcropperlib.view.CropImageView;

public class DummyActivity extends AppCompatActivity {

    private Button mButton;
    private CropImageView mCropImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dummy);
        mCropImageView = findViewById(R.id.smart_crop_image_view);
        mButton = findViewById(R.id.button);
        mCropImageView.setImageToCrop(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));

    }
}
