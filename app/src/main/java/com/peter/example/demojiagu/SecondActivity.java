package com.peter.example.demojiagu;

import android.app.Activity;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

/**
 * Created by Administrator on 2016/12/19.
 */

public class SecondActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        setContentView(R.layout.activity_second);
        Log.d("demo","SecondActivity"+this.getApplication().toString());
    }

    public  void onClick(View view) {
        Toast.makeText(this,"点击textView",Toast.LENGTH_SHORT).show();
    }
}
