package com.sysu.edu.api;

import android.content.Context;
import android.util.TypedValue;

public class ContextUtil {
    private final Context context;

    public ContextUtil(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public int getColorFromAttr(int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

}
