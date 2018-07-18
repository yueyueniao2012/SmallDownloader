package com.yyn.download;

import android.os.Handler;
import android.os.Looper;


import com.yyn.download.DownloadTaskManager;
import com.yyn.download.ExecutorDelivery;
import com.yyn.download.IDBHandler;
import com.yyn.download.IDownloadModel;
import com.yyn.download.IDownloadService;
import com.yyn.download.IDownloadTaskCallback;
import com.yyn.download.IDownloadTaskHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yyn
 *
 * @author yyn
 */
public class DownloadService implements IDownloadService {

    private List<IDownloadTaskCallback> mDownloadCallBackList;
    private IDownloadTaskHandler mIDownloadTaskHandler;
    private Handler mHandler;
    private DownloadTaskManager mDownloadTaskManager;
    private ExecutorDelivery mExecutorDelivery;

    private DownloadService() {
        init();
    }

    private static class DownloadServiceStub{
        static DownloadService sDownloadService = new DownloadService();
    }

    public static DownloadService getInstance(){
        return DownloadServiceStub.sDownloadService;
    }

    private void init() {

        mHandler = new Handler(Looper.getMainLooper());
        mDownloadCallBackList = new ArrayList<>();
        mExecutorDelivery = new ExecutorDelivery(mHandler);
        mExecutorDelivery.setDownloadCallBackList(mDownloadCallBackList);
        mDownloadTaskManager = new DownloadTaskManager(this,mExecutorDelivery);

    }

    @Override
    public void deleteAllIncompletedDownloadModels() {
        mDownloadTaskManager.deleteAllIncompletedDownloadModels();
    }

    @Override
    public void deleteAllDownloadedModels() {
        mDownloadTaskManager.deleteAllDownloadedModels();
    }

    @Override
    public void deleteAll() {
        mDownloadTaskManager.deleteAll();
    }

    @Override
    public void deleteDownloadModels(List<IDownloadModel> list) {
        mDownloadTaskManager.deleteDownloadModels(list);
    }

    @Override
    public void deleteDownloadModel(IDownloadModel downloadModel) {
        mDownloadTaskManager.deleteDownloadModel(downloadModel);
    }

    @Override
    public void addDownloadModel(IDownloadModel downloadModel, boolean isAutoDownload) {
        mDownloadTaskManager.addDownloadModel(downloadModel,isAutoDownload);
    }

    @Override
    public void addDownloadModels(List<IDownloadModel> list, boolean isAutoDownload) {
        mDownloadTaskManager.addDownloadModels(list,isAutoDownload);
    }

    @Override
    public void resumeDownloadModel(IDownloadModel downloadModel) {
        mDownloadTaskManager.resumeDownloadModel(downloadModel);
    }

    @Override
    public void resumeAllDownloadModels() {
        mDownloadTaskManager.resumeAllDownloadModels();
    }

    @Override
    public void pauseDownloadModel(IDownloadModel downloadModel) {
        mDownloadTaskManager.pauseDownloadModel(downloadModel);
    }

    @Override
    public void pauseAllDownloadModels() {
        mDownloadTaskManager.pauseAllDownloadModels();
    }

    @Override
    public List<IDownloadModel> getAllInCompletedDownloadedModels() {
        return mDownloadTaskManager.getAllInCompletedDownloadedModels();
    }

    @Override
    public List<IDownloadModel> getAllCompletedDownloadedModels() {
        return mDownloadTaskManager.getAllCompletedDownloadedModels();
    }

    @Override
    public List<IDownloadModel> getDownloadingModels() {
        return mDownloadTaskManager.getDownloadingModels();
    }

    @Override
    public List<IDownloadModel> getWaitingModels() {
        return mDownloadTaskManager.getWaitingModels();
    }

    @Override
    public List<IDownloadModel> getPauseModels() {
        return mDownloadTaskManager.getPauseModels();
    }

    @Override
    public int getDownloadModelSate(IDownloadModel downloadModel) {
        return mDownloadTaskManager.getDownloadModelSate(downloadModel);
    }

    private void addDownloadModels(List<IDownloadModel> list, boolean isAutoDownload, boolean shallSaveToDb) {
        mDownloadTaskManager.addDownloadModels(list,isAutoDownload,shallSaveToDb);
    }

    @Override
    public void registeDownloadCallback(IDownloadTaskCallback downloadCallBack) {
        if(!mDownloadCallBackList.contains(downloadCallBack)) {
            mDownloadCallBackList.add(downloadCallBack);
        }
    }

    @Override
    public void unRegisteDownloadCallback(IDownloadTaskCallback downloadCallBack) {
        mDownloadCallBackList.remove(downloadCallBack);
    }

    @Override
    public void registeDBHandler(IDBHandler dBHandler) {
       mDownloadTaskManager.setIDBHandler(dBHandler);
    }

    @Override
    public void unRegisteDBHandler() {
        mDownloadTaskManager.setIDBHandler(null);
    }

    @Override
    public void registeDownloadTaskHandler(IDownloadTaskHandler downloadTaskHandler) {
        mIDownloadTaskHandler = downloadTaskHandler;
        mDownloadTaskManager.setIDownloadTaskHandler(mIDownloadTaskHandler);
    }

    @Override
    public void unRegisteDownloadTaskHandler() {
        mIDownloadTaskHandler = null;
        mDownloadTaskManager.setIDownloadTaskHandler(null);
    }

}
