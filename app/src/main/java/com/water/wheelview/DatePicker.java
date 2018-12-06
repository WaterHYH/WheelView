package com.water.wheelview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by hui on 2018/11/22.
 */

public class DatePicker  extends LinearLayout{

    public static final String TAG = "DatePicker";
    public static final int TYPE_DATE = 1;
    public static final int TYPE_TIME = 2;

    private WheelView wheelViewYear;
    private WheelView wheelViewMonth;
    private WheelView wheelViewDay;
    private WheelView wheelViewHour;
    private WheelView wheelViewMinute;
    private View dividerDate;
    private TextView dividerTime;

    private TextWheelViewAdapter adapterYear;
    private TextWheelViewAdapter adapterMonth;
    private TextWheelViewAdapter adapterDay;
    private TextWheelViewAdapter adapterHour;
    private TextWheelViewAdapter adapterMinute;

    private float textSize = 20f;
    private int textColorNormal = Color.LTGRAY;
    private int textColorSelected = Color.GREEN;
    private int pickType;

    private int year,month,day,hour,minute;
    private OnDateChangedListener onDateChangedListener;

    private MyHandler myHandler;

    public OnDateChangedListener getOnDateChangedListener() {
        return onDateChangedListener;
    }

    public void setOnDateChangedListener(OnDateChangedListener onDateChangedListener) {
        this.onDateChangedListener = onDateChangedListener;
    }

    public interface OnDateChangedListener {
        void onChanged(DatePicker sender, int year, int month, int day, int hour, int minute);
    }

    public DatePicker(Context context) {
        super(context);
        initData(context);
    }

    public DatePicker(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        TypedArray typedArray = context.obtainStyledAttributes(attrs,R.styleable.DatePicker);
        textColorNormal = typedArray.getColor(R.styleable.DatePicker_pickerNormalColor,Color.LTGRAY);
        textColorSelected = typedArray.getColor(R.styleable.DatePicker_pickerSelectedColor,Color.GREEN);
        textSize = typedArray.getDimension(R.styleable.DatePicker_pickerTextSize,20f);
        pickType = typedArray.getInt(R.styleable.DatePicker_pickerType,1|2);

        Log.i(TAG,"init textColorNormal="+textColorNormal+" textColorSelected="+textColorSelected+" textSize="+textSize+" pickType="+((pickType&TYPE_DATE) == TYPE_DATE));
        initData(context);

    }

    public DatePicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initData(context);
    }

    private void initData(Context context) {

        myHandler = new MyHandler(this);

        //固定水平布局
        setHorizontalGravity(HORIZONTAL);

        Calendar calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);
        hour = calendar.get(Calendar.HOUR_OF_DAY);
        minute = calendar.get(Calendar.MINUTE);

        if ((pickType&TYPE_DATE) == TYPE_DATE){
            initDateView(context);
        }

        if ((pickType&(TYPE_DATE|TYPE_TIME)) == (TYPE_DATE|TYPE_TIME)) {
            dividerDate = createDividerDate(context);
            addView(dividerDate);
        }

        if ((pickType&TYPE_TIME) == TYPE_TIME){
            initTimeView(context);
        }

    }

    private void initTimeView(Context context) {
        wheelViewHour = createWheelView(context, 4, 0, 24,hour, new TextWheelViewAdapter.OnMiddleSelectedListener() {
            @Override
            public void onMiddleItemSelected(String text) {
                int h = Integer.valueOf(text);
                if (h != hour) {
                    hour = h;
                    onChange();
                }
            }
        });
        addView(wheelViewHour);

        dividerTime = createDivider(context,":");
        addView(dividerTime);

        wheelViewMinute = createWheelView(context, 4, 0, 60,minute, new TextWheelViewAdapter.OnMiddleSelectedListener() {
            @Override
            public void onMiddleItemSelected(String text) {
                int m = Integer.valueOf(text);
                if (m != minute) {
                    minute = m;
                    onChange();
                }
            }
        });
        addView(wheelViewMinute);
    }

    private void onChange() {
        if (onDateChangedListener != null) {
            onDateChangedListener.onChanged(DatePicker.this,year,month,day,hour,minute);
        }
    }

    private boolean isLeapYear(int year){
        if ((year%4 == 0 && year%100!=0) || year%400 == 0){
            return true;
        }else {
            return false;
        }
    }

    private int getMaxDay(){
        Log.i(TAG,"getMaxDay year="+year+" month="+month);
        if (month == 1){
            if (isLeapYear(year)) {
                //闰年
                return 29;
            }else {
                //平年
                return 28;
            }
        }else if (month == 0 || month == 2 || month == 4 || month == 6 || month == 7 || month == 9 || month == 11){
            //大月
            return 31;
        }else {
            //小月
            return 30;
        }
    }

    private ArrayList<String> getDayDatas(){
        ArrayList<String> datas = new ArrayList<String>();
        int maxDay = getMaxDay();
        for (int i = 0; i < maxDay; i++) {
            datas.add(String.valueOf(i+1));
        }
        return datas;
    }

    private void adjustDay(){
        TextWheelViewAdapter dayAdapter = (TextWheelViewAdapter)wheelViewDay.getAdapter();
        ArrayList<String> datas = getDayDatas();
        dayAdapter.setDatas(datas);
    }

    private static class MyHandler extends Handler{

        public static final int MSG_ADJUST_DAY = 101;

        private DatePicker datePicker ;
        public MyHandler(DatePicker datePicker){
            this.datePicker = datePicker;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_ADJUST_DAY:
                    if (datePicker != null) {
                        datePicker.adjustDay();
                    }
                    break;
            }
        }

        public void adjustDay(){
            removeMessages(MSG_ADJUST_DAY);
            sendEmptyMessageDelayed(MSG_ADJUST_DAY,200);
        }
    }

    private void initDateView(Context context) {

        wheelViewDay = createWheelView(context, 4, 1, getMaxDay()+1,day-1, new TextWheelViewAdapter.OnMiddleSelectedListener() {
            @Override
            public void onMiddleItemSelected(String text) {
                int d = Integer.valueOf(text);
                if (d != day) {
                    day = d;
                    onChange();
                }
            }
        });

        wheelViewMonth = createWheelView(context, 4, 1, 13,month, new TextWheelViewAdapter.OnMiddleSelectedListener() {
            @Override
            public void onMiddleItemSelected(String text) {
                int m = Integer.valueOf(text)-1;
                if (m != month) {
                    month = m;
                    onChange();
                    myHandler.adjustDay();
                }
            }
        });

        wheelViewYear = createWheelView(context, 6, 1970, year + 50,year-1970, new TextWheelViewAdapter.OnMiddleSelectedListener() {
            @Override
            public void onMiddleItemSelected(String text) {
                int y = Integer.valueOf(text);
                if (y != year) {
                    year = y;
                    onChange();
                    myHandler.adjustDay();
                }
            }
        });
        addView(wheelViewYear);
        addView(wheelViewMonth);
        addView(wheelViewDay);
    }

    private View createDividerDate(Context context) {
        View divider = new View(context);
        divider.setBackgroundColor(textColorSelected);
        LayoutParams layoutParams = new LayoutParams(0, 3);
        layoutParams.weight = 1;
        layoutParams.gravity = Gravity.CENTER_VERTICAL;
        layoutParams.topMargin = 3;
        divider.setLayoutParams(layoutParams);
        return divider;
    }

    private TextView createDivider(Context context, String text) {

        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextColor(textColorSelected);
        textView.setGravity(Gravity.CENTER);
        LayoutParams layoutParams = new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.weight = 1;
        textView.setLayoutParams(layoutParams);

        return textView;
    }

    private WheelView createWheelView(Context context, int weight, int dataBegin, int dataEnd,int selected, TextWheelViewAdapter.OnMiddleSelectedListener listener){
        WheelView wheelView = new WheelView(context);
        LayoutParams layoutParams = new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.weight = weight;
        wheelView.setLayoutParams(layoutParams);
        ArrayList<String> datas = new ArrayList<String>();
        for (int i = dataBegin; i < dataEnd; i++) {
            String s = "";
            if (i == 0) {
                s = "00";
            }else if (i < 10){
                s = "0" + i;
            }else {
                s = String.valueOf(i);
            }
            datas.add(s);
        }
        TextWheelViewAdapter adapter = new TextWheelViewAdapter(context,datas);
        adapter.setTextColorNormal(textColorNormal);
        adapter.setTextColorSelected(textColorSelected);
        adapter.setTextSize(textSize);
        adapter.setMiddleSelectedListener(listener);
        wheelView.setAdapter(adapter);

        wheelView.setMiddleSelection(selected);
        return wheelView;
    }
}
