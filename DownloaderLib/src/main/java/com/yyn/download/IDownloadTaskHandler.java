package com.yyn.download;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

/**
 * Created by yyn
 *
 * @author yyn
 */
public interface IDownloadTaskHandler {

    boolean isNetWorkAvailable();

    void updateDownloadModel(IDownloadModel downloadModel);

    String getSaveToSdcardBasePath();

    String generateSaveDownloadFilePath(String basePath,IDownloadModel downloadModel);

    HttpURLConnection createConnection(IDownloadModel mDownloadModel, Map<String, String> headMap) throws IOException;

    void onComplete(IDownloadModel mDownloadModel);
}
