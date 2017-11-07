package com.example.wordrecognition;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class RecognitonResult extends AppCompatActivity {
private TextView recogniton_result;
    private String result;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recogniton_result);
        ActionBar actionBar=getSupportActionBar();
        if(actionBar!=null){
            actionBar.hide();
        }
        recogniton_result=(TextView)findViewById(R.id.recogniton_result);
        Intent intent=getIntent();
        result=intent.getStringExtra("result");
        recogniton_result.setText(result);
    }
}
