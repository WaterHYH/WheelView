package com.water.wheelview;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by hui on 2018/11/22.
 */

public class TextWheelViewAdapter extends WheelView.WheelViewAdapter<String> {

    private float textSize = 20f;
    private int textColorNormal = Color.LTGRAY;
    private int textColorSelected = Color.GREEN;
    private int verticalMargin = 20;
    private OnMiddleSelectedListener middleSelectedListener;

    public OnMiddleSelectedListener getMiddleSelectedListener() {
        return middleSelectedListener;
    }

    public void setMiddleSelectedListener(OnMiddleSelectedListener middleSelectedListener) {
        this.middleSelectedListener = middleSelectedListener;
    }

    public interface OnMiddleSelectedListener{
        void onMiddleItemSelected(String text);
    }

    public TextWheelViewAdapter(Context context,ArrayList<String> datas) {
        super(context,datas);
    }


    @Override
    protected View getView(int position, View convertView, String data, boolean middle) {
        ViewHolder viewHolder = getViewHolder(context,convertView);
        TextView textView = viewHolder.textView;
        textView.setText(data);
        if (middle) {
            textView.setTextColor(textColorSelected);
            textView.setTextSize(textSize*1.5f);
            if (middleSelectedListener != null) {
                middleSelectedListener.onMiddleItemSelected(data);
            }
        }else {
            textView.setTextColor(textColorNormal);
            textView.setTextSize(textSize);
        }
        return viewHolder.rootView;
    }

    public ViewHolder getViewHolder(Context context, View convertView) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = new LinearLayout(context);
            convertView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            TextView textView = new TextView(context);
            textView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            layoutParams.topMargin = verticalMargin/2;
            layoutParams.bottomMargin = verticalMargin/2;
            textView.setLayoutParams(layoutParams);
            ((LinearLayout)convertView).addView(textView);

            viewHolder = new ViewHolder(convertView,textView);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        return viewHolder;
    }

    public static class ViewHolder{
        private View rootView;
        private TextView textView;

        public ViewHolder(View rootView,TextView textView) {
            this.rootView = rootView;
            rootView.setTag(this);
            this.textView = textView;
        }
    }

    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    public int getTextColorNormal() {
        return textColorNormal;
    }

    public void setTextColorNormal(int textColorNormal) {
        this.textColorNormal = textColorNormal;
    }

    public int getTextColorSelected() {
        return textColorSelected;
    }

    public void setTextColorSelected(int textColorSelected) {
        this.textColorSelected = textColorSelected;
    }
}
