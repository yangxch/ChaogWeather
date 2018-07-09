package com.example.chaogweather;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chaogweather.db.City;
import com.example.chaogweather.db.County;
import com.example.chaogweather.db.Province;
import com.example.chaogweather.util.HttpUtil;
import com.example.chaogweather.util.Utility;

import org.litepal.crud.ClusterQuery;
import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by xch on 2017/2/21.
 */

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    /**
     * 省列表
     */
    private List<Province> provinceList;

    /**
     * 市列表
     */
    private List<City> cityList;
    /**
     * 县列表
     */
    private List<County> countyList;
    /**
     * 选中的省份
     */
    private Province selectedProvince;
    /**
     * 选中的城市
     */
    private City selectedCity;
    /**
     * 当前选中的级别
     */
    private int currentLevel;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {//条目点击事件
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    //查询市
                    queryCity();
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    //查询县
                    queryCounty();
                }else if(currentLevel==LEVEL_COUNTY){
                    String weatherId=countyList.get(position).getWeatherId();
                    if(getActivity()instanceof MainActivity){
                        Intent intent=new Intent(getActivity(),WeatherActivity.class);
                        intent.putExtra("weather_id",weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }else if(getActivity()instanceof WeatherActivity){//如果该碎片在WeatherActivity中
                        WeatherActivity activity= (WeatherActivity) getActivity();
                        activity.drawerLayout.closeDrawers();//关闭滑动菜单
                        activity.swipeRefresh.setRefreshing(true);//显示下拉刷新进度条
                        activity.requestWeather(weatherId);//请求新城市天气信息
                    }

                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {//返回按钮点击事件
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_CITY) {
                    //查询省
                    queryProvinces();
                } else if (currentLevel == LEVEL_COUNTY) {
                    //查询市
                    queryCity();
                }
            }
        });
        //查询省
        queryProvinces();
    }

    /**
     * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询。
     */
    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);//将返回按钮隐藏
        provinceList = DataSupport.findAll(Province.class);//从数据库中查询
        if (provinceList.size()>0) {
            dataList.clear();
            for (Province province : provinceList) {
                String provinceName = province.getProvinceName();
                dataList.add(provinceName);//将省名加入dataList中
            }
            adapter.notifyDataSetChanged();//通知数据改变，重新适配
            listView.setSelection(0);//定位到第一行
            currentLevel = LEVEL_PROVINCE;
        } else {
            //从服务器查询
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
        }
    }

    /**
     * 查询全国所有的市，优先从数据库查询，如果没有查询到再去服务器上查询。
     */
    private void queryCity() {
        currentLevel = LEVEL_CITY;
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        ClusterQuery clusterQuery = DataSupport.where("provinceid = ?", String.valueOf(selectedProvince.getId()));
        cityList = clusterQuery.find(City.class);
        if (cityList.size()>0) {
            dataList.clear();
            for (City city : cityList) {
                String cityName = city.getCityName();
                dataList.add(cityName);
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
        } else {
            String address = "http://guolin.tech/api/china/" + selectedProvince.getProvinceCode();
            queryFromServer(address, "city");
        }
    }

    /**
     * 查询全国所有的县，优先从数据库查询，如果没有查询到再去服务器上查询。
     */
    private void queryCounty() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address, "county");
        }
    }

    /**
     * 根据传入的地址和类型从服务器上查询省市县数据。
     */

    private void queryFromServer(String address, final String type) {
        showProgressDialog();
        HttpUtil.sendOkhttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "获取服务器数据异常", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responsText = response.body().string();//服务器返回的数据
                boolean result = false;//result：数据是否解析处理成功
                if (type.equals("province")) {
                    result = Utility.handleProvinceResponse(responsText);
                } else if (type.equals("city")) {
                    result = Utility.handleCityResponse(responsText, selectedProvince.getId());
                } else if (type.equals("county")) {
                    result = Utility.handleCountyResponse(responsText, selectedCity.getId());
                }
                if (result) {//如果解析处理完数据，再次调用各自方法加载数据，涉及UI处理，应切换到主线程
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if (type.equals("province")) {
                                queryProvinces();
                            } else if (type.equals("city")) {
                                queryCity();
                            } else if (type.equals("county")) {
                                queryCounty();
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 显示进度框
     */
    public void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
    }

    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

}
