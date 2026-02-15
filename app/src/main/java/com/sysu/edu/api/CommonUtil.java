package com.sysu.edu.api;

import android.content.Context;

import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 通用工具类
 */
public class CommonUtil {

    /**
     * 从 JSONObject 中提取指定键的值
     *
     * @param data JSONObject 数据
     * @param keys 要提取的键数组
     * @return 包含提取值的 ArrayList
     *
     */
    public static ArrayList<String> extractValue(JSONObject data, String[] keys) {
        ArrayList<String> values = new ArrayList<>();
        for (String i : keys) values.add(data.getString(i));
        return values;
    }

    /**
     * 从 JSONObject 中提取指定键的值
     *
     * @param data JSONObject 数据
     * @param keys 要提取的键列表
     * @return 包含提取值的 ArrayList
     */
    public static ArrayList<String> extractValue(JSONObject data, List<String> keys) {
        ArrayList<String> values = new ArrayList<>();
        for (String i : keys) values.add(data.getString(i));
        return values;
    }

    /**
     * 将boolean值转换为字符串"1"或"0"
     *
     * @param b 要转换的 boolean 值
     * @return 转换后的字符串"1"或"0"
     *
     */
    public static String bool2str(boolean b) {
        return b ? "1" : "0";
    }

    /**
     * 检查字符串是否为空或仅包含空格
     *
     * @param str 要检查的字符串
     * @return 如果字符串为空或仅包含空格，则返回true；否则返回false
     *
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 对字符串进行trim操作，若字符串为空则返回空字符串
     *
     * @param str 要进行修剪操作的字符串
     * @return trim后的字符串，若原字符串为空则返回空字符串
     *
     */
    public static String trim(String str) {
        return str == null ? "" : str.trim();
    }

    /**
     * 从资源 ID 列表中获取对应的字符串数组
     *
     * @param context  上下文对象
     * @param resource 资源 ID 数组
     * @return 包含对应字符串的数组
     *
     */
    public static String[] getString(Context context, int[] resource) {
        return Arrays.stream(resource).mapToObj(context::getString).toArray(String[]::new);
    }

    /**
     * 从资源 ID 列表中获取对应的字符串数组
     *
     * @param context  上下文对象
     * @param resource 资源 ID 列表
     * @return 包含对应字符串的数组
     *
     */
    public static String[] getString(Context context, List<Integer> resource) {
        return resource.stream().mapToInt(Integer::intValue).mapToObj(context::getString).toArray(String[]::new);
    }
}
