package com.yyn.download;

/**
 * Created by yyn
 *
 * @author yyn
 */
public interface IDownloadService extends IDownloadTaskManager {

//    void setContext(Context context);
    void registeDownloadCallback(IDownloadTaskCallback downloadCallBack);
    void unRegisteDownloadCallback(IDownloadTaskCallback downloadCallBack);
    void registeDBHandler(IDBHandler dBHandler);
    void unRegisteDBHandler();
    void registeDownloadTaskHandler(IDownloadTaskHandler downloadTaskHandler);
    void unRegisteDownloadTaskHandler();

}
