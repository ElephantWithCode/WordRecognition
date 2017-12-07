package com.example.wordrecognition;

import android.graphics.BitmapFactory;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import me.pqpo.smartcropperlib.view.CropImageView;

public class DummyActivity extends AppCompatActivity {

    private Button mButton;
    private CropImageView mCropImageView;
    private HorizontalScrollingBarView mScrollView;

    private ArrayList<String> mTabNames = new ArrayList<>();
    private View mDummyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dummy);
        mCropImageView = findViewById(R.id.smart_crop_image_view);
        mButton = findViewById(R.id.button);
        mCropImageView.setImageToCrop(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        mScrollView = findViewById(R.id.dummy_horizontal);
        mDummyView = findViewById(R.id.dummy_view);


        mTabNames.add("Hello");
        mTabNames.add("World");
        mTabNames.add("Andr1");
        mTabNames.add("Andr2");
        mTabNames.add("Andr3");
        mScrollView.setTabName(mTabNames);
        mScrollView.attachView(mDummyView);
    }
}
