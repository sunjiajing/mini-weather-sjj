package cn.edu.pku.sunjiajing.miniweather;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class Guide extends Activity implements ViewPager.OnPageChangeListener {
    private String TAG = "ViewPager";
    private ViewPagerAdapter vpAdapter;
    private ViewPager vp;

    //导航视图的3个布局文件
    private List<View> views = new ArrayList<View>();
    private ImageView[] dots;
    //存放3个导航圆点的id
    private int[] ids = {R.id.iv1, R.id.iv2};
    private Button start_btn;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guide);
        initViews(); //动态的加载，需要在ViewPager中显示的视图
        initDots();//将三个导航圆点对象存入数组中

        //viewpager中关联控件
        start_btn = (Button) views.get(1).findViewById(R.id.start_btn);
        start_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Guide.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    //动态的加载，需要在ViewPager中显示的视图
    private void initViews(){
        //初始化viewPager，获取LayoutInflater类型对象
        LayoutInflater inflater = LayoutInflater.from(this);

        //LayoutInflater用于加载布局
        View page1 = inflater.inflate(R.layout.page1, null);
        View page2 = inflater.inflate(R.layout.page2, null);
        views.add(page1);
        views.add(page2);

        //为ViewPager添加适配器
        vpAdapter = new ViewPagerAdapter(views, this);
        //获取ViewPager对象，指定其适配器
        vp = (ViewPager) findViewById(R.id.viewpager);
        vp.setAdapter(vpAdapter);
        vp.setOnPageChangeListener(this);

    }

    //将两个导航圆点对象存入数组中
    void initDots(){
        dots = new ImageView[views.size()];
        for(int i = 0; i < views.size(); i++){
            dots[i] = (ImageView) findViewById(ids[i]);
        }
    }

    //动态修改小圆点的属性，即可实现相应的导航效果
    @Override
    public void onPageSelected(int i){
        for(int a = 0; a < ids.length; a++){
            if(a == i){//设置选中效果
                dots[a].setImageResource(R.drawable.page_indicator_focused);
            }else{//设置未选中效果
                dots[a].setImageResource(R.drawable.page_indicator_unfocused);
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state){

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels){

    }

}
