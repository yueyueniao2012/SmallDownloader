package com.example.xmdownloadertest;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.yyn.download.DownloadConstants;
import com.yyn.download.DownloadService;
import com.yyn.download.IDBHandler;
import com.yyn.download.IDBHandlerInitCallback;
import com.yyn.download.IDownloadModel;
import com.yyn.download.IDownloadTaskHandler;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.android.AndroidObjectBrowser;

public class MyApplication extends Application {

    public static final String TAG = "ObjectBoxExample";
    public static final boolean EXTERNAL_DIR = false;

    private BoxStore boxStore;

    @Override
    public void onCreate() {
        super.onCreate();
        boxStore = MyObjectBox.builder().androidContext(this).build();
        if (BuildConfig.DEBUG) {
            new AndroidObjectBrowser(boxStore).start(this);
        }

        Log.d("App", "Using ObjectBox " + BoxStore.getVersion() + " (" + BoxStore.getVersionNative() + ")");

        DownloadService downloadService = DownloadService.getInstance();
        downloadService.registeDownloadTaskHandler(new IDownloadTaskHandler() {

            @Override
            public boolean isNetWorkAvailable() {
                return true;
            }

            @Override
            public void updateDownloadModel(IDownloadModel downloadModel) {
            }

            @Override
            public String getSaveToSdcardBasePath() {
                File file[] = ContextCompat.getExternalFilesDirs(MyApplication.this,"download");
                if(file==null){
                    return getFilesDir().getPath();
                }
                return file[0].getPath();
            }

            @Override
            public String generateSaveDownloadFilePath(String basePath,IDownloadModel downloadModel) {
                String downloadUrl = downloadModel.getDownloadUrl();
                downloadUrl = downloadUrl + new Random().nextInt();
                return new File(basePath,MD5Tool.md5(downloadUrl.getBytes())).getAbsolutePath();
            }

            @Override
            public HttpURLConnection createConnection(IDownloadModel mDownloadModel, Map<String, String> headMap) throws IOException {
                URL url = null;
                try {
                    url = new URL(mDownloadModel.getDownloadUrl());
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                if(headMap!=null) {
                    for (Map.Entry<String, String> entry : headMap.entrySet()) {
                        httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }
                httpURLConnection.setConnectTimeout(10*1000);
                httpURLConnection.setReadTimeout(10*1000);
                return httpURLConnection;
            }

            @Override
            public void onComplete(IDownloadModel mDownloadModel) {

            }
        });

        downloadService.registeDBHandler(new IDBHandler() {
            @Override
            public void initDataFromDb(IDBHandlerInitCallback dBHandlerInitCallback) {
                List<IDownloadModel> downloadTasks = new ArrayList<>();

                Box<MyDownloadModel> box = boxStore.boxFor(MyDownloadModel.class);
                List<MyDownloadModel> downloadModels = box.query().build().find();
                downloadTasks.addAll(downloadModels);


                dBHandlerInitCallback.initReady(downloadTasks,true);
                Log.i(DownloadConstants.TAG,"initDataFromDb currentThread name:"+Thread.currentThread().getName()+" id:"+Thread.currentThread().getId());
            }

            @Override
            public boolean saveDownloadModel(@NonNull IDownloadModel task) {
                Log.i(DownloadConstants.TAG,"saveDownloadModel currentThread name:"+Thread.currentThread().getName()+" id:"+Thread.currentThread().getId());
                if(task instanceof MyDownloadModel){
                    MyDownloadModel downloadModel = (MyDownloadModel)task;
                    Box<MyDownloadModel> box = boxStore.boxFor(MyDownloadModel.class);
                    box.put(downloadModel);
                    return true;
                }
                return true;
            }

            @Override
            public boolean saveDownloadModel(@NonNull List<IDownloadModel> tasks) {
                Log.i(DownloadConstants.TAG,"saveDownloadModel list currentThread name:"+Thread.currentThread().getName()+" id:"+Thread.currentThread().getId());
                Box<MyDownloadModel> box = boxStore.boxFor(MyDownloadModel.class);
                List<MyDownloadModel> downloadModels = new ArrayList<>();
                for(IDownloadModel downloadModel : tasks){
                    if(downloadModel instanceof MyDownloadModel){
                        downloadModels.add((MyDownloadModel) downloadModel);
                    }
                }

                box.put(downloadModels);

                return true;
            }

            @Override
            public boolean deleteDownloadModel(@NonNull IDownloadModel task) {
                Log.i(DownloadConstants.TAG,"deleteDownloadModel currentThread name:"+Thread.currentThread().getName()+" id:"+Thread.currentThread().getId());
                if(task instanceof MyDownloadModel){
                    MyDownloadModel downloadModel = (MyDownloadModel)task;
                    Box<MyDownloadModel> box = boxStore.boxFor(MyDownloadModel.class);
                    box.remove(downloadModel);

                    return true;
                }

                return false;
            }

            @Override
            public boolean deleteDownloadModel(@NonNull List<IDownloadModel> tasks) {
                Log.i(DownloadConstants.TAG,"deleteDownloadModel list currentThread name:"+Thread.currentThread().getName()+" id:"+Thread.currentThread().getId());
                Box<MyDownloadModel> box = boxStore.boxFor(MyDownloadModel.class);
                List<MyDownloadModel> downloadModels = new ArrayList<>();
                for(IDownloadModel downloadModel : tasks){
                    if(downloadModel instanceof MyDownloadModel){
                        downloadModels.add((MyDownloadModel)downloadModel);
                    }
                }
                box.remove(downloadModels);

                return true;
            }
        });


    }

    public BoxStore getBoxStore() {
        return boxStore;
    }
}
