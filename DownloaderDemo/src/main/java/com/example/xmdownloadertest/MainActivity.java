package com.example.xmdownloadertest;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.yyn.download.DownloadConstants;
import com.yyn.download.DownloadService;
import com.yyn.download.IDownloadModel;
import com.yyn.download.IDownloadTaskCallback;

import java.util.ArrayList;
import java.util.List;

import io.objectbox.BoxStore;

public class MainActivity extends AppCompatActivity {

    List<MyDownloadModel> mList;
    List<MyDownloadModel> mList2;
    MyAdapter mMyAdapter;
    MyAdapter mMyAdapter2;
    DownloadService mDownloadService;
    private BoxStore mBoxStore;
    private ViewPager mViewPager;
    private List<View> mViewList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mViewPager = findViewById(R.id.viewpager);
        ListView listView = new ListView(this);
        ListView listView2 = new ListView(this);
        mViewList = new ArrayList<>();
        mViewList.add(listView);
        mViewList.add(listView2);

        mViewPager.setAdapter(new MyPagerAdapter());

        mBoxStore = ((MyApplication)getApplication()).getBoxStore();

        mDownloadService = DownloadService.getInstance();

        mList = (List<MyDownloadModel>)(List)mDownloadService.getAllInCompletedDownloadedModels();

        mMyAdapter = new MyAdapter(mList);
        listView.setAdapter(mMyAdapter);

        mList2 = (List<MyDownloadModel>)(List)mDownloadService.getAllCompletedDownloadedModels();

        mMyAdapter2 = new MyAdapter(mList2);
        listView2.setAdapter(mMyAdapter2);

        findViewById(R.id.add_one).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.add_one).setEnabled(false);
                MyDownloadModel downloadModel = new MyDownloadModel();
                downloadModel.setDownloadUrl("http://audio.xmcdn.com/group38/M02/A0/BA/wKgJo1poJaqxzffqAFOjiGwrplg133.m4a");
//                downloadModel.setDownloadUrl("http://s1.xmcdn.com/apk/MainApp_v6.3.99.3_c163_release_proguard_180523_and-a1.apk");
                mDownloadService.addDownloadModel(downloadModel,true);
            }
        });

        findViewById(R.id.delete_all).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDownloadService.deleteAll();
            }
        });

        findViewById(R.id.pause_all).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDownloadService.pauseAllDownloadModels();
            }
        });

        findViewById(R.id.add_ten).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.add_ten).setEnabled(false);
                List<IDownloadModel> list = new ArrayList<>();
                MyDownloadModel downloadModel = new MyDownloadModel();
                downloadModel.setDownloadPriority(0);
                downloadModel.setDownloadUrl("http://audio.xmcdn.com/group41/M0B/01/77/wKgJ8Vri99fzBI1qAFb9kKGrwcQ797.mp3");
                list.add(downloadModel);

                downloadModel = new MyDownloadModel();
                downloadModel.setDownloadPriority(1);
                downloadModel.setDownloadUrl("http://audio.xmcdn.com/group41/M09/01/72/wKgJ8Vri98bAwVBFACt-3yAxpmo306.mp3");
                list.add(downloadModel);

                downloadModel = new MyDownloadModel();
                downloadModel.setDownloadUrl("http://audio.xmcdn.com/group41/M0B/01/73/wKgJ8Vri98yjIpheAFgLSwj75Mo948.m4a");
                list.add(downloadModel);

                downloadModel = new MyDownloadModel();
                downloadModel.setDownloadUrl("http://audio.xmcdn.com/group41/M09/01/72/wKgJ8Vri98jDqtJ7ACGsPRKghoU247.m4a");
                list.add(downloadModel);

                downloadModel = new MyDownloadModel();
                downloadModel.setDownloadUrl("http://aod.tx.xmcdn.com/group42/M06/6D/58/wKgJ81rbYiCQvL6uAE3hUUrkBp8493.mp3");
                list.add(downloadModel);

                downloadModel = new MyDownloadModel();
                downloadModel.setDownloadUrl("http://aod.tx.xmcdn.com/group40/M06/37/3E/wKgJT1rbYhyQW4pdACbwvwB42ZY954.mp3");
                list.add(downloadModel);

                downloadModel = new MyDownloadModel();
                downloadModel.setDownloadUrl("http://aod.tx.xmcdn.com/group40/M06/37/3E/wKgJT1rbYh-AYI6XAE7TgzjGKjs231.m4a");
                list.add(downloadModel);

                downloadModel = new MyDownloadModel();
                downloadModel.setDownloadUrl("http://aod.tx.xmcdn.com/group40/M06/37/F4/wKgJVFrbYhzwi8kSAB4lfzWPFGc105.m4a");
                list.add(downloadModel);

                downloadModel = new MyDownloadModel();
                downloadModel.setDownloadUrl("http://audio.xmcdn.com/group41/M09/53/83/wKgJ8lrZ9uvCEM40AJdZe2wD2CM416.mp3");
                list.add(downloadModel);

                downloadModel = new MyDownloadModel();
                downloadModel.setDownloadUrl("http://audio.xmcdn.com/group40/M09/1A/2E/wKgJT1rZ9vSi_s7RAEus1CyMtfk666.mp3");
                list.add(downloadModel);
                mDownloadService.addDownloadModels(list,true);
            }
        });

        findViewById(R.id.resume_all).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDownloadService.resumeAllDownloadModels();
                findViewById(R.id.add_one).setEnabled(true);
                findViewById(R.id.add_ten).setEnabled(true);
            }
        });

        findViewById(R.id.delete_all_downloading).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDownloadService.deleteAllIncompletedDownloadModels();
            }
        });
        findViewById(R.id.delete_all_download_completed).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDownloadService.deleteAllDownloadedModels();
            }
        });

        mDownloadService.registeDownloadCallback(new IDownloadTaskCallback(){

            @Override
            public void onWait(@NonNull IDownloadModel downloadModel) {

            }

            @Override
            public void onStart(@NonNull IDownloadModel downloadModle) {
                Log.i(DownloadConstants.TAG,"onStart downloadModle.getDownloadPriority():"+downloadModle.getDownloadPriority());
                mMyAdapter.notifyDataSetChanged();
            }

            @Override
            public void onDownloadProgress(@NonNull IDownloadModel downloadModle) {
                Log.i(DownloadConstants.TAG,"onDownloadProgress downloadModle.getDownloadPriority():"+downloadModle.getDownloadPriority());
            }

            @Override
            public void onPause(@NonNull IDownloadModel downloadModle) {
                Log.i(DownloadConstants.TAG,"onPause downloadModle.getDownloadPriority():"+downloadModle.getDownloadPriority());
            }

            @Override
            public void onComplete(@NonNull IDownloadModel downloadModle) {
                Log.i(DownloadConstants.TAG,"onComplete downloadModle.getDownloadPriority():"+downloadModle.getDownloadPriority());
                if(mList.contains(downloadModle)) {
                    mList.remove(downloadModle);
                    mMyAdapter.notifyDataSetChanged();
                }
                if(!mList2.contains(downloadModle)) {
                    mList2.add((MyDownloadModel) downloadModle);
                    mMyAdapter2.notifyDataSetChanged();
                }
            }

            @Override
            public void onDelete(@NonNull IDownloadModel downloadModle) {
                Log.i(DownloadConstants.TAG,"onDelete downloadModle.getDownloadPriority():"+downloadModle.getDownloadPriority());
                if(downloadModle.getDownloadState()!=DownloadConstants.STATE_COMPLETE) {
                    if (mList.contains(downloadModle)) {
                        mList.remove(downloadModle);
                        mMyAdapter.notifyDataSetChanged();
                    }
                }else if (mList2.contains(downloadModle)) {
                    mList2.remove(downloadModle);
                    mMyAdapter2.notifyDataSetChanged();
                }

            }

            @Override
            public void onWait(@NonNull List<IDownloadModel> downloadModelList) {
            }

            @Override
            public void onDownloadProgress(@NonNull List<IDownloadModel> downloadModleList) {

                for(IDownloadModel downloadModle:downloadModleList){
                    Log.i(DownloadConstants.TAG,"onDownloadProgress downloadModleList.getDownloadPriority():"+downloadModle.getDownloadPriority());
                }
                //应该定向更新正在下载的item,这样效率更高，而不是直接notifyDataSetChanged，这里是为了方便，仅做演示
                mMyAdapter.notifyDataSetChanged();

            }

            @Override
            public void onPause(@NonNull List<IDownloadModel> downloadModleList) {
                for(IDownloadModel downloadModle:downloadModleList){
                    Log.i(DownloadConstants.TAG,"onPause downloadModleList.getDownloadPriority():"+downloadModle.getDownloadPriority());
                }
                mMyAdapter.notifyDataSetChanged();
            }

            @Override
            public void onDelete(@NonNull List<IDownloadModel> downloadModelList) {
                List<MyDownloadModel> inCompleteList = new ArrayList<>();
                List<MyDownloadModel> completeList = new ArrayList<>();
                for(IDownloadModel downloadModel:downloadModelList){
                    if(downloadModel.getDownloadState()!=DownloadConstants.STATE_COMPLETE){
                        inCompleteList.add((MyDownloadModel) downloadModel);
                    }else{
                        completeList.add((MyDownloadModel) downloadModel);
                    }
                }

                if(inCompleteList.size()>0){
                    mList.removeAll(inCompleteList);
                    mMyAdapter.notifyDataSetChanged();
                }

                if(completeList.size()>0){
                    mList2.removeAll(completeList);
                    mMyAdapter2.notifyDataSetChanged();
                }
            }

            @Override
            public void onAdd(@NonNull IDownloadModel downloadModel) {
                if(downloadModel.getDownloadState()!=DownloadConstants.STATE_COMPLETE) {
                    if(!mList.contains(downloadModel)) {
                        mList.add((MyDownloadModel) downloadModel);
                        mMyAdapter.notifyDataSetChanged();
                    }
                }else if(!mList2.contains(downloadModel)){
                    mList2.add((MyDownloadModel) downloadModel);
                    mMyAdapter2.notifyDataSetChanged();
                }

            }

            @Override
            public void onError(@NonNull IDownloadModel downloadModel, int errorType, String errorMsg) {
                Log.i(DownloadConstants.TAG,"onError downloadModle.getDownloadPriority():"+downloadModel.getDownloadPriority()
                        +" errorType:"+errorType+" errorMsg:"+errorMsg);
                mMyAdapter.notifyDataSetChanged();
            }

            @Override
            public void onAdd(@NonNull List<IDownloadModel> downloadModelList) {
                List<MyDownloadModel> inCompleteList = new ArrayList<>();
                List<MyDownloadModel> completeList = new ArrayList<>();
                for(IDownloadModel downloadModel:downloadModelList){
                    if(downloadModel.getDownloadState()!=DownloadConstants.STATE_COMPLETE){
                        if(!mList.contains(downloadModel)) {
                            inCompleteList.add((MyDownloadModel) downloadModel);
                        }
                    }else if(!mList2.contains(downloadModel)){
                        completeList.add((MyDownloadModel) downloadModel);
                    }
                }

                if(inCompleteList.size()>0){
                    mList.addAll(inCompleteList);
                    mMyAdapter.notifyDataSetChanged();
                }

                if(completeList.size()>0){
                    mList2.addAll(completeList);
                    mMyAdapter2.notifyDataSetChanged();
                }

            }
        });

    }

    public class MyAdapter extends BaseAdapter{

        List<MyDownloadModel> list;

        public MyAdapter(List<MyDownloadModel> list){
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            if(convertView==null) {
                convertView = MainActivity.this.getLayoutInflater().inflate(R.layout.item,null);
            }
            final MyDownloadModel downloadModel = list.get(position);
            TextView textView= (TextView) convertView.findViewById(R.id.textview);
            textView.setText("Progress:"+downloadModel.getDownloadProgress()
                    +"\n State:"+convertState(downloadModel.getDownloadState()));
            ((TextView) convertView.findViewById(R.id.priority)).setText(list.get(position).getDownloadPriority()+"");
            Button pauseBtn = (Button) convertView.findViewById(R.id.pause);
            pauseBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Button button = (Button)v;
                    MyDownloadModel downloadModel = list.get(position);

                    if(downloadModel.getDownloadState()==DownloadConstants.STATE_PAUSE
                            ||downloadModel.getDownloadState()==DownloadConstants.STATE_WAITING){
                        DownloadService.getInstance().resumeDownloadModel(downloadModel);
                        button.setText("PAUSE");
                    }else if(downloadModel.getDownloadState()==DownloadConstants.STATE_DOWNLOADING){
                        DownloadService.getInstance().pauseDownloadModel(list.get(position));
                        button.setText("RESUME");
                    }
                }
            });

            if(downloadModel.getDownloadState()==DownloadConstants.STATE_PAUSE
                    ||downloadModel.getDownloadState()==DownloadConstants.STATE_WAITING){
                pauseBtn.setText("RESUME");
            }else if(downloadModel.getDownloadState()==DownloadConstants.STATE_DOWNLOADING){
                pauseBtn.setText("PAUSE");
            }else if(downloadModel.getDownloadState()==DownloadConstants.STATE_COMPLETE){
                pauseBtn.setText("完成");
            }

            convertView.findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDownloadService.deleteDownloadModel(downloadModel);
//                    list.remove(downloadModel);
//                    notifyDataSetChanged();
                }
            });
            return convertView;
        }
    }

    public class MyPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return mViewList.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(mViewList.get(position));
            return mViewList.get(position);
        }
    }

    private String convertState(int state){
        switch (state){
            case DownloadConstants.STATE_COMPLETE:
                return "下载完成";
                case DownloadConstants.STATE_DOWNLOADING:
                    return "下载中";
                    case DownloadConstants.STATE_PAUSE:
                        return "暂停";
                        case DownloadConstants.STATE_WAITING:
                            return "等待下载";

        }
        return "";
    }
}
