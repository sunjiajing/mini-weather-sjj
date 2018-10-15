package cn.edu.pku.sunjiajing.miniweather;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import cn.edu.pku.sunjiajing.app.MyApplication;
import cn.edu.pku.sunjiajing.bean.City;

public class SelectCity extends Activity implements View.OnClickListener{

    private ImageView mBackBtn;


//    private ClearEditText mClearEditText;
//
//    private ListView mList;
//
//    private List<City> cityList;
//
//    private Myadapter myadapter;

//    protected void search(){
//        mClearEditText = (ClearEditText) findViewById(R.id.search_city);
//
//        //根据输入框的值的改变来过滤搜索
//        mClearEditText.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                filterData(s.toString());
//                mList.setAdapter(myadapter);
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//
//            }
//        });
//    }

//    private void filterData(String filterStr){
//        filterDateList = new ArrayList<City>();
//
//        Log.d("Filter", filterStr);
//
//        if(TextUtils.isEmpty(filterStr)){
//            for(City city: cityList){
//                filterDateList.add(city);
//            }
//        }else{
//            filterDateList.clear();
//            for(City city: cityList){
//                if(city.getCity().indexOf(filterStr.toString() != -1)){
//                    filterDateList.add(city);
//                }
//            }
//        }
//
//
//        myadapter.updateListView(filterDateList);
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.select_city);

        //initViews();

        mBackBtn = (ImageView) findViewById(R.id.title_back);
        mBackBtn.setOnClickListener(this);
    }

//    protected void initViews(){
//        //为mBackBtn设置监听事件
//        mBackBtn = (ImageView) findViewById(R.id.title_back);
//        mBackBtn.setOnClickListener(this);
//
//        mClearEditText = (ClearEditText) findViewById(R.id.search_city);
//
//        mList = (ListView) findViewById(R.id.title_list);
//        MyApplication myApplication = (MyApplication) getApplication();
//        cityList = myApplication.getCityList();
//        for(City city : cityList){
//            filterDataList.add(city);
//        }
//
//        myadapter = new Myadapter(SelectCity.this, cityList);
//        mList.setAdapter(myadapter);
//        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                City city = filterDataList.get(position);
//                Intent i = new Intent();
//                i.putExtra("cityCode", city.getNumber());
//                setResult(RESULT_OK, i);
//                finish();
//            }
//        });
//
//    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.title_back:
                Intent i = new Intent();
                i.putExtra("cityCode","101160101");
                setResult(RESULT_OK, i);
                finish();
                break;
            default:
                break;
        }
    }
}
