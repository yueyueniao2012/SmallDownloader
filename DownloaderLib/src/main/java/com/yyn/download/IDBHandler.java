package com.yyn.download;

import android.support.annotation.NonNull;

import java.util.List;

/**
 * Created by yyn
 *
 * @author yyn
 */
public interface IDBHandler {

    /**
     * 异步查询db数据
     * @param dBHandlerInitCallback
     */
    public void initDataFromDb(IDBHandlerInitCallback dBHandlerInitCallback);
    public boolean saveDownloadModel(@NonNull IDownloadModel model);
    public boolean saveDownloadModel(@NonNull List<IDownloadModel> models);
    public boolean deleteDownloadModel(@NonNull IDownloadModel model);
    public boolean deleteDownloadModel(@NonNull List<IDownloadModel> models);

}
