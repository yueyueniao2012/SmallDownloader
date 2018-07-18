package com.yyn.download;

import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.yyn.download.DownloadConstants;
import com.yyn.download.DownloadTask;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by yyn
 *
 * @author yyn
 */
public class DownloadTaskManager implements IDownloadTaskCallback,IDownloadTaskManager {
    private IDownloadService mIDownloadService;
    private List<IDownloadModel> mAllDownloadModuleList;
    private PriorityBlockingQueue<Runnable> mDownloadingQueue;
    private static final int CORE_POOL_SIZE = 3;

    public void setIDownloadTaskHandler(IDownloadTaskHandler downloadTaskHandler) {
        this.mIDownloadTaskHandler = downloadTaskHandler;
    }

    public void setIDBHandler(final IDBHandler dBHandler) {

        if(dBHandler==null){
            mIDBHandler = null;
            return;
        }

        mIDBHandler = (IDBHandler)Proxy.newProxyInstance(getClass().getClassLoader(),dBHandler.getClass().getInterfaces(),new InvocationHandler (){
            @Override
            public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
                if(Looper.getMainLooper()==Looper.myLooper()){
                    mJobThreadPoolExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                method.invoke(dBHandler, args);
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            } catch (InvocationTargetException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    return false;
                }else {
                    return method.invoke(dBHandler, args);
                }
            }
        });

        mIDBHandler.initDataFromDb(new IDBHandlerInitCallback() {
            @Override
            public void initReady(List<IDownloadModel> list, boolean isAutoDownload) {
                //按照优先级排序后再添加到下载列表中
                Collections.sort(list, new Comparator<IDownloadModel>() {
                    @Override
                    public int compare(IDownloadModel o1, IDownloadModel o2) {
                        return o2.getDownloadPriority() - o1.getDownloadPriority();
                    }
                });
                addDownloadModels(list, isAutoDownload, false);
            }
        });

    }

    private IDownloadTaskHandler mIDownloadTaskHandler;
    private IDBHandler mIDBHandler;
    private ExecutorDelivery mExecutorDelivery;
    private ThreadPoolExecutor mDownloadThreadPoolExecutor;
    private List<DownloadTask> mDownloaddingTaskList = Collections.synchronizedList(new ArrayList<DownloadTask>());

    private LinkedBlockingDeque<Runnable> mJobQueue;
    private static final int CORE_JOB_POOL_SIZE = 1;
    private ThreadPoolExecutor mJobThreadPoolExecutor;

    private static final ThreadFactory mThreadFactory = new ThreadFactory() {

        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "XmDownloadTask #" + mCount.getAndIncrement());
        }
    };

    private static final ThreadFactory mJobThreadFactory = new ThreadFactory() {

        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "XmDownloadJobTask #" + mCount.getAndIncrement());
        }
    };

    public DownloadTaskManager(IDownloadService downloadService,ExecutorDelivery executorDelivery) {
        mIDownloadService = downloadService;
        mExecutorDelivery = executorDelivery;
        mAllDownloadModuleList = Collections.synchronizedList(new ArrayList<IDownloadModel>());;
        mDownloadingQueue = new PriorityBlockingQueue<>();
        mDownloadThreadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                CORE_POOL_SIZE,
                10,
                TimeUnit.SECONDS,
                mDownloadingQueue,
                mThreadFactory){
            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                super.beforeExecute(t, r);
                DownloadTask downloadTask = (DownloadTask)r;
                Log.i(DownloadConstants.TAG,"beforeExecute downloadTask mDownloaddingTaskList size before add:"+mDownloaddingTaskList.size());
                mDownloaddingTaskList.add(downloadTask);
                Log.i(DownloadConstants.TAG,"beforeExecute downloadTask mDownloaddingTaskList size after add:"+mDownloaddingTaskList.size());
                if(mDownloaddingTaskList.size()>3){
                    Log.i(DownloadConstants.TAG,"异常情况");
                }
            }

            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                DownloadTask downloadTask = (DownloadTask)r;
//                Log.i(DownloadConstants.TAG,"afterExecute downloadTask:"+downloadTask.getDownloadModel().getDownloadPriority()+
//                " getDownloadState()："+downloadTask.getDownloadModel().getDownloadState());
                Log.i(DownloadConstants.TAG,"afterExecute downloadTask mDownloaddingTaskList size before remove:"+mDownloaddingTaskList.size());
                mDownloaddingTaskList.remove(downloadTask);
                Log.i(DownloadConstants.TAG,"afterExecute downloadTask mDownloaddingTaskList size after remove:"+mDownloaddingTaskList.size());
                if(downloadTask.shallRetryAfterExecute()
                        && downloadTask.getDownloadModel().getDownloadState()!=DownloadConstants.STATE_COMPLETE){
                    //如果非用户手动暂停，重新加入下载队列中
                    DownloadTask maxPriorityDownloadTask = (DownloadTask) mDownloadingQueue.peek();
                    if(maxPriorityDownloadTask!=null) {
                        downloadTask.getDownloadModel().setDownloadPriority(maxPriorityDownloadTask.getDownloadModel().getDownloadPriority());
                    }
                    downloadTask.resumeTask();
                    submitTaskToExecutor(downloadTask);
                }
            }
        };
        mDownloadThreadPoolExecutor.allowCoreThreadTimeOut(true);
        Executors.newCachedThreadPool();
        mJobQueue = new LinkedBlockingDeque<>();
        mJobThreadPoolExecutor = new ThreadPoolExecutor(
                CORE_JOB_POOL_SIZE,
                CORE_JOB_POOL_SIZE,
                10,
                TimeUnit.SECONDS,
                mJobQueue,
                mJobThreadFactory);
        mJobThreadPoolExecutor.allowCoreThreadTimeOut(true);
    }

    @Override
    public void addDownloadModel(IDownloadModel downloadModel, boolean isAutoDownload) {

        synchronized (mAllDownloadModuleList) {
            if (!mAllDownloadModuleList.contains(downloadModel)) {
//                downloadModel.setDownloadPriority(IDownloadModel.DEFAULT_PRIORITY);
                mAllDownloadModuleList.add(downloadModel);
            }
        }

        boolean fileExist = false;
        if(!TextUtils.isEmpty(downloadModel.getSavedFileToSdcardPath())){
            File downloadFile = new File(downloadModel.getSavedFileToSdcardPath());
            fileExist = downloadFile.exists();
        }

        //下载的文件被删除
        if(!fileExist||downloadModel.getDownloadState()!=DownloadConstants.STATE_COMPLETE){
            if(isAutoDownload){
                downloadModel.setDownloadState(DownloadConstants.STATE_WAITING);
                submitRunnableToExecutor(new DownloadTask(downloadModel, mIDownloadTaskHandler,this,mIDBHandler));
            }else{
                downloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
            }
        }
        onAdd(downloadModel);
    }

    private void submitRunnableToExecutor(DownloadTask downloadTask) {

        synchronized (mDownloaddingTaskList) {
            if (mDownloaddingTaskList.contains(downloadTask)) {
                downloadTask.getDownloadModel().setDownloadState(DownloadConstants.STATE_DOWNLOADING);
                return;
            }
            synchronized (mDownloadingQueue) {
                if (mDownloadingQueue.contains(downloadTask)) {
                    downloadTask.getDownloadModel().setDownloadState(DownloadConstants.STATE_WAITING);
                    return;
                }
                mDownloadThreadPoolExecutor.execute(downloadTask);
            }
        }
    }

    @Override
    public void addDownloadModels(List<IDownloadModel> list, boolean isAutoDownload) {
        addDownloadModels(list,isAutoDownload,true);
    }

    public void addDownloadModels(List<IDownloadModel> list, boolean isAutoDownload, boolean shallSaveToDb) {
        List<IDownloadModel> saveList = new ArrayList<>();
        if(!shallSaveToDb){
            //说明是从DB中读取的数据，不用再次进行Save
            for(IDownloadModel downloadModel:list){
                synchronized (mAllDownloadModuleList) {
                    if (!mAllDownloadModuleList.contains(downloadModel)) {
                        mAllDownloadModuleList.add(downloadModel);
                    }
                }

                boolean fileExist = false;
                if(!TextUtils.isEmpty(downloadModel.getSavedFileToSdcardPath())){
                    File downloadFile = new File(downloadModel.getSavedFileToSdcardPath());
                    fileExist = downloadFile.exists();
                }

                //下载的文件被删除
                if(!fileExist||downloadModel.getDownloadState()!=DownloadConstants.STATE_COMPLETE){
                    if(isAutoDownload){
                        downloadModel.setDownloadState(DownloadConstants.STATE_WAITING);
                        //将合并触发Add事件，这里省略了EVENT_WAIT事件,Add事件拥有更高的优先级
                        submitRunnableToExecutor(new DownloadTask(downloadModel, mIDownloadTaskHandler,this,mIDBHandler));
                    }else{
                        downloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
                    }
                }
            }
            mExecutorDelivery.post(list,IDownloadTaskCallback.EVENT_ADD);
        }else{
            //说明是新增下载数据需要保存到DB中
            for(IDownloadModel downloadModel:list){
                synchronized (mAllDownloadModuleList) {
                    if (!mAllDownloadModuleList.contains(downloadModel)) {
                        downloadModel.setDownloadPriority(IDownloadModel.DEFAULT_PRIORITY);
                        mAllDownloadModuleList.add(downloadModel);
                        //将合并触发Add事件，这里省略了EVENT_WAIT事件,Add事件拥有更高的优先级
                        downloadModel.setDownloadState(DownloadConstants.STATE_WAITING);
                        saveList.add(downloadModel);
                        submitRunnableToExecutor(new DownloadTask(downloadModel, mIDownloadTaskHandler,this,mIDBHandler));
                    }
                }
            }
            onAdd(saveList);
        }

    }

    @Override
    public void deleteAllIncompletedDownloadModels() {


        List<IDownloadModel> deleteList = new ArrayList<>();

        synchronized (mAllDownloadModuleList) {
            Iterator<IDownloadModel> iterable = mAllDownloadModuleList.iterator();
            while (iterable.hasNext()) {
                IDownloadModel downloadModel = iterable.next();
                if (downloadModel.getDownloadState() != DownloadConstants.STATE_COMPLETE) {
                    iterable.remove();
                    deleteList.add(downloadModel);
                }
            }
        }

        //清除线程池中的任务
        mDownloadThreadPoolExecutor.getQueue().clear();
        synchronized (mDownloaddingTaskList) {
            for (DownloadTask downloadTask : mDownloaddingTaskList) {
                downloadTask.cancleTask(false);
            }
        }

        onDelete(deleteList);

    }

    @Override
    public void deleteAllDownloadedModels() {
        List<IDownloadModel> deleteList = new ArrayList<>();

        synchronized (mAllDownloadModuleList) {
            Iterator<IDownloadModel> iterable = mAllDownloadModuleList.iterator();
            while (iterable.hasNext()) {
                IDownloadModel downloadModel = iterable.next();
                if (downloadModel.getDownloadState() == DownloadConstants.STATE_COMPLETE) {
                    iterable.remove();
                    deleteList.add(downloadModel);
                }
            }
        }

        onDelete(deleteList);
    }

    @Override
    public void deleteAll() {
        List<IDownloadModel> deleteList = new ArrayList<>();

        synchronized (mAllDownloadModuleList) {
            Iterator<IDownloadModel> iterable = mAllDownloadModuleList.iterator();
            while (iterable.hasNext()) {
                IDownloadModel downloadModel = iterable.next();
                iterable.remove();
                deleteList.add(downloadModel);
            }
        }

        //清除线程池中的任务
        mDownloadThreadPoolExecutor.getQueue().clear();
        synchronized (mDownloaddingTaskList) {
            for (DownloadTask downloadTask : mDownloaddingTaskList) {
                downloadTask.cancleTask(false);
            }
        }

        onDelete(deleteList);

    }

    @Override
    public void deleteDownloadModels(List<IDownloadModel> list) {
        List<IDownloadModel> deleteList = new ArrayList<>();
        synchronized (mAllDownloadModuleList) {
            for(IDownloadModel downloadModel:list){
                if(mAllDownloadModuleList.contains(downloadModel)){
                    mAllDownloadModuleList.remove(downloadModel);
                    deleteList.add(downloadModel);
                }
                synchronized (mDownloaddingTaskList) {
                    for (DownloadTask downloadTask : mDownloaddingTaskList) {
                        if (downloadTask.getDownloadModel().equals(downloadModel)) {
                            downloadTask.cancleTask(false);
                        }
                    }
                }
                //PriorityBlockingQueue是拷贝数组迭代，不用加锁
                for (Runnable runnable : mDownloadingQueue) {
                    DownloadTask downloadTask = (DownloadTask)runnable;
                    if (downloadTask.getDownloadModel().equals(downloadModel)) {
                        downloadTask.cancleTask(false);
                        mDownloadThreadPoolExecutor.remove(runnable);
                    }
                }
            }
        }
        onDelete(deleteList);

    }

    @Override
    public void deleteDownloadModel(IDownloadModel downloadModel) {
        synchronized (mAllDownloadModuleList) {
            if (mAllDownloadModuleList.contains(downloadModel)) {
                mAllDownloadModuleList.remove(downloadModel);
                onDelete(downloadModel);
            }
        }
        synchronized (mDownloaddingTaskList) {
            for (DownloadTask downloadTask : mDownloaddingTaskList) {
                if (downloadTask.getDownloadModel().equals(downloadModel)) {
                    downloadTask.cancleTask(false);
                }
            }
        }
        //PriorityBlockingQueue是拷贝数组迭代，不用加锁
        for (Runnable runnable : mDownloadingQueue) {
            DownloadTask downloadTask = (DownloadTask)runnable;
            if (downloadTask.getDownloadModel().equals(downloadModel)) {
                downloadTask.cancleTask(false);
                mDownloadThreadPoolExecutor.remove(runnable);
            }
        }
    }

    private void deleteDownloadModelFile(final List<IDownloadModel> list){
        mJobThreadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                for(IDownloadModel downloadModel:list){
                    if(!TextUtils.isEmpty(downloadModel.getSavedFileToSdcardPath())) {
                        try {
                            File file = new File(downloadModel.getSavedFileToSdcardPath());
                            if (file.exists()) {
                                file.delete();
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

    }

    @Override
    public void resumeDownloadModel(IDownloadModel downloadModel) {

        synchronized (mAllDownloadModuleList) {
            if (!mAllDownloadModuleList.contains(downloadModel)) {
                addDownloadModel(downloadModel, true);
                return;
            }
        }

        if(downloadModel.getDownloadState()==DownloadConstants.STATE_WAITING) {
            DownloadTask downloadTask = null;
            DownloadTask maxPriorityDownloadTask = (DownloadTask) mDownloadingQueue.peek();
            if (maxPriorityDownloadTask != null) {
                if (!maxPriorityDownloadTask.getDownloadModel().equals(downloadModel)) {
                    for (Runnable runnable : mDownloadingQueue) {
                        DownloadTask downloadTaskInQueue = (DownloadTask) runnable;
                        if (downloadTaskInQueue.getDownloadModel().equals(downloadModel)) {
                            mDownloadThreadPoolExecutor.remove(downloadTaskInQueue);
                            downloadTask = downloadTaskInQueue;
                            break;
                        }
                    }
                } else {
                    downloadTask = maxPriorityDownloadTask;
                }
                //将该任务优先级提高
                if (downloadTask != null) {
                    downloadTask.getDownloadModel().setDownloadPriority(maxPriorityDownloadTask.getDownloadModel().getDownloadPriority() + 1);
                    downloadModel = downloadTask.getDownloadModel();
                } else {
                    downloadModel.setDownloadPriority(maxPriorityDownloadTask.getDownloadModel().getDownloadPriority() + 1);
                }
            }
            if (downloadTask == null) {
                downloadTask = new DownloadTask(downloadModel, mIDownloadTaskHandler, this,mIDBHandler);
            }
            downloadModel.setDownloadState(DownloadConstants.STATE_WAITING);

            synchronized (mDownloaddingTaskList) {
                if (mDownloaddingTaskList.size() == CORE_POOL_SIZE) {
                    DownloadTask cancleTask = mDownloaddingTaskList.get(0);
                    if (cancleTask.getDownloadModel().getDownloadPriority() >= downloadModel.getDownloadPriority()) {
                        downloadModel.setDownloadPriority(cancleTask.getDownloadModel().getDownloadPriority() + 1);
                    }
                    submitTaskToExecutor(downloadTask);
                    cancleTask.cancleTask(true);
                } else {
                    submitTaskToExecutor(downloadTask);
                }
            }
//            mExecutorDelivery.post(downloadModel,IDownloadTaskCallback.EVENT_START);
        }else if(downloadModel.getDownloadState() != DownloadConstants.STATE_COMPLETE) {
            boolean shallAddInDownloadingQueue = true;

            for(Runnable runnable:mDownloadingQueue){
                DownloadTask downloadTask = (DownloadTask) runnable;
                if(downloadTask.getDownloadModel().equals(downloadModel)){
                    downloadModel.setDownloadState(DownloadConstants.STATE_WAITING);
                    mExecutorDelivery.post(downloadModel,IDownloadTaskCallback.EVENT_WAIT);
                    shallAddInDownloadingQueue = false;
                    break;
                }
            }

            synchronized (mDownloaddingTaskList) {
                for (DownloadTask downloadTask : mDownloaddingTaskList) {
                    if (downloadTask.getDownloadModel().equals(downloadModel)) {
                        downloadModel.setDownloadState(DownloadConstants.STATE_DOWNLOADING);
                        mExecutorDelivery.post(downloadModel, IDownloadTaskCallback.EVENT_START);
                        shallAddInDownloadingQueue = false;
                        break;
                    }
                }
            }
            if(shallAddInDownloadingQueue){
                DownloadTask downloadTask = new DownloadTask(downloadModel,mIDownloadTaskHandler,this,mIDBHandler);
                submitTaskToExecutor(downloadTask);
                downloadModel.setDownloadState(DownloadConstants.STATE_WAITING);
                mExecutorDelivery.post(downloadModel, IDownloadTaskCallback.EVENT_WAIT);
            }
        }

    }

    @Override
    public void resumeAllDownloadModels() {
        List<IDownloadModel> resumeList = new ArrayList<>();
        synchronized (mAllDownloadModuleList) {
            for (IDownloadModel downloadModel : mAllDownloadModuleList) {
                if (downloadModel.getDownloadState() != DownloadConstants.STATE_COMPLETE) {
                    boolean shallAddInDownloadingQueue = true;

                    for(Runnable runnable:mDownloadingQueue){
                        DownloadTask downloadTask = (DownloadTask) runnable;
                        if(downloadTask.getDownloadModel().equals(downloadModel)){
                            downloadModel.setDownloadState(DownloadConstants.STATE_WAITING);
                            shallAddInDownloadingQueue = false;
                            break;
                        }
                    }

                    synchronized (mDownloaddingTaskList) {
                        for (DownloadTask downloadTask : mDownloaddingTaskList) {
                            if (downloadTask.getDownloadModel().equals(downloadModel)) {
                                downloadModel.setDownloadState(DownloadConstants.STATE_DOWNLOADING);
                                shallAddInDownloadingQueue = false;
                                break;
                            }
                        }
                    }
                    if(shallAddInDownloadingQueue) {
                        resumeList.add(downloadModel);
                        downloadModel.setDownloadState(DownloadConstants.STATE_WAITING);
                        DownloadTask downloadTask = new DownloadTask(downloadModel, mIDownloadTaskHandler, this, mIDBHandler);
                        submitRunnableToExecutor(downloadTask);
                    }
                }
            }
        }
        onWait(resumeList);
    }

    @Override
    public void pauseDownloadModel(IDownloadModel downloadModel) {
        if(!mAllDownloadModuleList.contains(downloadModel)){
            return;
        }
        boolean isAbnormalSate = true;
        synchronized (mDownloaddingTaskList) {
            for (DownloadTask downloadTask : mDownloaddingTaskList) {
                if (downloadTask.getDownloadModel().equals(downloadModel)) {
                    downloadTask.cancleTask(false);
                    isAbnormalSate = false;
                }
            }
        }

        for(Runnable runnable:mDownloadingQueue){
            DownloadTask downloadTaskInQueue = (DownloadTask)runnable;
            if(downloadTaskInQueue.getDownloadModel().equals(downloadModel)){
                mDownloadThreadPoolExecutor.remove(downloadTaskInQueue);
                onPause(downloadModel);
                isAbnormalSate = false;
                break;
            }
        }

        //异常状态处理：不在正在下载列表中
        if(isAbnormalSate
                && (downloadModel.getDownloadState() == DownloadConstants.STATE_DOWNLOADING
                || downloadModel.getDownloadState() == DownloadConstants.STATE_WAITING)){
            onPause(downloadModel);
        }

    }

    @Override
    public void pauseAllDownloadModels() {

        //清除线程池中的任务
        mDownloadThreadPoolExecutor.getQueue().clear();

        synchronized (mDownloaddingTaskList) {
            for (DownloadTask downloadTask : mDownloaddingTaskList) {
                downloadTask.cancleTask(false);
            }
        }

        List<IDownloadModel> pauseList = new ArrayList<>();
        synchronized (mAllDownloadModuleList) {
            for (IDownloadModel downloadModel : mAllDownloadModuleList) {
                if (downloadModel.getDownloadState() != DownloadConstants.STATE_COMPLETE) {
                    downloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
                    pauseList.add(downloadModel);
                }
            }
        }

        onPause(pauseList);

    }

    @Override
    public List<IDownloadModel> getAllInCompletedDownloadedModels() {

        List<IDownloadModel> list = new ArrayList<>();

        synchronized (mAllDownloadModuleList) {
            for (IDownloadModel downloadModel : mAllDownloadModuleList) {
                if (downloadModel.getDownloadState() != DownloadConstants.STATE_COMPLETE) {
                    list.add(downloadModel);
                }
            }
        }

        return list;
    }

    @Override
    public List<IDownloadModel> getAllCompletedDownloadedModels() {
        List<IDownloadModel> list = new ArrayList<>();

        synchronized (mAllDownloadModuleList) {
            for (IDownloadModel downloadModel : mAllDownloadModuleList) {
                if (downloadModel.getDownloadState() == DownloadConstants.STATE_COMPLETE) {
                    list.add(downloadModel);
                }
            }
        }
        return list;
    }

    @Override
    public List<IDownloadModel> getDownloadingModels() {
        List<IDownloadModel> list = new ArrayList<>();

        synchronized (mAllDownloadModuleList) {
            for (IDownloadModel downloadModel : mAllDownloadModuleList) {
                if (downloadModel.getDownloadState() == DownloadConstants.STATE_DOWNLOADING) {
                    list.add(downloadModel);
                }
            }
        }
        return list;
    }

    @Override
    public List<IDownloadModel> getWaitingModels() {
        List<IDownloadModel> list = new ArrayList<>();

        synchronized (mAllDownloadModuleList) {
            for (IDownloadModel downloadModel : mAllDownloadModuleList) {
                if (downloadModel.getDownloadState() == DownloadConstants.STATE_WAITING) {
                    list.add(downloadModel);
                }
            }
        }
        return list;
    }

    @Override
    public List<IDownloadModel> getPauseModels() {
        List<IDownloadModel> list = new ArrayList<>();

        synchronized (mAllDownloadModuleList) {
            for (IDownloadModel downloadModel : mAllDownloadModuleList) {
                if (downloadModel.getDownloadState() == DownloadConstants.STATE_PAUSE) {
                    list.add(downloadModel);
                }
            }
        }
        return list;
    }

    @Override
    public int getDownloadModelSate(IDownloadModel downloadModel) {
        synchronized (mAllDownloadModuleList) {
            for (IDownloadModel model : mAllDownloadModuleList) {
                if (model.equals(downloadModel)) {
                    return model.getDownloadState();
                }
            }
        }
        //查询不到该对象，异常情况
        return DownloadConstants.STATE_COMPLETE;
    }

    private void submitTaskToExecutor(DownloadTask downloadTask){
        onWait(downloadTask.getDownloadModel());
        submitRunnableToExecutor(downloadTask);
    }

    @Override
    public void onWait(@NonNull IDownloadModel downloadModel) {
//        if(mIDBHandler!=null) {
//            mIDBHandler.saveDownloadModel(downloadModel);
//        }
        downloadModel.setDownloadState(DownloadConstants.STATE_WAITING);
        mExecutorDelivery.post(downloadModel,IDownloadTaskCallback.EVENT_WAIT);
    }

    @Override
    public void onStart(@NonNull IDownloadModel downloadModel) {
        if(mIDBHandler!=null) {
            mIDBHandler.saveDownloadModel(downloadModel);
        }
        mExecutorDelivery.post(downloadModel,IDownloadTaskCallback.EVENT_START);
    }

    private long lastProgressChangeTime = System.currentTimeMillis();

    @Override
    public void onDownloadProgress(@NonNull IDownloadModel downloadModel) {
        //防止过于频繁的回调DownloadProgress，导致界面卡顿
        if(System.currentTimeMillis()-lastProgressChangeTime > 1000){
            lastProgressChangeTime = System.currentTimeMillis();
            //TODO查询所有符合条件的IDownloadModel
            List<IDownloadModel> list = new ArrayList<>();
            synchronized (mDownloaddingTaskList) {
                for (DownloadTask downloadTask : mDownloaddingTaskList) {
                    list.add(downloadTask.getDownloadModel());
                }
            }
            onDownloadProgress(list);

        }
    }

    @Override
    public void onDownloadProgress(@NonNull List<IDownloadModel> downloadModelList) {
        mExecutorDelivery.post(downloadModelList, ISingleDownloadTaskCallback.EVENT_PROGRESS);
    }

    @Override
    public void onPause(@NonNull IDownloadModel downloadModel) {
        downloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
        mExecutorDelivery.post(downloadModel,IDownloadTaskCallback.EVENT_PAUSE);
    }

    @Override
    public void onWait(@NonNull List<IDownloadModel> downloadModelList) {
        mExecutorDelivery.post(downloadModelList,ISingleDownloadTaskCallback.EVENT_WAIT);
    }

    @Override
    public void onPause(@NonNull List<IDownloadModel> downloadModelList){
        mExecutorDelivery.post(downloadModelList,IDownloadTaskCallback.EVENT_PAUSE);
    }

    @Override
    public void onDelete(@NonNull List<IDownloadModel> downloadModelList) {
        if(mIDBHandler!=null) {
            mIDBHandler.deleteDownloadModel(downloadModelList);
        }

        mExecutorDelivery.post(downloadModelList,IDownloadTaskCallback.EVENT_DELETE);
        deleteDownloadModelFile(downloadModelList);
    }

    @Override
    public void onComplete(@NonNull IDownloadModel downloadModel) {
        if(mIDBHandler!=null) {
            mIDBHandler.saveDownloadModel(downloadModel);
        }
        mExecutorDelivery.post(downloadModel,IDownloadTaskCallback.EVENT_COMPLETE);
    }

    @Override
    public void onDelete(@NonNull IDownloadModel downloadModel) {
        if(mIDBHandler!=null) {
            mIDBHandler.deleteDownloadModel(downloadModel);
        }
        mExecutorDelivery.post(downloadModel,IDownloadTaskCallback.EVENT_DELETE);
        List<IDownloadModel> delList = new ArrayList<>();
        delList.add(downloadModel);
        deleteDownloadModelFile(delList);
    }

    @Override
    public void onAdd(@NonNull IDownloadModel downloadModel) {
        if(mIDBHandler!=null) {
            mIDBHandler.saveDownloadModel(downloadModel);
        }
        mExecutorDelivery.post(downloadModel,IDownloadTaskCallback.EVENT_ADD);
    }

    @Override
    public void onAdd(@NonNull List<IDownloadModel> downloadModelList) {
        if(mIDBHandler!=null) {
            mIDBHandler.saveDownloadModel(downloadModelList);
        }
        mExecutorDelivery.post(downloadModelList,IDownloadTaskCallback.EVENT_ADD);
    }

    @Override
    public void onError(@NonNull IDownloadModel downloadModel,int errorType,String errorMsg) {
        if(mIDBHandler!=null) {
            mIDBHandler.saveDownloadModel(downloadModel);
        }
        mExecutorDelivery.postError(downloadModel,IDownloadTaskCallback.EVENT_ERROR,errorType,errorMsg);
    }

}
