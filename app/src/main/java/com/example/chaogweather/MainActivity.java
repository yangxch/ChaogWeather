package com.example.chaogweather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(this);
        //如果读取到缓存数据，说明之前已经启动并且请求过天气数据，已经选择过城市。所以没必要再选地点，直接跳到WeatherActivity
        if(preferences.getString("weather",null)!=null){
            Intent intent=new Intent(MainActivity.this,WeatherActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
