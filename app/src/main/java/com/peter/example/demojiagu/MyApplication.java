package com.peter.example.demojiagu;

import android.app.Application;

/**
 * Created by Administrator on 2016/12/19.
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        android.util.Log.e("peter","sourceApplication onCreate");
    }
}
