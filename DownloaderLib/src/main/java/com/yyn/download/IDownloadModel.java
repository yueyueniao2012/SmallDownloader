package com.yyn.download;

import com.yyn.download.DownloadConstants;

/**
 * Created by yyn
 *
 * @author yyn
 */
public interface IDownloadModel {

    int DEFAULT_PRIORITY = 0;

    public @DownloadConstants.DownloadState int getDownloadState();
    public void setDownloadState(@DownloadConstants.DownloadState int downloadState);

    public String getDownloadUrl();
    public void setDownloadUrl(String downloadUrl);

    public String getSavedFileToSdcardPath();
    public void setSavedFileToSdcardPath(String savedFileToSdcardPath);

    public int getDownloadPriority();
    public void setDownloadPriority(int priority);

    public void setDownloadedSize(long downloadedSize);
    public long getDownloadedSize();

    public void setContentLength(long contentLength);
    public long getContentLength();

}
