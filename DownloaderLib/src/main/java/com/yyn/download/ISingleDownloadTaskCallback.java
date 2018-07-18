package com.yyn.download;

import android.support.annotation.NonNull;

/**
 * Created by yyn
 *
 * @author yyn
 */
public interface ISingleDownloadTaskCallback {

    int EVENT_WAIT = 0;
    int EVENT_START = 1;
    int EVENT_PROGRESS = 2;
    int EVENT_PAUSE = 3;
    int EVENT_COMPLETE = 4;
    int EVENT_ADD = 5;
    int EVENT_DELETE = 6;
    int EVENT_ERROR = 7;

    int ERROR_NETWORK = 0;
    int ERROR_SAVE_TO_DB = 1;
    int ERROR_SDCARD = 2;

    void onWait(@NonNull IDownloadModel downloadModel);

    void onStart(@NonNull IDownloadModel downloadModel);

    void onDownloadProgress(@NonNull IDownloadModel downloadModel);

    void onPause(@NonNull IDownloadModel downloadModel);

    void onComplete(@NonNull IDownloadModel downloadModel);

    void onDelete(@NonNull IDownloadModel downloadModel);

    void onAdd(@NonNull IDownloadModel downloadModel);

    void onError(@NonNull IDownloadModel downloadModel,int errorType,String errorMsg);

}
