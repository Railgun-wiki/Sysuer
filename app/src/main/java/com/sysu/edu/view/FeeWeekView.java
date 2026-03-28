package com.sysu.edu.view;

import static com.sysu.edu.api.CommonUtil.trim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;

import com.haibin.calendarview.Calendar;
import com.haibin.calendarview.WeekView;
import com.sysu.edu.api.ContextUtil;

/**
 * 演示一个变态需求的周视图
 */

public class FeeWeekView extends WeekView {


    /**
     * 自定义魅族标记的文本画笔
     */
    private final Paint mTextPaint = new Paint();
    /**
     * 24节气画笔
     */
    private final Paint mSolarTermTextPaint = new Paint();
    /**
     * 背景圆点
     */
    private final Paint mPointPaint = new Paint();
    /**
     * 今天的背景色
     */
    private final Paint mCurrentDayPaint = new Paint();
    /**
     * 圆点半径
     */
    private final float mPointRadius;
    private final int mPadding;
    private final float mCircleRadius;
    /**
     * 自定义魅族标记的圆形背景
     */
    private final Paint mSchemeBasicPaint = new Paint();
    private final float mSchemeBaseLine;
    private int other = Color.GRAY;
    private int mRadius;

    public FeeWeekView(Context context) {
        super(context);

        ContextUtil contextUtil = new ContextUtil(context);
        other = contextUtil.getColorFromAttr(com.google.android.material.R.attr.colorTertiary);
        mTextPaint.setTextSize(dipToPx(context, 8));
        mTextPaint.setColor(0xffffffff);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setFakeBoldText(true);


        mSolarTermTextPaint.setColor(0xff489dff);
        mSolarTermTextPaint.setAntiAlias(true);
        mSolarTermTextPaint.setTextAlign(Paint.Align.CENTER);

        mSchemeBasicPaint.setAntiAlias(true);
        mSchemeBasicPaint.setStyle(Paint.Style.FILL);
        mSchemeBasicPaint.setTextAlign(Paint.Align.CENTER);
        mSchemeBasicPaint.setFakeBoldText(true);
        mSchemeBasicPaint.setColor(Color.WHITE);

        mPointPaint.setAntiAlias(true);
        mPointPaint.setStyle(Paint.Style.FILL);
        mPointPaint.setTextAlign(Paint.Align.CENTER);
        mPointPaint.setColor(Color.RED);


        mCurrentDayPaint.setAntiAlias(true);
        mCurrentDayPaint.setStyle(Paint.Style.FILL);
        mCurrentDayPaint.setColor(0xFFeaeaea);


        mCircleRadius = dipToPx(getContext(), 7);

        mPadding = dipToPx(getContext(), 3);

        mPointRadius = dipToPx(context, 2);

        Paint.FontMetrics metrics = mSchemeBasicPaint.getFontMetrics();
        mSchemeBaseLine = mCircleRadius - metrics.descent + (metrics.bottom - metrics.top) / 2 + dipToPx(getContext(), 1);

    }

    /**
     * dp转px
     *
     * @param context context
     * @param dpValue dp
     * @return px
     */
    private static int dipToPx(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    @Override
    protected void onPreviewHook() {
        mSolarTermTextPaint.setTextSize(mCurMonthLunarTextPaint.getTextSize());
        mRadius = Math.min(mItemWidth, mItemHeight) / 2;
    }

    @Override
    protected boolean onDrawSelected(Canvas canvas, Calendar calendar, int x, boolean hasScheme) {
        int cx = x + mItemWidth / 2;
        int cy = mItemHeight / 2;
        canvas.drawCircle(cx, cy, mRadius, mSelectedPaint);
        return true;
    }

    @Override
    protected void onDrawScheme(Canvas canvas, Calendar calendar, int x) {

        boolean isSelected = isSelected(calendar);
        if (isSelected) {
            mPointPaint.setColor(Color.WHITE);
        } else {
            mPointPaint.setColor(Color.GRAY);
        }

//        canvas.drawCircle(x + (float) mItemWidth / 2, mItemHeight - 3 * mPadding, mPointRadius, mPointPaint);
    }

    @Override
    protected void onDrawText(Canvas canvas, Calendar calendar, int x, boolean hasScheme, boolean isSelected) {
        int cx = x + mItemWidth / 2;
        int cy = mItemHeight / 2;
        int top = -mItemHeight / 6;

        if (calendar.isCurrentDay() && !isSelected) {
            canvas.drawCircle(cx, cy, mRadius, mCurrentDayPaint);
        }

        String scheme = trim(calendar.getScheme());
        if (hasScheme) {
//            canvas.drawCircle(x + mItemWidth - mPadding - mCircleRadius / 2, mPadding + mCircleRadius, mCircleRadius, mSchemeBasicPaint);

            mTextPaint.setColor(calendar.getSchemeColor());

            canvas.drawText(scheme, x + mItemWidth - mPadding - mCircleRadius, mPadding + mSchemeBaseLine, mTextPaint);
        }

//        if (calendar.isWeekend() && calendar.isCurrentMonth()) {
//            mCurMonthTextPaint.setColor(0xFF489dff);
//            mCurMonthLunarTextPaint.setColor(0xFF489dff);
//            mSchemeTextPaint.setColor(0xFF489dff);
//            mSchemeLunarTextPaint.setColor(0xFF489dff);
//            mOtherMonthLunarTextPaint.setColor(0xFF489dff);
//            mOtherMonthTextPaint.setColor(0xFF489dff);
//        } else {
            mCurMonthTextPaint.setColor(other);
            mCurMonthLunarTextPaint.setColor(other);
            mSchemeTextPaint.setColor(other);
            mSchemeLunarTextPaint.setColor(other);

            mOtherMonthTextPaint.setColor(0xFFEEEEEE);
            mOtherMonthLunarTextPaint.setColor(0xFFEEEEEE);
//        }

        if (isSelected) {
            canvas.drawText(String.valueOf(calendar.getDay()), cx, mTextBaseLine + top,
                    mSelectTextPaint);
            canvas.drawText(scheme, cx, mTextBaseLine + (float) mItemHeight / 10, mSelectedLunarTextPaint);
        } else if (hasScheme) {

            canvas.drawText(String.valueOf(calendar.getDay()), cx, mTextBaseLine + top,
                    calendar.isCurrentMonth() ? mSchemeTextPaint : mOtherMonthTextPaint);

            canvas.drawText(scheme, cx, mTextBaseLine + (float) mItemHeight / 10,
                    !TextUtils.isEmpty(calendar.getSolarTerm()) ? mSolarTermTextPaint : mSchemeLunarTextPaint);
        } else {
            canvas.drawText(String.valueOf(calendar.getDay()), cx, mTextBaseLine + top,
                    calendar.isCurrentDay() ? mCurDayTextPaint :
                            calendar.isCurrentMonth() ? mCurMonthTextPaint : mOtherMonthTextPaint);

            canvas.drawText(scheme, cx, mTextBaseLine + (float) mItemHeight / 10,
                    calendar.isCurrentDay() ? mCurDayLunarTextPaint :
                            !TextUtils.isEmpty(calendar.getSolarTerm()) ? mSolarTermTextPaint :
                                    calendar.isCurrentMonth() ?
                                            mCurMonthLunarTextPaint : mOtherMonthLunarTextPaint);
        }
    }
}
