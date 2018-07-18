package com.yyn.download;

import android.support.annotation.NonNull;

import java.util.List;


/**
 * Created by yyn
 *
 * @author yyn
 */
public interface IDownloadTaskCallback extends ISingleDownloadTaskCallback{

    void onWait(@NonNull List<IDownloadModel> downloadModelList);

    void onDownloadProgress(@NonNull List<IDownloadModel> downloadModelList);

    void onPause(@NonNull List<IDownloadModel> downloadModelList);

    void onDelete(@NonNull List<IDownloadModel> downloadModelList);

    void onAdd(@NonNull List<IDownloadModel> downloadModelList);

}
