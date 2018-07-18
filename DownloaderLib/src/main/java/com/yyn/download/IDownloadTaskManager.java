package com.yyn.download;

import com.yyn.download.DownloadConstants;

import java.util.List;

/**
 * Created by yyn
 *
 * @author yyn
 */
public interface IDownloadTaskManager {

    /**
     * 删除所有未下载完成的声音
     */
    void deleteAllIncompletedDownloadModels();

    /**
     * 删除所有下载完成的Models
     */
    void deleteAllDownloadedModels();
    void deleteAll();
    void deleteDownloadModels(List<IDownloadModel> list);
    void deleteDownloadModel(IDownloadModel downloadModel);
    void addDownloadModel(IDownloadModel downloadModel, boolean isAutoDownload);
    void addDownloadModels(List<IDownloadModel> list, boolean isAutoDownload);
    void resumeDownloadModel(IDownloadModel downloadModel);
    void resumeAllDownloadModels();
    void pauseDownloadModel(IDownloadModel downloadModel);
    //    void pauseDownloadModels(List<IDownloadModel> list);
    void pauseAllDownloadModels();
    List<IDownloadModel> getAllInCompletedDownloadedModels();
    List<IDownloadModel> getAllCompletedDownloadedModels();
    List<IDownloadModel> getDownloadingModels();
    List<IDownloadModel> getWaitingModels();
    List<IDownloadModel> getPauseModels();
    @DownloadConstants.DownloadState int getDownloadModelSate(IDownloadModel downloadModel);
}
