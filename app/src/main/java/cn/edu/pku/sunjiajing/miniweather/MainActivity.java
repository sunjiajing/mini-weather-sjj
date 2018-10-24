package cn.edu.pku.sunjiajing.miniweather;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;

import cn.edu.pku.sunjiajing.bean.TodayWeather;
import cn.edu.pku.sunjiajing.util.NetUtil;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final int UPDATA_TODAY_WEATHER = 1;

    private ImageView mUpdateBtn;

    private ImageView mCitySelect;

    private TextView cityTv, timeTv, humidityTv, weekTv, pmDataTv, pmQualityTv,
            temperatureTv, climateTv, windTv, city_name_Tv;
    private ImageView weatherImg, pmImg;

    private TodayWeather todayWeather;

    //主进程
    private Handler mHandler = new Handler(){
        public void handleMessage(android.os.Message msg){
            switch (msg.what){
                case UPDATA_TODAY_WEATHER:
                    updateTodayWeather((TodayWeather) msg.obj);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather_info);

        mUpdateBtn = (ImageView) findViewById(R.id.title_update_btn);
        mUpdateBtn.setOnClickListener(this);

        if(NetUtil.getNetworkState(this) != NetUtil.NETWOR_NONE){
            Log.d("myWeather","网络OK");
            Toast.makeText(MainActivity.this,"网络OK!",Toast.LENGTH_LONG).show();
        }else{
            Log.d("myWeather","网络挂了");
            Toast.makeText(MainActivity.this,"网络挂了！",Toast.LENGTH_LONG).show();
        }

        mCitySelect = (ImageView) findViewById(R.id.title_city_manager);
        mCitySelect.setOnClickListener(this);

        initView();
    }

    //初始化页面信息
    void initView(){
        city_name_Tv = (TextView) findViewById(R.id.title_city_name);
        cityTv = (TextView) findViewById(R.id.city);
        timeTv = (TextView) findViewById(R.id.time);
        humidityTv = (TextView) findViewById(R.id.humidity);
        weekTv = (TextView) findViewById(R.id.week_today);
        pmDataTv = (TextView) findViewById(R.id.pm_data);
        pmQualityTv = (TextView) findViewById(R.id.pm2_5_quality);
        pmImg = (ImageView) findViewById(R.id.pm2_5_img);
        temperatureTv = (TextView) findViewById(R.id.temperature);
        climateTv = (TextView) findViewById(R.id.climate);
        windTv = (TextView) findViewById(R.id.wind);
        weatherImg = (ImageView) findViewById(R.id.weather_img);

        //从SharedPreferences中获取用户上一次选择的城市信息，在页面展示
        SharedPreferences sp = getSharedPreferences("todayWeather",MODE_PRIVATE);
        String city_name = sp.getString("city_name_Tv","N/A");
        String city = sp.getString("cityTv","N/A");
        String time = sp.getString("timeTv","N/A");
        String humidity = sp.getString("humidityTv","N/A");
        String pmData = sp.getString("pmDataTv","N/A");
        String pmQuality = sp.getString("pmQualityTv","N/A");
        String week = sp.getString("weekTv","N/A");
        String temperature = sp.getString("temperatureTv","N/A");
        String climate = sp.getString("climateTv","N/A");
        String wind = sp.getString("windTv","N/A");

        city_name_Tv.setText(city_name);
        cityTv.setText(city);
        timeTv.setText(time);
        humidityTv.setText(humidity);
        pmDataTv.setText(pmData);
        pmQualityTv.setText(pmQuality);
        weekTv.setText(week);
        temperatureTv.setText(temperature);
        climateTv.setText(climate);
        windTv.setText(wind);

    }

    @Override
    public void onClick(View view){

        //用户点击添加城市按钮后，触发onActivityResult，进入SelectCity界面
        if(view.getId() == R.id.title_city_manager){
            Intent i = new Intent(this, SelectCity.class);
            //startActivity(i);
            startActivityForResult(i,1);
        }

        //用户点击更新按钮后的结果
        if(view.getId() == R.id.title_update_btn){
            //从sharedPreferences中获取用户在SelectActivity中选择的城市的cityCode，并根据该cityCode更新数据
            SharedPreferences sharedPreferences = getSharedPreferences("currentCity",MODE_PRIVATE);
            String cityCode = sharedPreferences.getString("cityCode","101010100");//101160101

            Log.d("myWeather",cityCode);

            if(NetUtil.getNetworkState(this) != NetUtil.NETWOR_NONE){
                Log.d("myWeather","网络OK");
                queryWeatherCode(cityCode);
            }else{
                Log.d("myWeather","网络挂了");
                Toast.makeText(MainActivity.this,"网络挂了！",Toast.LENGTH_LONG).show();
            }
        }
    }

    //用户点击选择城市按钮后该函数被触发
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == 1 && resultCode == RESULT_OK){
            //从Intent中获取用户在SelectActivity中选择的城市的cityCode,并根据该cityCode更新数据
            String newCityCode = data.getStringExtra("cityCode");
            Log.d("myWeather","选择的城市代码为"+newCityCode);

            if(NetUtil.getNetworkState(this) != NetUtil.NETWOR_NONE){
                Log.d("myWeather","网络OK");
                queryWeatherCode(newCityCode);
            }else{
                Log.d("myWeather","网络挂了");
                Toast.makeText(MainActivity.this,"网络挂了！",Toast.LENGTH_LONG).show();
            }
        }
    }

    //根据传入的cityCode，获取该cityCode对应的城市数据
    private void queryWeatherCode(String cityCode){
        final String address = "http://wthrcdn.etouch.cn/WeatherApi?citykey=" + cityCode;
        Log.d("myWeather",address);
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection con = null;
                todayWeather = null;
                try{
                    URL url = new URL(address);
                    con = (HttpURLConnection)url.openConnection();
                    con.setRequestMethod("GET");
                    con.setConnectTimeout(8000);
                    con.setReadTimeout(8000);
                    InputStream in = con.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader((in)));
                    StringBuilder response = new StringBuilder();
                    String str;
                    while((str = reader.readLine()) != null){
                        response.append(str);
                        Log.d("myWeather",str);
                    }
                    String responseStr = response.toString();
                    Log.d("myWeather",responseStr);

                    //将把XML数据解析后的城市数据存放到todayWeather对象中
                    todayWeather = parseXML(responseStr);

                    //将todayWeather对象的各个属性值存入SharedPreferences中，以便在初始化MainActivity时将用户上一次选择的城市数据展示
                    SharedPreferences sp = getSharedPreferences("todayWeather",MODE_PRIVATE);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("city_name_Tv",todayWeather.getCity()+"天气");
                    editor.putString("cityTv",todayWeather.getCity());
                    editor.putString("timeTv",todayWeather.getUpdatetime()+ "发布");
                    editor.putString("humidityTv","湿度:"+todayWeather.getShidu());
                    editor.putString("pmDataTv",todayWeather.getPm25());
                    editor.putString("pmQualityTv",todayWeather.getQuality());
                    editor.putString("weekTv",todayWeather.getDate());
                    editor.putString("temperatureTv",todayWeather.getHigh()+"~"+ todayWeather.getLow());
                    editor.putString("climateTv",todayWeather.getType());
                    editor.putString("windTv","风力:"+todayWeather.getFengli());
                    editor.commit();

                    if(todayWeather != null){
                        Log.d("myWeather", todayWeather.toString());
                        Message msg = new Message();
                        msg.what = UPDATA_TODAY_WEATHER; //传递给主进程
                        msg.obj = todayWeather; //传递给主进程
                        mHandler.sendMessage(msg);
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    if(con != null){
                        con.disconnect();
                    }
                }
            }
        }).start();
    }

    //将XML数据转换成TodayWeather对象
    private TodayWeather parseXML(String xmldata){
        TodayWeather todayWeather = null;
        int fengxiangCount=0;
        int fengliCount =0;
        int dateCount=0;
        int highCount =0;
        int lowCount=0;
        int typeCount =0;
        try{
            XmlPullParserFactory fac = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = fac.newPullParser();
            xmlPullParser.setInput(new StringReader(xmldata));
            int eventType = xmlPullParser.getEventType();
            Log.d("myWeather","parseXML");
            while(eventType != XmlPullParser.END_DOCUMENT){
                switch (eventType) {
                    //判断当前事件是否为文档开始事件
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    //判断当前事件是否为标签元素开始事件
                    case XmlPullParser.START_TAG:
                        if (xmlPullParser.getName().equals("resp")) {
                            todayWeather = new TodayWeather();
                        }
                        if (todayWeather != null) {
                            if (xmlPullParser.getName().equals("city")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setCity(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("updatetime")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setUpdatetime(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("shidu")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setShidu(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("wendu")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setWendu(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("pm25")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setPm25(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("quality")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setQuality(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("fengxiang") && fengxiangCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengxiang(xmlPullParser.getText());
                                fengxiangCount++;
                            } else if (xmlPullParser.getName().equals("fengli") && fengliCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengli(xmlPullParser.getText());
                                fengliCount++;
                            } else if (xmlPullParser.getName().equals("date") && dateCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setDate(xmlPullParser.getText());
                                dateCount++;
                            } else if (xmlPullParser.getName().equals("high") && highCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setHigh(xmlPullParser.getText().substring(2).trim());
                                highCount++;
                            } else if (xmlPullParser.getName().equals("low") && lowCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setLow(xmlPullParser.getText().substring(2).trim());
                                lowCount++;
                            } else if (xmlPullParser.getName().equals("type") && typeCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setType(xmlPullParser.getText());
                                typeCount++;
                            }
                        }
                        break;
                    //判断当前事件是否为是否为标签元素结束事件
                    case XmlPullParser.END_TAG:
                        break;
                }
                //进去下一个元素并触发相应事件
                eventType = xmlPullParser.next();
            }
        }catch (XmlPullParserException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        return todayWeather;
    }

    //根据TodayWeather对象更新页面的天气信息
    void updateTodayWeather(TodayWeather todayWeather){
        city_name_Tv.setText(todayWeather.getCity()+"天气");
        cityTv.setText(todayWeather.getCity());
        timeTv.setText(todayWeather.getUpdatetime()+ "发布");
        humidityTv.setText("湿度:"+todayWeather.getShidu());
        pmDataTv.setText(todayWeather.getPm25());
        pmQualityTv.setText(todayWeather.getQuality());
        weekTv.setText(todayWeather.getDate());
        temperatureTv.setText(todayWeather.getHigh()+"~"+ todayWeather.getLow());
        climateTv.setText(todayWeather.getType());
        windTv.setText("风力:"+todayWeather.getFengli());
        
        //更新天气图片
        Bitmap bmweather;
        String weather = todayWeather.getType();
        if(weather!=null){
            if(weather.equals("暴雪")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_baoxue);
                weatherImg.setImageBitmap(bmweather);
            }else if(weather.equals("暴雨")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_baoyu);
                weatherImg.setImageBitmap(bmweather);
            }else if(weather.equals("大暴雨")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_dabaoyu);
                weatherImg.setImageBitmap(bmweather);
            }else if(weather.equals("大雪")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_daxue);
                weatherImg.setImageBitmap(bmweather);
            }else if(weather.equals("大雨")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_dayu);
                weatherImg.setImageBitmap(bmweather);
            }else if(weather.equals("多云")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_duoyun);
                weatherImg.setImageBitmap(bmweather);
            }
            else if(weather.equals("雷阵雨")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_leizhenyu);
                weatherImg.setImageBitmap(bmweather);
            }
            else if(weather.equals("雷阵雨冰雹")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_leizhenyubingbao);
                weatherImg.setImageBitmap(bmweather);
            }else if(weather.equals("晴")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_qing);
                weatherImg.setImageBitmap(bmweather);
            }else if(weather.equals("沙尘暴")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_shachenbao);
                weatherImg.setImageBitmap(bmweather);
            }else if(weather.equals("特大暴雨")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_tedabaoyu);
                weatherImg.setImageBitmap(bmweather);
            }else if(weather.equals("雾")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_wu);
                weatherImg.setImageBitmap(bmweather);
            }else if(weather.equals("晴")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_qing);
                weatherImg.setImageBitmap(bmweather);
            }else if(weather.equals("小雪")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_xiaoxue);
                weatherImg.setImageBitmap(bmweather);
            }else if(weather.equals("小雨")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_xiaoyu);
                weatherImg.setImageBitmap(bmweather);
            }else if(weather.equals("阴")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_yin);
                weatherImg.setImageBitmap(bmweather);
            }else if(weather.equals("雨夹雪")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_yujiaxue);
                weatherImg.setImageBitmap(bmweather);
            }else if(weather.equals("阵雪")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_zhenxue);
                weatherImg.setImageBitmap(bmweather);
            }else if(weather.equals("阵雨")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_zhenyu);
                weatherImg.setImageBitmap(bmweather);
            }else if(weather.equals("中雪")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_zhongxue);
                weatherImg.setImageBitmap(bmweather);
            }else if(weather.equals("中雨")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_zhongyu);
                weatherImg.setImageBitmap(bmweather);
            }
        }else{
            bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_qing);
            weatherImg.setImageBitmap(bmweather);
        }

        //更新pm图片
        Bitmap bmpm;
        if(todayWeather.getPm25()!=null){
            int pm25 = Integer.parseInt(todayWeather.getPm25());
            if(pm25 >= 0 && pm25 <=50){
                bmpm = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_0_50);
                pmImg.setImageBitmap(bmpm);
            }else if(pm25 >= 51 && pm25 <= 100){
                bmpm = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_51_100);
                pmImg.setImageBitmap(bmpm);
            }else if(pm25 >= 101 && pm25 <= 150){
                bmpm = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_101_150);
                pmImg.setImageBitmap(bmpm);
            }else if(pm25 >= 151 && pm25 <= 200){
                bmpm = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_151_200);
                pmImg.setImageBitmap(bmpm);
            }else if(pm25 >= 201 && pm25 <= 300){
                bmpm = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_201_300);
                pmImg.setImageBitmap(bmpm);
            }else if(pm25 > 300){
                bmpm = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_greater_300);
                pmImg.setImageBitmap(bmpm);
            }
        }
        else{
            bmpm = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_greater_300);
            pmImg.setImageBitmap(bmpm);
        }
        Toast.makeText(MainActivity.this,"更新成功!",Toast.LENGTH_SHORT).show();
    }
}
