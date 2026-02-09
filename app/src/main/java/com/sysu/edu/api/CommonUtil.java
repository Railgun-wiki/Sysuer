package com.sysu.edu.api;

import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;

public class CommonUtil {
    /**
    * 从JSONObject中提取指定键的值
    * @param data JSONObject数据
    * @param keys 要提取的键数组
    * @return 包含提取值的ArrayList
    * */
    public static ArrayList<String> extractValue(JSONObject data, String[] keys) {
        ArrayList<String> values = new ArrayList<>();
        for (String i : keys) {
            values.add(data.getString(i));
        }
        return values;
    }

}
