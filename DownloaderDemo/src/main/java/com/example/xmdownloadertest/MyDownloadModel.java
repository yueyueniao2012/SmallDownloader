package com.example.xmdownloadertest;


import com.yyn.download.IDownloadModel;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class MyDownloadModel implements IDownloadModel {

    @Id
    long id;

    private volatile int downloadState;
    private volatile int downloadPriority;
    private long contentLength;
    private long downloadedSize;
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

    public int getDownloadProgress() {
        return (int)(getDownloadedSize()*1.0/getContentLength()*100);
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
        this.downloadedSize = downloadedSize;
    }

    @Override
    public long getDownloadedSize() {
        return downloadedSize;
    }

    @Override
    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    @Override
    public long getContentLength() {
        return contentLength;
    }
}
