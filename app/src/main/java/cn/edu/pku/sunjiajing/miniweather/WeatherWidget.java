package cn.edu.pku.sunjiajing.miniweather;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import cn.edu.pku.sunjiajing.bean.TodayWeather;

public class WeatherWidget extends AppWidgetProvider {
    public static TodayWeather todayWeather = new TodayWeather();
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId){
        CharSequence widgetText = "天气预报";

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget_layout);
        views.setTextViewText(R.id.city_name, widgetText);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    public static RemoteViews updateRemoteViews(Context context){
        RemoteViews view = new RemoteViews(context.getPackageName(),R.layout.weather_widget_layout);
        if(null == todayWeather){
            return null;
        }else{
            view.setTextViewText(R.id.city_name, todayWeather.getCity());
            view.setTextViewText(R.id.cloud, todayWeather.getFengxiang());
            view.setTextViewText(R.id.cur_temp, todayWeather.getType());
            view.setTextViewText(R.id.low_temp, "低" +todayWeather.getLow());
            view.setTextViewText(R.id.high_temp, "低" +todayWeather.getHigh());

        }
        return view;
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds){
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent){
        super.onReceive(context, intent);
    }

    private void updateWidget(Context context, long time){
        //RemoteViews处理异进程中的View
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.weather_widget_layout);
        System.out.println("time="+time);

        AppWidgetManager am = AppWidgetManager.getInstance(context);
        int [] appWidgetIds = am.getAppWidgetIds(new ComponentName(context, WeatherWidget.class));
        am.updateAppWidget(appWidgetIds, rv);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int []appWidgetIds){
//        MainActivity.appWidgetIds = appWidgetIds;
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onEnabled(Context context){
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context){
        super.onDisabled(context);
    }
}
