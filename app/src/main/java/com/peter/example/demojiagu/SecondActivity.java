package com.peter.example.demojiagu;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.duowan.jni.JEnv;

/**
 * Created by Administrator on 2016/12/19.
 */

public class SecondActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        Log.d("demo","SecondActivity"+this.getApplication().toString());
    }

    public  void onClick(View view) {


        Toast.makeText(this,"点击textView"+ JEnv.getCryptSeed(),Toast.LENGTH_SHORT).show();

    }

}
