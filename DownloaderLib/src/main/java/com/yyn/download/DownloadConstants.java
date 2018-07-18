package com.yyn.download;

import android.support.annotation.IntDef;

/**
 * Created by yyn
 *
 * @author yyn
 */
public class DownloadConstants {

    public static final String TAG = "DownloadTask";

    public static final int STATE_DOWNLOADING = 0;
    public static final int STATE_WAITING = 1;
    public static final int STATE_COMPLETE = 2;
    public static final int STATE_PAUSE = 3;

    // 自定义一个注解MyState
    @IntDef({STATE_DOWNLOADING, STATE_WAITING, STATE_COMPLETE,STATE_PAUSE})
    public @interface  DownloadState {}
}
