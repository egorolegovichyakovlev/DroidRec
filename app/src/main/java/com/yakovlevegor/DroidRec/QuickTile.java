/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
*/

package com.yakovlevegor.DroidRec;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.os.Binder;
import android.os.IBinder;
import android.os.Build;
import android.annotation.TargetApi;

@TargetApi(Build.VERSION_CODES.N)
public class QuickTile extends TileService {

    private ScreenRecorder.RecordingTileBinder recordingTileBinder;

    public static String ACTION_CONNECT_TILE = MainActivity.appName+".ACTION_CONNECT_TILE";

    public Tile mainTile;

    public class TileBinder extends Binder {
        void recordingState(boolean active) {
            setTileState(active);
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            recordingTileBinder = (ScreenRecorder.RecordingTileBinder)service;
            recordingTileBinder.setConnectTile(new TileBinder());
            setTileState(recordingTileBinder.isStarted());
        }

        public void onServiceDisconnected(ComponentName className) {
            recordingTileBinder.setDisconnectTile();
            setTileState(false);
        }
    };

    public void setTileState(boolean active) {
        if (active == true) {
            mainTile.setState(Tile.STATE_ACTIVE);
        } else {
            mainTile.setState(Tile.STATE_INACTIVE);
        }

        mainTile.updateTile();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        mainTile = getQsTile();
        setTileState(false);
        Intent serviceIntent = new Intent(QuickTile.this, ScreenRecorder.class);
        serviceIntent.setAction(ACTION_CONNECT_TILE);
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStopListening() {
        super.onStopListening();

        if (recordingTileBinder != null) {
            unbindService(mConnection);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onClick() {
        super.onClick();

        if (recordingTileBinder != null) {
            if (recordingTileBinder.isStarted() == true) {
                recordingTileBinder.stopService();
                return;
            }
        }
        Intent recordingstart = new Intent(getApplicationContext(), MainActivity.class);
        recordingstart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        recordingstart.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        recordingstart.setAction(MainActivity.ACTION_ACTIVITY_START_RECORDING);
        startActivityAndCollapse(recordingstart);
    }
}
