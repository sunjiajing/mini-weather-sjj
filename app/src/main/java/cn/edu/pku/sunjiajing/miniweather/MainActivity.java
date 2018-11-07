package cn.edu.pku.sunjiajing.miniweather;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.umeng.analytics.MobclickAgent;

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
import java.util.ArrayList;
import java.util.List;

import cn.edu.pku.sunjiajing.app.MyApplication;
import cn.edu.pku.sunjiajing.bean.City;
import cn.edu.pku.sunjiajing.bean.TodayWeather;
import cn.edu.pku.sunjiajing.util.NetUtil;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final int UPDATE_TODAY_WEATHER = 1;

    private ImageView mUpdateBtn;//更新按钮
    private ProgressBar pbForUpdate; //更新按钮相对应的进度条

    private ImageView mCitySelect;//选择城市安妮

    private TextView cityTv, timeTv, humidityTv, weekTv, pmDataTv, pmQualityTv,
            temperatureTv, climateTv, windTv, city_name_Tv;
    private ImageView weatherImg, pmImg;

    private TodayWeather todayWeather;

    //未来四天的天气信息
    private TextView date1, date2, date3, date4;//未来四天日期
    private TextView temperatureTv1, temperatureTv2, temperatureTv3, temperatureTv4;//温度信息
    private TextView type1Tv, type2Tv, type3Tv, type4Tv;//天气状况
    private ImageView weatherImg1, weatherImg2, weatherImg3, weatherImg4;//天气状况图片
    private TextView feng1, feng2, feng3, feng4;
    private  ImageView[] dots2;//导航小圆点
    private int[] ids2 = {R.id.weatheriv1, R.id.weatheriv2};//小圆点的imageview值
    private ViewPagerAdapter vpAdapter2;
    private ViewPager vp2;
    private List<View> views2 = new ArrayList<>();
    
    //定位相关变量
    private static final int UPDATE_LOCATION = 2;
    private ImageView mLocateBtn;//定位按钮
    public LocationClient mLocationClient = null;//使用百度定位接口
    private MyLocationListener mLocationListener = new MyLocationListener(); //设置定位监控器
    private LocationService.MyBinder binder = null;
    private MyServiceConn myServiceConn;
    private String cityname;//保存定位到的当前城市的城市名称
    private List<City> cityList;//保存数据库中所有的城市数据

    private String newCityCode = "101010100"; //定位到的城市和用户手动选择的城市中最先的城市

    //主进程
    private Handler mHandler = new Handler(){
        public void handleMessage(android.os.Message msg){
            switch (msg.what){
                case UPDATE_TODAY_WEATHER:
                    updateTodayWeather((TodayWeather) msg.obj);
                    break;
                case UPDATE_LOCATION:
                    String cityname = (String) msg.obj;
                    String cityCode = null;
                    Log.d("myinfo", cityname+"---second");
                    for(City city:cityList) {
                        if(cityname.substring(0,cityname.length()-1) .equals( city.getCity())){
                            cityCode = city.getNumber();
                        }
                    }
                    Log.d("myinfo", cityCode+"---third");

                    SharedPreferences sharedPreferences = getSharedPreferences("locationCity", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("location_cityCode", cityCode);//供定位按钮根据该cityCode更新城市数据
                    editor.commit();
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

        //更新界面
        mUpdateBtn = (ImageView) findViewById(R.id.title_update_btn);
        pbForUpdate = (ProgressBar) findViewById(R.id.title_update_progress);//更新按钮对应的进度条
        mUpdateBtn.setOnClickListener(this);

        if(NetUtil.getNetworkState(this) != NetUtil.NETWOR_NONE){
            Log.d("myWeather","网络OK");
            Toast.makeText(MainActivity.this,"网络OK!",Toast.LENGTH_LONG).show();
        }else{
            Log.d("myWeather","网络挂了");
            Toast.makeText(MainActivity.this,"网络挂了！",Toast.LENGTH_LONG).show();
        }

        //选择城市
        mCitySelect = (ImageView) findViewById(R.id.title_city_manager);
        mCitySelect.setOnClickListener(this);

        //定位按钮

        mLocateBtn = (ImageView) findViewById(R.id.title_location);
        mLocateBtn.setOnClickListener(this);

        //程序第一次执行，跳转到引导界面
        SharedPreferences preferences = getSharedPreferences("count", MODE_PRIVATE);
        int count = preferences.getInt("count", 0);
        //判断程序第几次运行，如果是第一次运行则跳转到引导界面
        if(count == 0){
            Intent i = new Intent(MainActivity.this, Guide.class);
            startActivity(i);
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("count", ++count);
        editor.commit();

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
        String type = sp.getString("type", "晴");


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
        updateWeatherPIC(type, weatherImg);

        //初始化viewPager
        LayoutInflater inflater = LayoutInflater.from(this);
        //LayoutInflater加载布局
        View one_page = inflater.inflate(R.layout.weatherpage1, null);
        View two_page = inflater.inflate(R.layout.weatherpage2, null);
        views2.add(one_page);
        views2.add(two_page);
        //为ViewPager添加适配器
        vpAdapter2 = new ViewPagerAdapter(views2, this);
        //获取ViewPager对象，指定其适配器
        vp2 = (ViewPager) findViewById(R.id.mViewpager);
        vp2.setAdapter(vpAdapter2);
        vp2.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                for(int a = 0; a < ids2.length; a++){
                    if(a == i){//设置选中效果
                        dots2[a].setImageResource(R.drawable.page_indicator_focused);
                    }else{//设置未选中效果
                        dots2[a].setImageResource(R.drawable.page_indicator_unfocused);
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        //增加页面变化的监听事件，动态修改导航小圆点的属性
        dots2 = new ImageView[views2.size()];
        for(int i = 0; i < views2.size(); i++){
            dots2[i] = (ImageView)findViewById(ids2[i]);
        }

        //未来四天日期
        date1 = (TextView) one_page.findViewById(R.id.date01);
        date2 = (TextView) one_page.findViewById(R.id.date02);
        date3 = (TextView) two_page.findViewById(R.id.date03);
        date4 = (TextView) two_page.findViewById(R.id.date04);

        String d1 = sp.getString("date1","N/A");
        String d2 = sp.getString("date2","N/A");
        String d3 = sp.getString("date3","N/A");
        String d4 = sp.getString("date4","N/A");

        date1.setText(d1);
        date2.setText(d2);
        date3.setText(d3);
        date4.setText(d4);

        //未来四天天气状况图片
        weatherImg1 = (ImageView) one_page.findViewById(R.id.weather01_pic);//明天
        weatherImg2 = (ImageView) one_page.findViewById(R.id.weather02_pic);//后天
        weatherImg3 = (ImageView) two_page.findViewById(R.id.weather03_pic);//第三天
        weatherImg4 = (ImageView) two_page.findViewById(R.id.weather04_pic);//第四天

        String type1 = sp.getString("type1", "晴");
        String type2 = sp.getString("type2", "晴");
        String type3 = sp.getString("type3", "晴");
        String type4 = sp.getString("type4", "晴");

        updateWeatherPIC(type1, weatherImg1);
        updateWeatherPIC(type2, weatherImg2);
        updateWeatherPIC(type3, weatherImg3);
        updateWeatherPIC(type4, weatherImg4);

        //未来四天最高温度、最低温度
        temperatureTv1 = (TextView) one_page.findViewById(R.id.weather01);//明天
        temperatureTv2 = (TextView) one_page.findViewById(R.id.weather02);//后天
        temperatureTv3 = (TextView) two_page.findViewById(R.id.weather03);//第三天
        temperatureTv4 = (TextView) two_page.findViewById(R.id.weather04);//第四天

        String temperaturetv1 = sp.getString("temperature1Tv","N/A");
        String temperaturetv2 = sp.getString("temperature2Tv","N/A");
        String temperaturetv3 = sp.getString("temperature3Tv","N/A");
        String temperaturetv4 = sp.getString("temperature4Tv","N/A");

        temperatureTv1.setText(temperaturetv1);
        temperatureTv2.setText(temperaturetv2);
        temperatureTv3.setText(temperaturetv3);
        temperatureTv4.setText(temperaturetv4);

        //未来四天天气状况
        type1Tv = (TextView) one_page.findViewById(R.id.weather01_tips);//明天
        type2Tv = (TextView) one_page.findViewById(R.id.weather02_tips);//后天
        type3Tv = (TextView) two_page.findViewById(R.id.weather03_tips);//第三天
        type4Tv = (TextView) two_page.findViewById(R.id.weather04_tips);//第四天

        String type1tv = sp.getString("type1Tv","N/A");
        String type2tv = sp.getString("type2Tv","N/A");
        String type3tv = sp.getString("type3Tv","N/A");
        String type4tv = sp.getString("type4Tv","N/A");

        type1Tv.setText(type1tv);
        type2Tv.setText(type2tv);
        type3Tv.setText(type3tv);
        type4Tv.setText(type4tv);

        //未来四天风力
        feng1 = (TextView) one_page.findViewById(R.id.fengli01);
        feng2 = (TextView) one_page.findViewById(R.id.fengli02);
        feng3 = (TextView) two_page.findViewById(R.id.fengli03);
        feng4 = (TextView) two_page.findViewById(R.id.fengli04);

        String fengli1 = sp.getString("fengli1","N/A");
        String fengli2 = sp.getString("fengli2","N/A");
        String fengli3 = sp.getString("fengli3","N/A");
        String fengli4 = sp.getString("fengli4","N/A");

        feng1.setText(fengli1);
        feng2.setText(fengli2);
        feng3.setText(fengli3);
        feng4.setText(fengli4);

    }

    //加载数据库中所有城市数据
    private void initdata(){
        cityList = MyApplication.getInstance().getCityList();
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
            mUpdateBtn.setVisibility(View.GONE);
            pbForUpdate.setVisibility(View.VISIBLE);

            Log.d("myWeather",newCityCode);

            if(NetUtil.getNetworkState(this) != NetUtil.NETWOR_NONE){
                Log.d("myWeather","网络OK");
                queryWeatherCode(newCityCode);
            }else{
                Log.d("myWeather","网络挂了");
                Toast.makeText(MainActivity.this,"网络挂了！",Toast.LENGTH_LONG).show();
            }
        }

        //用户点击定位按钮后的结果
        if(view.getId() == R.id.title_location){

            //声明LocationClient类
            mLocationClient = new LocationClient(getApplicationContext());
            mLocationClient.registerLocationListener(mLocationListener);
            //注册监听器函数
            LocationClientOption option = new LocationClientOption();
            option.setIsNeedAddress(true); //是否需要地址信息
            mLocationClient.setLocOption(option);
            //加载数据库中所有城市数据
            initdata();
            //启动百度地图定位service
            Intent intent = new Intent(this, LocationService.class);
            myServiceConn = new MyServiceConn();
            startService(intent); //开启LocationService
            bindService(intent, myServiceConn, Context.BIND_AUTO_CREATE);//绑定service

            //获取定位到的城市的cityCode,更新页面城市天气信息
            SharedPreferences sharedPreferences = getSharedPreferences("locationCity", MODE_PRIVATE);
            String citycode = sharedPreferences.getString("location_cityCode", "101010100");

            queryWeatherCode(citycode);
            newCityCode = citycode;
            Toast.makeText(MainActivity.this,"已定位到当前位置所在城市!",Toast.LENGTH_SHORT).show();

        }
    }

    //用户点击选择城市按钮后该函数被触发
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == 1 && resultCode == RESULT_OK){
            //从Intent中获取用户在SelectActivity中选择的城市的cityCode,并根据该cityCode更新数据
            String cityCode = data.getStringExtra("cityCode");

            newCityCode = cityCode;

            Log.d("myWeather","选择的城市代码为"+cityCode);

            if(NetUtil.getNetworkState(this) != NetUtil.NETWOR_NONE){
                Log.d("myWeather","网络OK");
                queryWeatherCode(cityCode);
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
                    editor.putString("type",todayWeather.getType());

                    //未来四天日期
                    editor.putString("date1", todayWeather.getDate1());
                    editor.putString("date2", todayWeather.getDate2());
                    editor.putString("date3", todayWeather.getDate3());
                    editor.putString("date4", todayWeather.getDate4());
                    editor.putString("date5", todayWeather.getDate5());
                    editor.putString("date6", todayWeather.getDate6());

                    //未来四天最高温度~最低温度
                    editor.putString("temperature1Tv",todayWeather.getHigh1()+"~"+ todayWeather.getLow1());
                    editor.putString("temperature2Tv",todayWeather.getHigh2()+"~"+ todayWeather.getLow2());
                    editor.putString("temperature3Tv",todayWeather.getHigh3()+"~"+ todayWeather.getLow3());
                    editor.putString("temperature4Tv",todayWeather.getHigh4()+"~"+ todayWeather.getLow4());
                    editor.putString("temperature5Tv",todayWeather.getHigh5()+"~"+ todayWeather.getLow5());
                    editor.putString("temperature6Tv",todayWeather.getHigh6()+"~"+ todayWeather.getLow6());

                    //未来四天天气状况
                    editor.putString("type1Tv",todayWeather.getType1());
                    editor.putString("type2Tv",todayWeather.getType2());
                    editor.putString("type3Tv",todayWeather.getType3());
                    editor.putString("type4Tv",todayWeather.getType4());
                    editor.putString("type5Tv",todayWeather.getType5());
                    editor.putString("type6Tv",todayWeather.getType6());

                    //未来四天风
                    editor.putString("fengli1","风力:"+todayWeather.getFengli1());
                    editor.putString("fengli2","风力:"+todayWeather.getFengli2());
                    editor.putString("fengli3","风力:"+todayWeather.getFengli3());
                    editor.putString("fengli4","风力:"+todayWeather.getFengli4());
                    editor.putString("fengli5","风力:"+todayWeather.getFengli5());
                    editor.putString("fengli6","风力:"+todayWeather.getFengli6());

                    editor.commit();

                    if(todayWeather != null){
                        Log.d("myWeather", todayWeather.toString());
                        Message msg = new Message();
                        msg.what = UPDATE_TODAY_WEATHER; //传递给主进程
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
                            }
                            else if (xmlPullParser.getName().equals("updatetime")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setUpdatetime(xmlPullParser.getText());
                            }
                            else if (xmlPullParser.getName().equals("shidu")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setShidu(xmlPullParser.getText());
                            }
                            else if (xmlPullParser.getName().equals("wendu")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setWendu(xmlPullParser.getText());
                            }
                            else if (xmlPullParser.getName().equals("pm25")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setPm25(xmlPullParser.getText());
                            }
                            else if (xmlPullParser.getName().equals("quality")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setQuality(xmlPullParser.getText());
                            }
                            //今天风向
                            else if (xmlPullParser.getName().equals("fengxiang") && fengxiangCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengxiang(xmlPullParser.getText());
                                fengxiangCount++;
                            }
                            //未来四天风向
                            //明天风向
                            else if (xmlPullParser.getName().equals("fengxiang") && fengxiangCount == 1) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengxiang1(xmlPullParser.getText());
                                fengxiangCount++;
                            }
                            //后天风向
                            else if (xmlPullParser.getName().equals("fengxiang") && fengxiangCount == 2) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengxiang2(xmlPullParser.getText());
                                fengxiangCount++;
                            }
                            //第三天风向
                            else if (xmlPullParser.getName().equals("fengxiang") && fengxiangCount == 3) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengxiang3(xmlPullParser.getText());
                                fengxiangCount++;
                            }
                            //第四天风向
                            else if (xmlPullParser.getName().equals("fengxiang") && fengxiangCount == 4) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengxiang4(xmlPullParser.getText());
                                fengxiangCount++;
                            }
                            //第五天风向
                            else if (xmlPullParser.getName().equals("fengxiang") && fengxiangCount == 5) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengxiang5(xmlPullParser.getText());
                                fengxiangCount++;
                            }
                            //第六天风向
                            else if (xmlPullParser.getName().equals("fengxiang") && fengxiangCount == 6) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengxiang6(xmlPullParser.getText());
                                fengxiangCount++;
                            }
                            //今天风力
                            else if (xmlPullParser.getName().equals("fengli") && fengliCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengli(xmlPullParser.getText());
                                fengliCount++;
                            }
                            //未来四天风力
                            //明天
                            else if (xmlPullParser.getName().equals("fengli") && fengliCount == 1) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengli1(xmlPullParser.getText());
                                fengliCount++;
                            }
                            //后天
                            else if (xmlPullParser.getName().equals("fengli") && fengliCount == 2) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengli2(xmlPullParser.getText());
                                fengliCount++;
                            }
                            //第三天
                            else if (xmlPullParser.getName().equals("fengli") && fengliCount == 3) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengli3(xmlPullParser.getText());
                                fengliCount++;
                            }
                            //第四天
                            else if (xmlPullParser.getName().equals("fengli") && fengliCount == 4) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengli4(xmlPullParser.getText());
                                fengliCount++;
                            }
                            //第五天
                            else if (xmlPullParser.getName().equals("fengli") && fengliCount == 5) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengli5(xmlPullParser.getText());
                                fengliCount++;
                            }
                            //第六天
                            else if (xmlPullParser.getName().equals("fengli") && fengliCount == 6) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengli6(xmlPullParser.getText());
                                fengliCount++;
                            }
                            //今天日期
                            else if (xmlPullParser.getName().equals("date") && dateCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setDate(xmlPullParser.getText());
                                dateCount++;
                            }
                            //未来四天日期
                            //明天
                            else if (xmlPullParser.getName().equals("date") && dateCount == 1) {
                                eventType = xmlPullParser.next();
                                todayWeather.setDate1(xmlPullParser.getText());
                                dateCount++;
                            }
                            //后天
                            else if (xmlPullParser.getName().equals("date") && dateCount == 2) {
                                eventType = xmlPullParser.next();
                                todayWeather.setDate2(xmlPullParser.getText());
                                dateCount++;
                            }
                            //第三天
                            else if (xmlPullParser.getName().equals("date") && dateCount == 3) {
                                eventType = xmlPullParser.next();
                                todayWeather.setDate3(xmlPullParser.getText());
                                dateCount++;
                            }
                            //第四天
                            else if (xmlPullParser.getName().equals("date") && dateCount == 4) {
                                eventType = xmlPullParser.next();
                                todayWeather.setDate4(xmlPullParser.getText());
                                dateCount++;
                            }
                            //第五天
                            else if (xmlPullParser.getName().equals("date") && dateCount == 5) {
                                eventType = xmlPullParser.next();
                                todayWeather.setDate5(xmlPullParser.getText());
                                dateCount++;
                            }
                            //第六天
                            else if (xmlPullParser.getName().equals("date") && dateCount == 6) {
                                eventType = xmlPullParser.next();
                                todayWeather.setDate6(xmlPullParser.getText());
                                dateCount++;
                            }
                            //今天最高气温
                            else if (xmlPullParser.getName().equals("high") && highCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setHigh(xmlPullParser.getText().substring(2).trim());
                                highCount++;
                            }
                            //未来四天最高气温
                            //明天
                            else if (xmlPullParser.getName().equals("high") && highCount == 1) {
                                eventType = xmlPullParser.next();
                                todayWeather.setHigh1(xmlPullParser.getText().substring(2).trim());
                                highCount++;
                            }
                            //后天
                            else if (xmlPullParser.getName().equals("high") && highCount == 2) {
                                eventType = xmlPullParser.next();
                                todayWeather.setHigh2(xmlPullParser.getText().substring(2).trim());
                                highCount++;
                            }
                            //第三天
                            else if (xmlPullParser.getName().equals("high") && highCount == 3) {
                                eventType = xmlPullParser.next();
                                todayWeather.setHigh3(xmlPullParser.getText().substring(2).trim());
                                highCount++;
                            }
                            //第四天
                            else if (xmlPullParser.getName().equals("high") && highCount == 4) {
                                eventType = xmlPullParser.next();
                                todayWeather.setHigh4(xmlPullParser.getText().substring(2).trim());
                                highCount++;
                            }
                            //第五天
                            else if (xmlPullParser.getName().equals("high") && highCount == 5) {
                                eventType = xmlPullParser.next();
                                todayWeather.setHigh5(xmlPullParser.getText().substring(2).trim());
                                highCount++;
                            }
                            //第六天
                            else if (xmlPullParser.getName().equals("high") && highCount == 6) {
                                eventType = xmlPullParser.next();
                                todayWeather.setHigh6(xmlPullParser.getText().substring(2).trim());
                                highCount++;
                            }
                            //今天最低气温
                            else if (xmlPullParser.getName().equals("low") && lowCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setLow(xmlPullParser.getText().substring(2).trim());
                                lowCount++;
                            }
                            //未来四天最低气温
                            //明天
                            else if (xmlPullParser.getName().equals("low") && lowCount == 1) {
                                eventType = xmlPullParser.next();
                                todayWeather.setLow1(xmlPullParser.getText().substring(2).trim());
                                lowCount++;
                            }
                            //后天
                            else if (xmlPullParser.getName().equals("low") && lowCount == 2) {
                                eventType = xmlPullParser.next();
                                todayWeather.setLow2(xmlPullParser.getText().substring(2).trim());
                                lowCount++;
                            }
                            //第三天
                            else if (xmlPullParser.getName().equals("low") && lowCount == 3) {
                                eventType = xmlPullParser.next();
                                todayWeather.setLow3(xmlPullParser.getText().substring(2).trim());
                                lowCount++;
                            }
                            //第四天
                            else if (xmlPullParser.getName().equals("low") && lowCount == 4) {
                                eventType = xmlPullParser.next();
                                todayWeather.setLow4(xmlPullParser.getText().substring(2).trim());
                                lowCount++;
                            }
                            //第五天
                            else if (xmlPullParser.getName().equals("low") && lowCount == 5) {
                                eventType = xmlPullParser.next();
                                todayWeather.setLow5(xmlPullParser.getText().substring(2).trim());
                                lowCount++;
                            }
                            //第六天
                            else if (xmlPullParser.getName().equals("low") && lowCount == 6) {
                                eventType = xmlPullParser.next();
                                todayWeather.setLow6(xmlPullParser.getText().substring(2).trim());
                                lowCount++;
                            }
                            //今天天气状况
                            else if (xmlPullParser.getName().equals("type") && typeCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setType(xmlPullParser.getText());
                                typeCount++;
                            }
                            //未来四天天气状况
                            //明天
                            else if (xmlPullParser.getName().equals("type") && typeCount == 1) {
                                eventType = xmlPullParser.next();
                                todayWeather.setType1(xmlPullParser.getText());
                                typeCount++;
                            }
                            //后天
                            else if (xmlPullParser.getName().equals("type") && typeCount == 2) {
                                eventType = xmlPullParser.next();
                                todayWeather.setType2(xmlPullParser.getText());
                                typeCount++;
                            }
                            //第三天
                            else if (xmlPullParser.getName().equals("type") && typeCount == 3) {
                                eventType = xmlPullParser.next();
                                todayWeather.setType3(xmlPullParser.getText());
                                typeCount++;
                            }
                            //第四天
                            else if (xmlPullParser.getName().equals("type") && typeCount == 4) {
                                eventType = xmlPullParser.next();
                                todayWeather.setType4(xmlPullParser.getText());
                                typeCount++;
                            }
                            //第五天
                            else if (xmlPullParser.getName().equals("type") && typeCount == 5) {
                                eventType = xmlPullParser.next();
                                todayWeather.setType5(xmlPullParser.getText());
                                typeCount++;
                            }
                            //第六天
                            else if (xmlPullParser.getName().equals("type") && typeCount == 6) {
                                eventType = xmlPullParser.next();
                                todayWeather.setType6(xmlPullParser.getText());
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
        //更新今日天气信息
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

        //更新未来四天的日期
        date1.setText(todayWeather.getDate1());
        date2.setText(todayWeather.getDate2());
        date3.setText(todayWeather.getDate3());
        date4.setText(todayWeather.getDate4());

        //更新未来四天的最高~最低温度
        temperatureTv1.setText(todayWeather.getHigh1()+"~"+ todayWeather.getLow1());
        temperatureTv2.setText(todayWeather.getHigh2()+"~"+ todayWeather.getLow2());
        temperatureTv3.setText(todayWeather.getHigh3()+"~"+ todayWeather.getLow3());
        temperatureTv4.setText(todayWeather.getHigh4()+"~"+ todayWeather.getLow4());

        //更新未来四天的天气状况
        type1Tv.setText(todayWeather.getType1());
        type2Tv.setText(todayWeather.getType2());
        type3Tv.setText(todayWeather.getType3());
        type4Tv.setText(todayWeather.getType4());

        //更新未来四天的风情况
        feng1.setText("风力:"+todayWeather.getFengli1());
        feng2.setText("风力:"+todayWeather.getFengli2());
        feng3.setText("风力:"+todayWeather.getFengli3());
        feng4.setText("风力:"+todayWeather.getFengli4());


        //更新今日天气图片
        updateWeatherPIC(todayWeather.getType(), weatherImg);
        //更新未来四天天气照片
        updateWeatherPIC(todayWeather.getType1(), weatherImg1);
        updateWeatherPIC(todayWeather.getType2(), weatherImg2);
        updateWeatherPIC(todayWeather.getType3(), weatherImg3);
        updateWeatherPIC(todayWeather.getType4(), weatherImg4);


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
        mUpdateBtn.setVisibility(View.VISIBLE);
        pbForUpdate.setVisibility(View.GONE);
    }

    //根据天气状况更新照片
    void updateWeatherPIC(String weather, ImageView weatherImg){
        ImageView wImg = weatherImg;
        Bitmap bmweather;
        if(weather!=null){
            if(weather.equals("暴雪")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_baoxue);
                wImg.setImageBitmap(bmweather);
            }else if(weather.equals("暴雨")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_baoyu);
                wImg.setImageBitmap(bmweather);
            }else if(weather.equals("大暴雨")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_dabaoyu);
                wImg.setImageBitmap(bmweather);
            }else if(weather.equals("大雪")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_daxue);
                wImg.setImageBitmap(bmweather);
            }else if(weather.equals("大雨")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_dayu);
                wImg.setImageBitmap(bmweather);
            }else if(weather.equals("多云")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_duoyun);
                wImg.setImageBitmap(bmweather);
            }
            else if(weather.equals("雷阵雨")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_leizhenyu);
                wImg.setImageBitmap(bmweather);
            }
            else if(weather.equals("雷阵雨冰雹")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_leizhenyubingbao);
                wImg.setImageBitmap(bmweather);
            }else if(weather.equals("晴")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_qing);
                wImg.setImageBitmap(bmweather);
            }else if(weather.equals("沙尘暴")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_shachenbao);
                wImg.setImageBitmap(bmweather);
            }else if(weather.equals("特大暴雨")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_tedabaoyu);
                wImg.setImageBitmap(bmweather);
            }else if(weather.equals("雾")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_wu);
                wImg.setImageBitmap(bmweather);
            }else if(weather.equals("晴")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_qing);
                wImg.setImageBitmap(bmweather);
            }else if(weather.equals("小雪")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_xiaoxue);
                wImg.setImageBitmap(bmweather);
            }else if(weather.equals("小雨")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_xiaoyu);
                wImg.setImageBitmap(bmweather);
            }else if(weather.equals("阴")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_yin);
                wImg.setImageBitmap(bmweather);
            }else if(weather.equals("雨夹雪")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_yujiaxue);
                wImg.setImageBitmap(bmweather);
            }else if(weather.equals("阵雪")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_zhenxue);
                wImg.setImageBitmap(bmweather);
            }else if(weather.equals("阵雨")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_zhenyu);
                wImg.setImageBitmap(bmweather);
            }else if(weather.equals("中雪")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_zhongxue);
                wImg.setImageBitmap(bmweather);
            }else if(weather.equals("中雨")){
                bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_zhongyu);
                wImg.setImageBitmap(bmweather);
            }
        }else{
            bmweather = BitmapFactory.decodeResource(getResources(),R.drawable.biz_plugin_weather_qing);
            wImg.setImageBitmap(bmweather);
        }
    }

    //定位服务监听器类MyLocationListener，继承了BDAbstractLocationListener
    class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location){
            //此处的BDLocation为定位结果信息类，通过它的各种get方法可获取定位相关的全部结果
            String city = location.getCity(); //获取城市
            String district = location.getDistrict(); //获取区县
            cityname = city;
            Log.d("myinfo", cityname+"---first");
        }
    }

    //负责MainActivity和LocationService进行通信的类
    class MyServiceConn implements ServiceConnection{
        //服务被绑定成功之后执行
        @Override
        public void onServiceConnected(ComponentName name, IBinder service){
            //IBinder service为OnBind方法返回的Service实例
            binder = (LocationService.MyBinder) service;
            binder.getService().setDataCallback(new LocationService.DataCallback() {
                @Override
                public void datachanged(String str) {
                    Message msg = new Message();
                    msg.obj = str;
                    msg.what = UPDATE_LOCATION;
                    //发送通知
                    mHandler.sendMessage(msg);
                }
            });
        }

        //服务崩溃或者被杀掉执行
        @Override
        public void onServiceDisconnected(ComponentName name){
            System.out.println("-----------stop-----------");
            binder = null;
        }
    }

    //下面两个方法供友盟统计使用
    @Override
    protected void onResume(){
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause(){
        super.onPause();
        MobclickAgent.onPause(this);
    }
}
