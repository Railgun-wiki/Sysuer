package com.sysu.edu.api;

import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;

public class CommonUtil {
    /**
     * 从JSONObject中提取指定键的值
     *
     * @param data JSONObject数据
     * @param keys 要提取的键数组
     * @return 包含提取值的ArrayList
     *
     */
    public static ArrayList<String> extractValue(JSONObject data, String[] keys) {
        ArrayList<String> values = new ArrayList<>();
        for (String i : keys) {
            values.add(data.getString(i));
        }
        return values;
    }

    /**
    * 将boolean值转换为字符串"1"或"0"
    * @param b 要转换的boolean值
    * @return 转换后的字符串"1"或"0"
    * */
    public static String bool2str(boolean b) {
        return b ? "1" : "0";
    }

    /**
    * 检查字符串是否为空或仅包含空格
    * @param str 要检查的字符串
    * @return 如果字符串为空或仅包含空格，则返回true；否则返回false
    * */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
    * 对字符串进行trim操作，若字符串为空则返回空字符串
    * @param str 要进行修剪操作的字符串
    * @return trim后的字符串，若原字符串为空则返回空字符串
    * */
    public static String trim(String str) {
        return str == null ? "" : str.trim();
    }

}
