package com.yyn.download;

import android.os.Handler;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Created by yyn
 *
 * @author yyn
 */
public class ExecutorDelivery {

    private final Executor mResponsePoster;
    private List<IDownloadTaskCallback> mDownloadCallBackList;

    public ExecutorDelivery(final Handler handler) {
        // Make an Executor that just wraps the handler.
        mResponsePoster = new Executor() {
            @Override
            public void execute(Runnable command) {
                handler.post(command);
            }
        };
    }

    public void setDownloadCallBackList(List<IDownloadTaskCallback> downloadCallBackList){
        mDownloadCallBackList = downloadCallBackList;
    }

    public void post(List<IDownloadModel> tasks, int event) {
        DownloadTaskCallbackRunnable runnable = new DownloadTaskCallbackRunnable(tasks, event);
        mResponsePoster.execute(runnable);
    }

    public void post(IDownloadModel task, int event) {
        DownloadTaskCallbackRunnable runnable = new DownloadTaskCallbackRunnable(task, event);
        mResponsePoster.execute(runnable);
    }

    public void postError(IDownloadModel task, int event,int errorType,String msg) {
        DownloadTaskCallbackRunnable runnable = new DownloadTaskCallbackRunnable(task, event,errorType,msg);
        mResponsePoster.execute(runnable);
    }


    class DownloadTaskCallbackRunnable implements Runnable {
        private List<IDownloadModel> mTasks;
        private IDownloadModel mTaskSingle;
        private int mEvent;
        private int mErrorType;
        private String mErrorMsg;

        public DownloadTaskCallbackRunnable(List<IDownloadModel> tasks, int event) {
            this.mTasks = tasks;
            this.mEvent = event;
        }

        public DownloadTaskCallbackRunnable(IDownloadModel taskSingle, int event) {
            this.mTaskSingle = taskSingle;
            this.mEvent = event;
        }

        public DownloadTaskCallbackRunnable(IDownloadModel taskSingle, int event,int errorType,String msg) {
            this.mTaskSingle = taskSingle;
            this.mEvent = event;
            this.mErrorType = errorType;
            this.mErrorMsg = msg;
        }

        @Override
        public void run() {
            if(mDownloadCallBackList==null||mDownloadCallBackList.size()==0){
                return;
            }
            switch (mEvent) {
                case IDownloadTaskCallback.EVENT_WAIT:

                    for(IDownloadTaskCallback downloadTaskCallback:mDownloadCallBackList) {
                        if(mTaskSingle!=null) {
                            downloadTaskCallback.onWait(mTaskSingle);
                        }else if(mTasks!=null){
                            downloadTaskCallback.onWait(mTasks);
                        }
                    }

                    break;
                case IDownloadTaskCallback.EVENT_START:
                    if(mTaskSingle!=null) {
                        for(IDownloadTaskCallback downloadTaskCallback:mDownloadCallBackList) {
                            downloadTaskCallback.onStart(mTaskSingle);
                        }
                    }
                    break;
                case IDownloadTaskCallback.EVENT_PROGRESS:
                    for(IDownloadTaskCallback downloadTaskCallback:mDownloadCallBackList) {
                        if(mTasks!=null) {
                            downloadTaskCallback.onDownloadProgress(mTasks);
                        }
                    }
                    break;
                case IDownloadTaskCallback.EVENT_PAUSE:

                    for(IDownloadTaskCallback downloadTaskCallback:mDownloadCallBackList) {
                        if(mTaskSingle!=null) {
                            downloadTaskCallback.onPause(mTaskSingle);
                        }else if(mTasks!=null){
                            downloadTaskCallback.onPause(mTasks);
                        }
                    }

                    break;
                case IDownloadTaskCallback.EVENT_COMPLETE:
                    for(IDownloadTaskCallback downloadTaskCallback:mDownloadCallBackList) {
                        if(mTaskSingle!=null) {
                            downloadTaskCallback.onComplete(mTaskSingle);
                        }
                    }
                    break;
                case IDownloadTaskCallback.EVENT_DELETE:
                    for(IDownloadTaskCallback downloadTaskCallback:mDownloadCallBackList) {
                        if(mTaskSingle!=null) {
                            downloadTaskCallback.onDelete(mTaskSingle);
                        }else{
                            downloadTaskCallback.onDelete(mTasks);
                        }
                    }

                    break;
                case IDownloadTaskCallback.EVENT_ADD:
                    for(IDownloadTaskCallback downloadTaskCallback:mDownloadCallBackList) {
                        if(mTaskSingle!=null) {
                            downloadTaskCallback.onAdd(mTaskSingle);
                        }else{
                            downloadTaskCallback.onAdd(mTasks);
                        }
                    }

                    break;
                case IDownloadTaskCallback.EVENT_ERROR:
                    for(IDownloadTaskCallback downloadTaskCallback:mDownloadCallBackList) {
                        if(mTaskSingle!=null) {
                            downloadTaskCallback.onError(mTaskSingle,mErrorType,mErrorMsg);
                        }
                    }
                    break;
            }
        }
    }

}
