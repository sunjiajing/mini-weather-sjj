package cn.edu.pku.sunjiajing.miniweather;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.edu.pku.sunjiajing.app.MyApplication;
import cn.edu.pku.sunjiajing.bean.City;

public class SelectCity extends Activity {

    private ImageView mBackBtn;

    private ListView mListView;

    private List<City> allCities; //用于存放数据库中获取的所有城市数据
    private ArrayList<City> filterDataList; //存储根据输入框内容对所有的城市进行过滤，过滤后的数据

    private TextView titleName;

    private ClearEditText eSearch; //输入框
    private SelectCityAdapter adapter; //ListView的适配器

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.select_city);

        //从sharedPreferences中获取当前城市的城市名称，用于标题显示当前城市的名称
        SharedPreferences sharedPreferences = getSharedPreferences("currentCity",MODE_PRIVATE);
        String cityName = sharedPreferences.getString("cityName","北京");
        titleName = (TextView)findViewById(R.id.title_name);
        titleName.setText("当前城市："+cityName);

        //返回按钮
        mBackBtn = (ImageView)findViewById(R.id.title_back);
        //返回按钮设置ClickListener,在此处并未将用户选择的城市信息返回到MainActivity,而是在用户点击所选择的城市时，就将所选城市数据返回
        mBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        //ListView显示城市列表
        adapter = new SelectCityAdapter();
        mListView = (ListView)findViewById(R.id.title_list);
        mListView.setAdapter(adapter);
        //设置该ListView的OnItemClickListener，在OnItemClickListener中指定当用户所选择的子项目被点击时触发该函数，将子项目的城市数据返回到MainActivity
        mListView.setOnItemClickListener(new ItemClickEvent());

        //监听搜索框文本，根据搜索框文本内容的变化过滤城市数据，并更新ListView展示的城市
        eSearch = (ClearEditText)findViewById(R.id.search_city);
        eSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //这个方法被调用，说明在s字符串中，从start位置开始的count个字符即将被长度为after的新文本所取代。在这个方法里面改变s，会报错。
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //搜索匹配
                //这个方法被调用，说明在s字符串中，从start位置开始的count个字符刚刚取代了长度为before的旧文本。在这个方法里面改变s，会报错。

                //根据输入框中的值来过滤数据，过滤后的数据匹配用户输入，此步骤ListView的Adapter会更新数据
                filterData(s.toString());
                //ListView将过滤后的数据展示给用户
                mListView.setAdapter(adapter);
            }

            @Override
            public void afterTextChanged(Editable s) {
                //这个方法被调用，说明s字符串的某个地方已经被改变
                String txt = eSearch.getText().toString();
                Pattern p = Pattern.compile("[A-Z]]");//根据城市首字母（大写）来过滤数据
                Matcher m = p.matcher(txt);
                if (!m.matches()){
                    Toast.makeText(SelectCity.this,"请输入大写字母", Toast.LENGTH_SHORT).show();
                }

            }

            //根据输入框中的值来过滤数据并更新ListView
            private void filterData(String filterStr){
                filterDataList = new ArrayList<City>();
                Log.d("filters", filterStr);
                //如果输入框内容为空，将所有的城市通过ListView展示
                if(TextUtils.isEmpty(filterStr)){
                    for(City city:allCities){
                        filterDataList.add(city);
                    }
                }else{
                    //将filterDataList置空，用以存储过滤后的新数据
                    filterDataList.clear();
                    //根据输入框中的内容开始过滤
                    for(City city:allCities){
                        if (city.getAllPY().indexOf(filterStr.toString().trim())!= -1|| city.getAllFristPY().indexOf(filterStr.toString().trim())!= -1){
                            filterDataList.add(city);
                        }
                    }
                }
                //通过adapter更新ListView的数据
                adapter.updateListView(filterDataList);
            }
        });

    }


    //继承OnItemClickListener, 当ListView中的子项目被点击的时候触发该函数
    private final class ItemClickEvent implements AdapterView.OnItemClickListener{

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id){

            //将用户选择的城市的CityCode和CityName存放到sharedPreferences中，以供当前Activity的标题显示，以及MainActivity根据回传的CityCode更新城市信息
            SharedPreferences sharedPreferences = getSharedPreferences("currentCity",MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            City city = (City)adapter.getItem(position);
            editor.putString("cityCode",city.getNumber());
            editor.putString("cityName",city.getCity()+"，"+city.getProvince()+" ("+city.getAllPY()+") ");
            editor.commit();

            //用户触发通过Intent将用户选择的的城市的cityCode传回到MainActivity，用户点击添加城市按钮后的结果时MainActivity根据该cityCode更新数据
            Intent intent = new Intent();
            intent.putExtra("cityCode", ((City)adapter.getItem(position)).getNumber());
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    //给ListView设置的适配器，继承BaseAdapter，该适配器可以根据输入框内容的变化更新ListView展示的内容
    class SelectCityAdapter extends BaseAdapter{
        private List<City> filterCityList;//存放根据输入框内容过滤后的新数据

        public SelectCityAdapter(){
            //初始创建对象的时候，allCities存放所有的城市数据
            allCities = MyApplication.getInstance().getCityList();
            filterCityList = allCities;
        }

        //根据输入框的北荣过滤后的数据存成列表newfilter，ListView进行更新，展示将newfilter中的数据
        public void updateListView(ArrayList<City> newfilter){
            filterCityList = newfilter;
            this.notifyDataSetChanged();
        }

        //ListView展示数据的长度
        @Override
        public int getCount(){
            return filterCityList.size();
        }

        //用户选择的ListView中的子项目数据
        @Override
        public Object getItem(int position){
            return filterCityList.get(position);
        }

        //用户选择的ListView中的子项目的ID
        @Override
        public  long getItemId(int position){
            return position;
        }

        //该函数用于通过ListView展示城市数据
        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            City city = filterCityList.get(position);
            View view = View.inflate(SelectCity.this, R.layout.listview_item, null);
            TextView cityName = (TextView) view.findViewById(R.id.cityName);
            TextView cityCode = (TextView) view.findViewById(R.id.cityCode);
            cityName.setText(city.getCity()+"，"+city.getProvince()+" ("+city.getAllPY()+") ");
            cityCode.setText(city.getNumber());
            return view;
        }
    }
}
