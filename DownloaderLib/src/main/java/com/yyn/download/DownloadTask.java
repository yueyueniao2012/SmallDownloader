package com.yyn.download;

import android.os.StatFs;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.yyn.download.DownloadConstants;

import org.apache.http.conn.ConnectTimeoutException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by yyn
 *
 * @author yyn
 */
public class DownloadTask implements IDownloadTask<DownloadTask> {

    private IDownloadModel mDownloadModel;
    private IDownloadTaskHandler mDownloadTaskHandler;
    private ISingleDownloadTaskCallback mISingleDownloadTaskCallback;
    private boolean mIsCancle;
    private boolean mShallRetryAfterExecute;
    private IDBHandler mDBHandler;
    // socket 超时重试次数 1次 当用户切换网络的时候 如果当前下载的文件没有完成 则会抛出此异常  这里重试一次连接到新的网络继续下载；
    private volatile int mSocketTimeOutRetryCount = 0;

    private String mSaveToSdcardBasePath;
    private int TIMEOUT = 10*1000;

    public boolean shallRetryAfterExecute() {
        return mShallRetryAfterExecute;
    }

    public DownloadTask(IDownloadModel downloadModel, IDownloadTaskHandler downloadTaskHandler, ISingleDownloadTaskCallback singleDownloadTaskCallback,IDBHandler dbHandler) {
        this.mDownloadModel = downloadModel;
        this.mDownloadTaskHandler = downloadTaskHandler;
        mISingleDownloadTaskCallback = singleDownloadTaskCallback;
        mIsCancle = false;
        mShallRetryAfterExecute = false;
        mDBHandler = dbHandler;
    }

    public IDownloadModel getDownloadModel(){
        return mDownloadModel;
    }

    @Override
    public void run() {
//        runForTest();

        if(mDownloadTaskHandler ==null){
            Log.i(DownloadConstants.TAG,"IDownloadTaskHandler 不能为空");
            handleDownloadFailed();
            return;
        }

        if(mDownloadModel==null){
            Log.i(DownloadConstants.TAG,"DownloadModel 不能为空");
            handleDownloadFailed();
            return;
        }

        if(mDBHandler==null){
            Log.i(DownloadConstants.TAG,"mDBHandler 不能为空");
            handleDownloadFailed();
            return;
        }

        if(mIsCancle){
            mDownloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
            mISingleDownloadTaskCallback.onPause(mDownloadModel);
            return;
        }

        if(!mDownloadTaskHandler.isNetWorkAvailable()){
            mDownloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
            mISingleDownloadTaskCallback.onError(mDownloadModel,ISingleDownloadTaskCallback.ERROR_NETWORK,"网络不可用");
            handleDownloadFailed();
            return;
        }

        if(mDownloadModel.getDownloadState()==DownloadConstants.STATE_COMPLETE){
            if(!TextUtils.isEmpty(mDownloadModel.getSavedFileToSdcardPath())){
                File downloadFile = new File(mDownloadModel.getSavedFileToSdcardPath());
                if(downloadFile.exists()){
                    handleDownloadCompleted();
                    return;
                }
            }
        }

        if (mIsCancle) {
            mDownloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
            mISingleDownloadTaskCallback.onPause(mDownloadModel);
            return;
        }

        mDownloadModel.setDownloadState(DownloadConstants.STATE_DOWNLOADING);
        mISingleDownloadTaskCallback.onStart(mDownloadModel);

        mDownloadTaskHandler.updateDownloadModel(mDownloadModel);

        if (mIsCancle) {
            mDownloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
            mISingleDownloadTaskCallback.onPause(mDownloadModel);
            return;
        }

        if(!mDBHandler.saveDownloadModel(mDownloadModel)){
            mDownloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
            mISingleDownloadTaskCallback.onError(mDownloadModel,ISingleDownloadTaskCallback.ERROR_SAVE_TO_DB,"下载数据保存失败");
            handleDownloadFailed();
            return;
        }

        if (mIsCancle) {
            mDownloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
            mISingleDownloadTaskCallback.onPause(mDownloadModel);
            return;
        }

        mSaveToSdcardBasePath = mDownloadTaskHandler.getSaveToSdcardBasePath();

        if(TextUtils.isEmpty(mSaveToSdcardBasePath)){
            mDownloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
            mISingleDownloadTaskCallback.onError(mDownloadModel,ISingleDownloadTaskCallback.ERROR_SDCARD,"获取sdcard存储路径失败");
            handleDownloadFailed();
            return;
        }

        if (mIsCancle) {
            mDownloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
            mISingleDownloadTaskCallback.onPause(mDownloadModel);
            return;
        }

        // 检查是否已经下载了文件，或者下载了部分文件（断点支持）
        if (doSmThingIfExistFile()) {
            return;
        }

        if (mIsCancle) {
            mDownloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
            mISingleDownloadTaskCallback.onPause(mDownloadModel);
            return;
        }

        mDBHandler.saveDownloadModel(mDownloadModel);

        if (mIsCancle) {
            mDownloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
            mISingleDownloadTaskCallback.onPause(mDownloadModel);
            return;
        }

        if (TextUtils.isEmpty(mDownloadModel.getSavedFileToSdcardPath())) {
            mDownloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
            mISingleDownloadTaskCallback.onError(mDownloadModel,ISingleDownloadTaskCallback.ERROR_SDCARD,"获取sdcard存储路径失败");
            handleDownloadFailed();
            return;
        }

        try {

            Map<String, String> headMap = new HashMap<>();
            headMap.put("Range", String.format("bytes=%d-", mStartPos));
            mHttpURLConnection = mDownloadTaskHandler.createConnection(mDownloadModel, headMap);

            if(mHttpURLConnection == null){
                URL url = new URL(mDownloadModel.getDownloadUrl());
                mHttpURLConnection = (HttpURLConnection) url.openConnection();
                if (mHttpURLConnection == null) {
                    mDownloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
                    mISingleDownloadTaskCallback.onError(mDownloadModel, ISingleDownloadTaskCallback.ERROR_NETWORK, "请求连接失败");
                    handleDownloadFailed();
                    return;
                }
                for (Map.Entry<String, String> entry : headMap.entrySet()) {
                    mHttpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
                }
                mHttpURLConnection.setConnectTimeout(TIMEOUT);
                mHttpURLConnection.setReadTimeout(TIMEOUT);
            }else{
                if(mHttpURLConnection.getConnectTimeout()==0){
                    mHttpURLConnection.setConnectTimeout(TIMEOUT);
                }
                if(mHttpURLConnection.getReadTimeout()==0){
                    mHttpURLConnection.setReadTimeout(TIMEOUT);
                }
            }

            String responseContent = null;
            long contentLength = 0;
            int responseCode = mHttpURLConnection.getResponseCode();
            if (responseCode == 200) {
                responseContent = mHttpURLConnection.getHeaderField("Content-Length");
                if (!TextUtils.isEmpty(responseContent)) {
                    contentLength = Long.valueOf(responseContent);
                }
            } else if (responseCode == 206) {
                responseContent = mHttpURLConnection.getHeaderField("Content-Range");
                if (!TextUtils.isEmpty(responseContent)) {
                    contentLength = Long.valueOf(responseContent.substring(responseContent.lastIndexOf("/") + 1));
                }
            } else {
                if (mStartPos > 0) {
                    if (mIsCancle) {
                        mDownloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
                        mISingleDownloadTaskCallback.onPause(mDownloadModel);
                        return;
                    }
                    mHttpURLConnection = mDownloadTaskHandler.createConnection(mDownloadModel, new HashMap<String, String>());
                    if(mHttpURLConnection.getReadTimeout() == 0){
                        mHttpURLConnection.setReadTimeout(TIMEOUT);
                    }
                    if(mHttpURLConnection.getConnectTimeout() == 0){
                        mHttpURLConnection.setConnectTimeout(TIMEOUT);
                    }
                    responseContent = mHttpURLConnection.getHeaderField("Content-Length");
                    if (!TextUtils.isEmpty(responseContent)) {
                        contentLength = Long.valueOf(responseContent);
                    }
                    if (contentLength > 0 && mStartPos >= contentLength) {
                        mDownloadModel.setContentLength(contentLength);
                        mDownloadModel.setDownloadedSize(contentLength);
                        handleDownloadCompleted();
                        mHttpURLConnection.disconnect();
                        // 如果文件已存在,表明下载已完成,但是下载量下载时间下载速度不存在
//                        setCNDData(DownloadCDNManager.DOWNLOAD_SUCCESS, contentLength, 0, 0, 0);
                    } else {
                        mDownloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
                        mISingleDownloadTaskCallback.onError(mDownloadModel, ISingleDownloadTaskCallback.ERROR_NETWORK, "请求连接失败responseCode:"+responseCode);
                        handleDownloadFailed();
                    }
                    return;
                }
            }

            if (mIsCancle) {
                mDownloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
                mISingleDownloadTaskCallback.onPause(mDownloadModel);
                return;
            }

            if (contentLength <= 0) {
                mDownloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
                mISingleDownloadTaskCallback.onError(mDownloadModel, ISingleDownloadTaskCallback.ERROR_NETWORK, "ContentLength<=0");
                handleDownloadFailed();
                return;
            } else {
                mDownloadModel.setContentLength(contentLength);
                if (contentLength > 0 && mStartPos >= contentLength) {
                    handleDownloadCompleted();
                    mHttpURLConnection.disconnect();
                    // 如果文件已存在,表明下载已完成,但是下载量下载时间下载速度不存在
//                    setCNDData(DownloadCDNManager.DOWNLOAD_SUCCESS, 0, 0, 0, 0);
                    return;
                } else {
                    mDBHandler.saveDownloadModel(mDownloadModel);
                }
            }

            if (mIsCancle) {
                mDownloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
                mISingleDownloadTaskCallback.onPause(mDownloadModel);
                return;
            }

            long freeSpace = getAvailableMemorySize(new File(mSaveToSdcardBasePath));

            if (freeSpace <= 0 || mDownloadModel.getContentLength() >= freeSpace) {
                mDownloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
                mISingleDownloadTaskCallback.onError(mDownloadModel, ISingleDownloadTaskCallback.ERROR_SDCARD, "磁盘空间不足");
                handleDownloadFailed();
                return;
            }

            BufferedInputStream mBufferedInputStream = new BufferedInputStream(mHttpURLConnection.getInputStream());
            if(fetchNetData(mBufferedInputStream)) {
                handleDownloadCompleted();
            }
        }catch (MalformedURLException | UnknownHostException | ConnectTimeoutException | FileNotFoundException e) {
            handleException(e);
        } catch (SocketTimeoutException | SocketException e) {
            if (mSocketTimeOutRetryCount < 1 && !mIsCancle) {// 后面两个判断是排除网络切换到用户数据网络或且没有流量包的情况，这里没有直接判断
                mSocketTimeOutRetryCount++;
                this.run();
            } else {
                mSocketTimeOutRetryCount = 0;
                handleException(e);
            }
        } catch (IOException e) {
            handleException(e);
        } catch (Exception e) {
            handleException(e);
        } finally {
            if (null != mHttpURLConnection) {
                mHttpURLConnection.disconnect();
            }
            if (mBufferedInputStream != null) {
                try {
                    mBufferedInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }

    private void handleDownloadFailed() {

        mShallRetryAfterExecute = false;

    }

    private void handleException(Exception e) {
        mDownloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
        mISingleDownloadTaskCallback.onError(mDownloadModel,ISingleDownloadTaskCallback.ERROR_NETWORK,e.toString());
        mShallRetryAfterExecute = false;
        mDBHandler.saveDownloadModel(mDownloadModel);
    }

    private void handleDownloadCompleted() {
        mDownloadModel.setDownloadState(DownloadConstants.STATE_COMPLETE);
        mISingleDownloadTaskCallback.onComplete(mDownloadModel);
        mDownloadTaskHandler.onComplete(mDownloadModel);
        mShallRetryAfterExecute = false;
        mDBHandler.saveDownloadModel(mDownloadModel);
    }

    private BufferedInputStream mBufferedInputStream;
    private HttpURLConnection mHttpURLConnection;

    private boolean fetchNetData(BufferedInputStream bufferedInputStream) throws IOException {
        RandomAccessFile saveFile = null;
        try {
            int received = 0;

            saveFile = new RandomAccessFile(mDownloadModel.getSavedFileToSdcardPath(), "rwd");

            if(saveFile.length()<mStartPos){
                throw new IOException("本地文件异常，请重新下载");
            }

            saveFile.seek(mStartPos);
            int offset;
            byte[] buffer = new byte[1024];

            while (!mIsCancle && (offset = bufferedInputStream.read(buffer, 0, 1024)) != -1) {
                saveFile.write(buffer, 0, offset);
                received += offset;
                mDownloadModel.setDownloadedSize(mStartPos + received);
                mISingleDownloadTaskCallback.onDownloadProgress(mDownloadModel);
            }

            if (mIsCancle) {
                mDownloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
                mISingleDownloadTaskCallback.onPause(mDownloadModel);
                mDBHandler.saveDownloadModel(mDownloadModel);
                return false;
            }

            return true;

        }catch (IOException e){
            throw e;
        }finally {
            if (null != saveFile) {
                try {
                    saveFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private long mStartPos=0;

    /**
     * 检查是否已经下载了文件，或者下载了部分文件 并设置相应的参数（断点支持）,并返回是否已经存在下载完成的文件
     */
    protected boolean doSmThingIfExistFile() {
        if (!TextUtils.isEmpty(mDownloadModel.getSavedFileToSdcardPath())) {
            File file = new File(mDownloadModel.getSavedFileToSdcardPath());
            if (!file.exists()) {
                mDownloadModel.setSavedFileToSdcardPath(mDownloadTaskHandler.generateSaveDownloadFilePath(mSaveToSdcardBasePath,mDownloadModel));
                mDownloadModel.setDownloadedSize(0);
            } else {
                mStartPos = file.length();
                if (mDownloadModel.getContentLength() > 0) {
                    if (mStartPos == mDownloadModel.getContentLength()) {// 已经存在完整的下载文件
                        handleDownloadCompleted();
                        return true;
                    } else if (mStartPos > mDownloadModel.getContentLength()) {
                        file.delete();
                        mStartPos = 0;
                    }
                }else{
                    file.delete();
                    mStartPos = 0;
                }
                mDownloadModel.setDownloadedSize(mStartPos);
            }
        } else {
            mDownloadModel.setSavedFileToSdcardPath(mDownloadTaskHandler.generateSaveDownloadFilePath(mSaveToSdcardBasePath,mDownloadModel));
        }
        return false;
    }

    public long getAvailableMemorySize(File path) {
        try {
            if (null != path && path.exists()) {
                StatFs stat = new StatFs(path.getPath());
                long blockSize = stat.getBlockSize();
                long availableBlocks = stat.getAvailableBlocks();
                return availableBlocks * blockSize;
            } else {
                return -1;
            }
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public int compareTo(@NonNull DownloadTask o) {
        return o.mDownloadModel.getDownloadPriority()-mDownloadModel.getDownloadPriority();
    }

    /**
     * shallRetryAfterExecute值为true，task执行完后会重新加入等待执行任务队列（DownloadTaskManager.mDownloadingQueue）中
     * @param shallRetryAfterExecute
     */
    public void cancleTask(boolean shallRetryAfterExecute){
        mIsCancle = true;
        mShallRetryAfterExecute = shallRetryAfterExecute;
    }

    public void resumeTask(){
        mIsCancle = false;
        mShallRetryAfterExecute = false;
        mSocketTimeOutRetryCount = 0;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof DownloadTask) {
            return mDownloadModel.equals(((DownloadTask)obj).getDownloadModel());
        }else{
            return super.equals(obj);
        }
    }

    @Override
    public int hashCode() {
        return mDownloadModel.hashCode();
    }

    //    private void runForTest(){
//        Log.i(DownloadConstants.TAG,"mDownloadModel start Priority:"+mDownloadModel.getDownloadPriority()
//                +" thread name:"+Thread.currentThread().getName());
//
//        if(mIsCancle){
//            mDownloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
//            mISingleDownloadTaskCallback.onPause(mDownloadModel);
//            return;
//        }
//
//        mDownloadModel.setDownloadState(DownloadConstants.STATE_DOWNLOADING);
//        mISingleDownloadTaskCallback.onStart(mDownloadModel);
//        int i = mDownloadModel.getDownloadProgress();
//        while(i++<200) {
//
//            if(mIsCancle){
//                mDownloadModel.setDownloadState(DownloadConstants.STATE_PAUSE);
//                mISingleDownloadTaskCallback.onPause(mDownloadModel);
//                return;
//            }
//            mDownloadModel.setDownloadProgress(i);
//            mISingleDownloadTaskCallback.onDownloadProgress(mDownloadModel);
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        mISingleDownloadTaskCallback.onComplete(mDownloadModel);
//        mDownloadModel.setDownloadState(DownloadConstants.STATE_COMPLETE);
//        Log.i(DownloadConstants.TAG,"mDownloadModel end priority:"+mDownloadModel.getDownloadPriority()
//                +" mIsCancle:"+mIsCancle+" i:"+i);
//    }

}
