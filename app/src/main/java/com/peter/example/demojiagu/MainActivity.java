package com.peter.example.demojiagu;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("demopeter","MainActivity"+this.getApplication().toString());
        Log.d("demopeter","MainActivity"+this.getApplicationInfo().dataDir);
    }

    public void onClick(View view) {
        Intent intent = new Intent(this,SecondActivity.class);
        startActivity(intent);
    }
}
