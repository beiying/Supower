package com.beiying.pluginfw;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by beiying on 2016/3/1.
 */
public class DLProxyService extends Service implements DLServiceAttachable{
    @Override
    public void attach(DLServicePlugin remoteService, DLPluginManager pluginManager) {

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
