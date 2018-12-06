package com.water.wheelview;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by hui on 2018/11/20.
 */

public class WheelView extends ListView {
    private static final String TAG = "WheelView";
    private static final Integer TASK_DOWN = 0;
    private static final Integer TASK_UP = 1;
    private WheelViewAdapter adapter;
    private int middleIndex;
    private View middleView;
    private int middleViewHeight = -1;
    private int toSelectIndex = -1;
    private boolean hasNoAdjust = true;
    private UIHandler uiHandler;
    private Map<Integer,Runnable> notifyRuns = new HashMap<Integer,Runnable>();
    private int tryCount;
    private int middleHeight;
    private boolean scrolling;
    private boolean isPressDown;
    private int scrollState = OnScrollListener.SCROLL_STATE_IDLE;

    public WheelView(Context context) {
        super(context);
        initData();
    }

    public WheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initData();
    }

    public WheelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initData();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            isPressDown = false;
            adjustMiddleItem();
        }else if (ev.getAction() == MotionEvent.ACTION_DOWN){
            isPressDown = true;
        }
        return super.onTouchEvent(ev);
    }

    private void initData() {
        Log.i(TAG,"initData");

        //隐藏滚动条
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
        setFastScrollEnabled(false);
        //去掉分割线
        setDividerHeight(0);
        //取消item的点击效果
        setSelector(new ColorDrawable(Color.TRANSPARENT));

        //监听滚动事件
        initScrollListener();

        //监听点击事件
        initClickListener();

        uiHandler = new UIHandler(getContext().getMainLooper(),this);
    }

    @Override
    public final void setAdapter(ListAdapter adapter) {
        if (adapter instanceof WheelViewAdapter) {
            ((WheelViewAdapter) adapter).setWheelView(this);
            this.adapter = (WheelViewAdapter) adapter;
            super.setAdapter(adapter);
        }
    }

    @Override
    public ListAdapter getAdapter() {
        return super.getAdapter();
    }

    private void initClickListener() {
        setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //跳转到指定position并显示在list中间
                scrollToMiddleItem(position);
            }
        });
    }

    public void setMiddleHeight(int middleHeight) {
        this.middleHeight = middleHeight;
    }

    public int getMiddleHeight() {
        return middleHeight;
    }

    public static class UIHandler extends Handler{

        public static final int MSG_ADJUST_ITEM = 101;
        public static final int MSG_NOTIFY = 102;
        public static final int MSG_CHECK_SCROLL = 103;
        private WheelView wheelView;

        public UIHandler(Looper looper, WheelView wheelView) {
            super(looper);
            this.wheelView = wheelView;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ADJUST_ITEM:
                    removeMessages(msg.what);

                    if (wheelView.scrolling) {
                        adjustMiddleItem(100);
                        break;
                    }

                    int middleIndex = wheelView.getMiddleIndex();
                    View item = wheelView.getChildAt(middleIndex - wheelView.getFirstVisiblePosition());
                    if (item != null) {
                        wheelView.setMiddleHeight(item.getMeasuredHeight());
                        Log.i(TAG, "adjustMiddleItem middleIndex=" + middleIndex + " middleViewHeight=" + wheelView.getMiddleHeight());
                        //获取listView的高度
                        int listHeight = wheelView.getHeight();
                        //调整
                        wheelView.smoothScrollToPositionFromTop(middleIndex, listHeight / 2 - wheelView.getMiddleHeight() / 2);
                    }

                    break;
                case MSG_NOTIFY:
                    if (wheelView.getAdapter() != null) {
                        ((WheelViewAdapter)wheelView.getAdapter()).superNotifyDataSetChanged();
                    }
                    break;
                case MSG_CHECK_SCROLL:
                    removeMessages(msg.what);
                    if (wheelView.scrolling && !wheelView.isPressDown && wheelView.scrollState==OnScrollListener.SCROLL_STATE_IDLE) {
                        wheelView.scrolling = false;
                    }
                    break;
                default:
                    break;
            }
        }

        public void adjustMiddleItem(long delay){
            removeMessages(MSG_ADJUST_ITEM);
            sendEmptyMessageDelayed(MSG_ADJUST_ITEM,delay);
        }

        public void notifyDataSetChanged() {
            removeMessages(MSG_NOTIFY);
            sendEmptyMessageDelayed(MSG_NOTIFY,100);
        }

        public void checkScrolling() {
            removeMessages(MSG_CHECK_SCROLL);
            sendMessageDelayed(obtainMessage(MSG_CHECK_SCROLL,System.currentTimeMillis()),100);
        }
    }

    @Override
    protected void handleDataChanged() {
        super.handleDataChanged();

        for (Integer key : notifyRuns.keySet()) {
            Runnable runnable = notifyRuns.get(key);
            runnable.run();
        }
        notifyRuns.clear();
    }

    /**
     * 调整选中item到listView的中间
     */
    private void adjustMiddleItem() {
        if (adapter == null || middleIndex == 0) {
            return;
        }
        uiHandler.adjustMiddleItem(100);
        hasNoAdjust = false;
    }

    private int getMiddleIndex() {
        return middleIndex;
    }

    public void setMiddleSelection(int position){
        if (adapter == null || position >= adapter.getSourceCount()) {
            return;
        }
        if (hasNoAdjust || scrolling) {
            //未初始化完成
            final int finalPosition = position;
            uiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setMiddleSelection(finalPosition);
                }
            },((1 << tryCount))*100);
            tryCount = tryCount + 1;
            Log.i(TAG,"setMiddleSelection delay. position="+position);
            return;
        }
        Log.i(TAG,"setMiddleSelection position="+position);
        //先将position转换为整个list的position
        position = getFirstVisiblePosition()/adapter.getSourceCount()*adapter.getSourceCount() + position;
        if (position == middleIndex) {
            return;
        }
        scrollToMiddleItem(position);
    }

    private void scrollToMiddleItem(int position){
        //获取listView的高度
        int listHeight = getHeight();
        if (position < adapter.getSourceCount()){
            position = position + adapter.getSourceCount();
        }
        if (position > adapter.getSourceCount()*2) {
            position = position - adapter.getSourceCount();
        }

        View middleItem = getChildAt(position-getFirstVisiblePosition());
        int offset = getMiddleHeight()/2;
        if (middleItem != null && middleItem.getTop() > listHeight/2) {
            offset = 0;
        }
        Log.i(TAG,"scrollToMiddleItem position="+position+" offset="+offset+" listHeight="+listHeight);

        setSelectionFromTop(position,listHeight/2 - offset);

        adjustMiddleItem();
    }

    public boolean isScrolling() {
        return scrolling;
    }

    /**
     * 监听ListView的滚动事件.实现无限滚动效果
     */
    private void initScrollListener() {
        setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                Log.i(TAG,"onScrollStateChanged scrollState="+scrollState);
                WheelView.this.scrollState = scrollState;
                if (OnScrollListener.SCROLL_STATE_IDLE == scrollState) {
                    scrolling = false;
                    adjustMiddleItem();
                }else {
                    scrolling = true;
                }
            }

            /**
             * listview滚动时回调
             * @param view
             * @param firstVisibleItem 界面上显示的第一个item的下标
             * @param visibleItemCount 界面上共显示多少个item
             * @param totalItemCount listview共有多少个item
             */
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                //Log.i(TAG,"onScroll firstVisibleItem="+firstVisibleItem+" visibleItemCount="+visibleItemCount+" totalItemCount="+totalItemCount);
                if (visibleItemCount == 0 || totalItemCount == 0 ) {
                    return;
                }
                scrolling = true;
                uiHandler.checkScrolling();
                //实现无限滚动的核心代码
                if (firstVisibleItem == 0) {
                    View topItem = getChildAt(0);
                    View bottomItem = getChildAt(visibleItemCount-1);
                    //界面中的第一个item是列表中的第一个item，并且界面显示小于两节item，则向下滚一节
                    if (topItem.getTop() == 0 && visibleItemCount < totalItemCount/3*2) {
                        //处于头部，不能再往上滚动了，向下移一节
                        //最顶部的item刚要显示出来的时候就要下移一节了，所以这里必须+1，否则上会出现向上滚动不连贯的现象
                        adapter.notifyDataSetChanged();
                        //notify列表成功会回调handleDataChanged方法，在回调方法里面setSelection，否则会出现setSelection无效的问题
                        notifyRuns.put(TASK_DOWN,new Runnable() {
                            @Override
                            public void run() {
                                Log.i(TAG,"onScroll down");
                                setSelection(adapter.getSourceCount());
                            }
                        });
                    }else {
                        changeMiddleIndex(firstVisibleItem, visibleItemCount);
                    }

                }else if (firstVisibleItem + visibleItemCount == totalItemCount){
                    final View topItem = getChildAt(0);
                    View bottomItem = getChildAt(visibleItemCount-1);
                    if (bottomItem.getBottom() == getHeight() && visibleItemCount < totalItemCount/3*2) {
                        //滚动到了底部，无法再往下滚动了，向上移一节
                        adapter.notifyDataSetChanged();
                        //notify列表成功会回调handleDataChanged方法，在回调方法里面setSelection，否则会出现setSelection无效的问题
                        notifyRuns.put(TASK_UP,new Runnable() {
                            @Override
                            public void run() {
                                Log.i(TAG,"onScroll up");
                                setSelectionFromTop(getFirstVisiblePosition() - adapter.getSourceCount(),topItem.getTop());
                            }
                        });
                    }else {
                        changeMiddleIndex(firstVisibleItem, visibleItemCount);
                    }
                }else {
                    changeMiddleIndex(firstVisibleItem, visibleItemCount);
                }

            }
        });
    }

    /**
     * 更新中间item的位置
     * @param firstVisibleItem 界面中显示的第一个item
     * @param visibleItemCount 界面中共显示多少个item
     */
    private void changeMiddleIndex(int firstVisibleItem, int visibleItemCount) {

        int middle = firstVisibleItem + visibleItemCount/2;
        int index = middle-firstVisibleItem;
        for (int i = index-1; i <= index+1; i++) {
            if (i < 0) {
                continue;
            }
            View item = getChildAt(i);
            int middleY = getHeight()/2;
            //必须判断item是否为null,因为当前界面显示少于3个item时，会出现item为null的情况
            if (item != null && item.getTop() > middleY-item.getHeight() && item.getBottom() < middleY+item.getHeight()){
                //在中间显示范围内的item是中间item
                middle = i+firstVisibleItem;
                break;
            }
        }
        if (middle != middleIndex) {
            int oldIndex = middleIndex;
            middleIndex = middle;
            Log.i(TAG,"middleIndex change. old="+oldIndex+" new="+middleIndex);

            //刷新list,触发getView方法来刷新中间item
            adapter.notifyDataSetChanged();

            //第二种方法
            /*for (int i = firstVisibleItem; i < firstVisibleItem+visibleItemCount; i++) {
                View item = getChildAt(i-firstVisibleItem);
                item = adapter.getView(i,item, WheelViewT2.this);
                item.invalidate();
            }*/
        }
    }

    public static abstract class WheelViewAdapter<T> extends BaseAdapter{

        protected Context context;
        protected WheelView wheelView;
        protected ArrayList<T> datas = new ArrayList<T>();

        protected abstract View getView(int position,View convertView,T data,boolean middle);

        public WheelViewAdapter(Context context,ArrayList<T> datas) {
            this.context = context;
            this.datas = datas;
            if (this.datas == null) {
                this.datas = new ArrayList<T>();
            }
        }

        public void setDatas(ArrayList<T> datas) {
            int index = -1;
            if (datas != null && this.datas != null) {
                index = wheelView.middleIndex + datas.size() - this.datas.size();
            }
            this.datas = datas;
            notifyDataSetChanged();
            if (index != -1) {
                wheelView.scrollToMiddleItem(index);
            }
        }

        @Override
        public void notifyDataSetChanged() {
            if (wheelView == null) {
                return;
            }
            super.notifyDataSetChanged();
            // 不能直接调用notifyDataSetChanged，必须post一个runnable。
            // 否则通过点击item来滚动时，调用了notifyDataSetChanged也不会触发getView方法。
            wheelView.uiHandler.notifyDataSetChanged();
            /*new Handler(context.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    WheelViewAdapter.super.notifyDataSetChanged();
                }
            });*/
        }

        @Override
        public int getCount() {
            return datas.size()*3;
        }

        public int getSourceCount(){
            return datas.size();
        }

        @Override
        public Object getItem(int position) {
            if (position >= datas.size()) {
                return null;
            }
            return datas.get(position);
        }

        public Object getMiddleItem(){
            if (wheelView == null || datas.size() == 0) {
                return null;
            }
            return datas.get(wheelView.getMiddleIndex() % datas.size());
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
//            Log.i(TAG,"getView position="+position);
            if (wheelView == null) {
                return null;
            }
            View view = getView(position%datas.size(),convertView,datas.get(position%datas.size()),position==wheelView.getMiddleIndex());
            wheelView.updateMiddleView(position,view);

            return view;
        }

        public void setWheelView(WheelView wheelView) {
            this.wheelView = wheelView;
        }

        public boolean remove(int index){
            if (index < 0 || index >= datas.size()){
                return false;
            }
            datas.remove(index);
            notifyDataSetChanged();
            return true;
        }

        public boolean add(int index,T data){
            if (index < 0 || index > datas.size() || data == null){
                return false;
            }
            datas.add(index,data);
            notifyDataSetChanged();
            return true;
        }

        public void superNotifyDataSetChanged() {
            super.notifyDataSetChanged();
        }
    }

    private void updateMiddleView(int position,View view) {
        if (position == middleIndex) {
            if (hasNoAdjust) {
                adjustMiddleItem();
            }
        }
    }
}
