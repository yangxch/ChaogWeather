package com.example.chaogweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by xch on 2017/2/21.
 */

public class Now {
    @SerializedName("tmp")
    public String tmperature;
    @SerializedName("cond")
    public More more;
    public class More{
        @SerializedName("txt")
        public String info;
    }
}
