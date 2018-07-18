package com.yyn.download;


/**
 * Created by yyn
 *
 * @author yyn
 */
public class DownloadModel implements IDownloadModel {

    private volatile int downloadState;
    private volatile int downloadPriority;
    private String downloadUrl;
    private String savedFileToSdcardPath;

    @Override
    public int getDownloadState() {
        return downloadState;
    }

    @Override
    public void setDownloadState(int state) {
        this.downloadState = state;
    }

    @Override
    public String getDownloadUrl() {
        return downloadUrl;
    }

    @Override
    public void setDownloadUrl(String url) {
        downloadUrl = url;
    }

    @Override
    public String getSavedFileToSdcardPath() {
        return savedFileToSdcardPath;
    }

    @Override
    public void setSavedFileToSdcardPath(String path) {
        this.savedFileToSdcardPath = path;
    }

    @Override
    public int getDownloadPriority() {
        return downloadPriority;
    }

    @Override
    public void setDownloadPriority(int priority) {
        this.downloadPriority = priority;
    }

    @Override
    public void setDownloadedSize(long downloadedSize) {

    }

    @Override
    public long getDownloadedSize() {
        return 0;
    }

    @Override
    public void setContentLength(long contentLength) {

    }

    @Override
    public long getContentLength() {
        return 0;
    }
}
