package cn.edu.pku.sunjiajing.miniweather;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * ViewPager的适配器
 * ViewPager将调用它来取得所需显示的页，而 PageAdapter也会在数据变化时，通知 ViewPager。
 */

public class ViewPagerAdapter extends PagerAdapter {

    private String TAG = "ViewPager";

    private List<View> views;
    private Context context;

    public ViewPagerAdapter(List<View> views, Context context){
        this.views = views;
        this.context = context;
    }

    //用于创建position位置的视图
    //当要显示的图片可以进行缓存的时候，会调用这个方法进行显示图片的初始化，我们将要显示的ImageView加入到ViewGroup中，然后作为返回值返回即可
    @Override
    public Object instantiateItem(ViewGroup container, int position){
        //添加position位置的视图
        container.addView(views.get(position));
        //返回对象
        return views.get(position);
    }

    //删除position位置指定的视图
    //PagerAdapter只缓存三张要显示的图片，如果滑动的图片超过了缓存的范围，就会调用这个方法，将图片销毁
    @Override
    public void destroyItem(ViewGroup container, int position, Object object){
        container.removeView(views.get(position));
    }

    //返回的对象是否与当前View代表同一对象
    @Override
    public boolean isViewFromObject(View view, Object object){
        return (view == object);
    }

    //返回滑动视图的个数
    @Override
    public int getCount() {
        int num = views.size();
        return num;
    }

    @Override
    public void finishUpdate(ViewGroup container){
        super.finishUpdate(container);
    }

}
