package com.coolweather.android;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    private ScrollView weatherLayout;//主要Weather布局的天气
    private TextView titleCity;//市名
    private TextView titleUpdateTime;//更新时间
    private TextView degreeText;//温度
    private TextView weatherInfoText;//天气信息
    private LinearLayout forecastLayout;//预报布局
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private ImageView bingPicImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
/*        if(Build.VERSION.SDK_INT>=21){
            //获取当前活动的DecorView
            View decorView=getWindow().getDecorView();
            //活动布局显示在状态栏上
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }*/
        setContentView(R.layout.activity_weather);
        //初始化各个控件
        weatherLayout=findViewById(R.id.weather_layout);
        titleCity=findViewById(R.id.title_city);
        titleUpdateTime=findViewById(R.id.title_update_time);
        degreeText=findViewById(R.id.degree_text);
        weatherInfoText=findViewById(R.id.weather_info_text);
        forecastLayout=findViewById(R.id.forecast_layout);
        aqiText=findViewById(R.id.aqi_text);
        pm25Text=findViewById(R.id.pm25_text);
        comfortText=findViewById(R.id.comfort_text);
        carWashText=findViewById(R.id.car_wash_text);
        sportText=findViewById(R.id.sport_text);
        bingPicImg=findViewById(R.id.bing_pic_img);
        //创建本地缓存
        SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString =prefs.getString("weather",null);
        //如果本地有缓存数据
        if (weatherString!=null){
            //直接解析天气数据
            Weather weather=Utility.handleWeatherResponse(weatherString);
            showWeatherInfo(weather);
        }else{//无缓存时区服务器查询天气
            //从Intent中获取天气id
            String weatherId=getIntent().getStringExtra("weather_id");
            //让ScrollView隐藏
            weatherLayout.setVisibility(View.INVISIBLE);
            //从服务器请求天气数据
            requestWeather(weatherId);
        }
        //从SharedPerences中读取缓存的图片
        String bingPic=prefs.getString("bing_pic",null);
        if (bingPic!=null){//如果本地有就直接让Glide加载图片
            Glide.with(this).load(bingPic).into(bingPicImg);
        }else{//如果没有从必应获取图片
            loadBingPic();
        }
    }

    private void loadBingPic() {
        //调用HttpUtil.sendOkHttpRequest从必应获取图片
        String requestBingPic="http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic=response.body().string();
                SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {//切回主线程更新UI页面
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }

    private void requestWeather(String weatherId) {
        //用天气id和之前盛情好的APIkey设置一个接口地址
        String weatherUrl="http://guolin.tech/api/weather?cityid="+weatherId+"&key=af538be1f470494d8faf0589fcbae5ce";
        //向该接口地址发出请求，返回天气信息的JSON数据
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //将JSON数据转化为Weather对象
                final String responseText=response.body().string();
                final Weather weather=Utility.handleWeatherResponse(responseText);
                //回到主线程修改页面UI内容
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather!=null&&"ok".equals(weather.status)){
                            //将数据缓存到SharedPreferences中
                            SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            //将修改的内容进行显示
                            showWeatherInfo(weather);
                        }else{
                            Toast.makeText(WeatherActivity.this,"获取天气信息失败!",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

        });
        loadBingPic();
    }
//处理并展示Weather实体类中的数据
    private void showWeatherInfo(Weather weather) {
        //设置title，now
        String cityName=weather.basic.cityName;
        String updateTime =weather.basic.update.updateTime.split(" ")[1];
        String degree=weather.now.temperature+"°C";
        String weatherInfo=weather.now.more.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        //遍历预报信息
        for (Forecast forecast:weather.forecastList){
            //将item中的数据加入到forecast中
            View view =LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);
            TextView dateText=view.findViewById(R.id.date_text);
            TextView infoText=view.findViewById(R.id.info_text);
            TextView maxText=view.findViewById(R.id.max_text);
            TextView minText=view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }
        //设置aqi
        if (weather.aqi!=null){
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        //设置建议
        String comfort="舒适度："+weather.suggestion.comfort.info;
        String carWash="洗车指数："+weather.suggestion.carWash.info;
        String sport="运动建议："+weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        //显示在滚屏加载的内容
        weatherLayout.setVisibility(View.VISIBLE);
    }

}
