package me.baron.weatherstyle.presenter;

import android.content.Context;

import java.sql.SQLException;

import javax.inject.Inject;

import me.baron.library.utils.NetworkUtil;
import me.baron.weatherstyle.WeatherApp;
import me.baron.weatherstyle.contract.HomePageContract;
import me.baron.weatherstyle.model.db.dao.WeatherDao;
import me.baron.weatherstyle.model.db.dao.component.DaggerWeatherDaoComponent;
import me.baron.weatherstyle.model.db.models.adapter.MiWeatherAdapter;
import me.baron.weatherstyle.model.db.models.adapter.WeatherAdapter;
import me.baron.weatherstyle.model.http.ApiClient;
import me.baron.weatherstyle.model.preferences.Preferences;
import me.baron.weatherstyle.model.preferences.WeatherSettings;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * @author baronzhang (baron[dot]zhanglei[at]gmail[dot]com)
 */
public final class HomePagePresenter implements HomePageContract.Presenter {

    private final Context context;
    private final HomePageContract.View weatherView;

    @Inject
    WeatherDao weatherDao;

    @Inject
    HomePagePresenter(Context context, HomePageContract.View view) {

        this.context = context;
        this.weatherView = view;
        weatherView.setPresenter(this);

        DaggerWeatherDaoComponent.builder()
                .applicationComponent(WeatherApp.getInstance().getApplicationComponent())
                .build().inject(this);
    }

    @Override
    public void start() {
        String cityId = Preferences.getSharedPreferences().getString(WeatherSettings.SETTINGS_CURRENT_CITY_ID.getId(), "");
        loadWeather(cityId);
    }

    @Override
    public void loadWeather(String cityId) {

        if (NetworkUtil.isNetworkConnected(context)) ApiClient.weatherService.getMiWeather(cityId)
                .map(miWeather -> {
                    WeatherAdapter weather = new MiWeatherAdapter(miWeather);
                    try {
                        weatherDao.insertWeather(weather.getWeather());
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    return weather;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(weatherView::displayWeatherInformation);
    }
}