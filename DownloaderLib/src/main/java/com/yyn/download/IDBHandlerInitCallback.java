package com.yyn.download;


import java.util.List;

/**
 * Created by yyn
 *
 * @author yyn
 */
public interface IDBHandlerInitCallback {

    void initReady(List<IDownloadModel> list, boolean isAutoDownload);

}
