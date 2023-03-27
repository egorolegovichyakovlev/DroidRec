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

import android.hardware.display.VirtualDisplay;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaCodecList;
import android.media.MediaCodecInfo;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.media.AudioFormat;
import android.media.AudioAttributes;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.content.Context;
import android.widget.Toast;
import android.os.Build;
import android.annotation.TargetApi;

import java.io.IOException;
import java.io.FileDescriptor;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;
import java.lang.SecurityException;

import com.yakovlevegor.DroidRec.R;

@TargetApi(Build.VERSION_CODES.Q)
public class PlaybackRecorder {
    private static final String TAG = PlaybackRecorder.class.getSimpleName();
    private static final int INVALID_INDEX = -1;

    private FileDescriptor mDstDesc = null;
    private VideoEncoder mVideoEncoder;
    private AudioPlaybackRecorder mAudioEncoder;

    private MediaFormat mVideoOutputFormat = null, mAudioOutputFormat = null;
    private int mVideoTrackIndex = INVALID_INDEX, mAudioTrackIndex = INVALID_INDEX;
    private MediaMuxer mMuxer;
    private boolean mMuxerStarted = false;

    private AtomicBoolean mForceQuit = new AtomicBoolean(false);
    private AtomicBoolean mIsRunning = new AtomicBoolean(false);
    private AtomicBoolean mIsPaused = new AtomicBoolean(false);

    private VirtualDisplay mVirtualDisplay;

    private HandlerThread mWorker;
    private CallbackHandler mHandler;

    private Callback mCallback;
    private LinkedList<Integer> mPendingVideoEncoderBufferIndices = new LinkedList<>();
    private LinkedList<Integer> mPendingAudioEncoderBufferIndices = new LinkedList<>();
    private LinkedList<MediaCodec.BufferInfo> mPendingAudioEncoderBufferInfos = new LinkedList<>();
    private LinkedList<MediaCodec.BufferInfo> mPendingVideoEncoderBufferInfos = new LinkedList<>();

    private boolean recordMicrophone;

    private boolean recordAudio;

    private MediaProjection audioPlaybackProjection = null;

    private ArrayList<String> codecsList = new ArrayList<String>();

    private ArrayList<String> codecsAudioList = new ArrayList<String>();

    private int codecsTryIndex = 0;

    private int codecsTryFramerate = 30;

    private int nativeFramerate;

    private ArrayList<MediaCodecInfo.CodecProfileLevel> codecProfileLevels = new ArrayList<MediaCodecInfo.CodecProfileLevel>();

    private MediaCodecInfo.CodecProfileLevel currentProfileLevel;

    private boolean tryNormalFPS = true;

    private boolean try60FPS = true;

    private boolean tryNativeFPS = true;

    private int videoWidth;

    private int videoHeight;

    private boolean doRestart = false;

    private float recordQualityScale = 1.0f;

    private boolean useCustomBitrate = false;

    private int recordCustomBitrate;

    private boolean useCustomCodec = false;

    private String customCodec;

    private boolean useCustomAudioCodec = false;

    private String customAudioCodec;

    private Context mainContext;

    private boolean audioOnly;

    private int sampleRateValue;

    private int channelsCountValue;

    private long lastPaused = 0;

    private long lastTimeout = 0;

    private void getAllAudioCodecs() {
        MediaCodecList listCodecs = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo[] allCodecsList = listCodecs.getCodecInfos();
        int numCodecs = allCodecsList.length;
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = allCodecsList[i];

            if (!codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_AAC)) {
                    codecsAudioList.add(codecInfo.getName());
                }
            }
        }
    }

    private void getAllCodecs() {
        MediaCodecList listCodecs = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo[] allCodecsList = listCodecs.getCodecInfos();
        int numCodecs = allCodecsList.length;
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = allCodecsList[i];

            if (!codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_AVC)) {
                    codecsList.add(codecInfo.getName());
                    codecProfileLevels.add(codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC).profileLevels[0]);
                }
            }
        }
    }

    private String getCodec() {
        if (codecsTryIndex < codecsList.size()) {
            if (tryNativeFPS == true) {
                codecsTryFramerate = nativeFramerate;
            } else if (try60FPS == true) {
                codecsTryFramerate = 60;
            } else if (tryNormalFPS == true) {
                codecsTryFramerate = 30;
            }
        } else {
            if (tryNativeFPS == true) {
                tryNativeFPS = false;
            } else if (try60FPS == true) {
                try60FPS = false;
            }
            codecsTryIndex = 0;
        }

        String codecReturn = codecsList.get(codecsTryIndex);
        currentProfileLevel = codecProfileLevels.get(codecsTryIndex);
        codecsTryIndex += 1;
        return codecReturn;
    }

    public PlaybackRecorder(Context appContext, boolean recordAudioOnly, VirtualDisplay display, FileDescriptor dstDesc, MediaProjection projection, int width, int height, int framerate, boolean microphone, boolean audio, boolean customQuality, float qualityScale, boolean customFramerate, int framerateValue, boolean customBitrate, int bitrateValue, boolean setCustomCodec, String codecName, boolean setCustomAudioCodec, String codecAudioName, int sampleRate, int channelsCount) {
        sampleRateValue = sampleRate;
        channelsCountValue = channelsCount;
        mainContext = appContext;
        audioOnly = recordAudioOnly;
        nativeFramerate = framerate;
        getAllCodecs();
        getAllAudioCodecs();
        mVirtualDisplay = display;
        mDstDesc = dstDesc;
        videoWidth = width;
        videoHeight = height;
        recordMicrophone = microphone;
        recordAudio = audio;

        if (recordAudio == true) {
            int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
            if (channelsCount == 1) {
                channelConfig = AudioFormat.CHANNEL_IN_MONO;
            }
            audioPlaybackProjection = projection;
        }

        if (customQuality == true) {
            recordQualityScale = qualityScale;
        }

        if (customFramerate == true) {
            nativeFramerate = framerateValue;
        }

        if (customBitrate == true) {
            useCustomBitrate = true;
            recordCustomBitrate = bitrateValue;
        }

        if (setCustomCodec == true && codecsList.contains(codecName)) {
            useCustomCodec = setCustomCodec;
            customCodec = codecName;
        }

        if (setCustomAudioCodec == true && codecsAudioList.contains(codecAudioName)) {
            useCustomAudioCodec = setCustomAudioCodec;
            customAudioCodec = codecAudioName;
        }

        if (nativeFramerate <= 60) {
            try60FPS = false;
        }

    }

    public final void pause() {
        mIsPaused.set(true);
        lastPaused = System.currentTimeMillis() * 1000;
        if (mVideoEncoder != null) {
            mVideoEncoder.suspendCodec(1);
        }
    }

    public final void resume() {
        lastTimeout += (System.currentTimeMillis() * 1000) - lastPaused;
        if (mVideoEncoder != null) {
            mVideoEncoder.suspendCodec(0);
        }
        mIsPaused.set(false);
    }

    public final void quit() {
        mForceQuit.set(true);
        if (!mIsRunning.get()) {
            release();
        } else {
            signalStop(false);
        }
    }

    public final void restart() {
        if (codecsTryIndex >= codecsList.size() && tryNormalFPS == true) {
            Toast.makeText(mainContext, R.string.error_recorder_codec_error, Toast.LENGTH_SHORT).show();
            quit();
        } else {
            doRestart = true;

            release();

            start();

            doRestart = false;
        }
    }

    public void start() {
        lastPaused = 0;
        lastTimeout = 0;
        if (audioOnly == false) {
            String codecInName = getCodec();
            if (useCustomCodec == true) {
                codecInName = customCodec;
                currentProfileLevel = codecProfileLevels.get(codecsList.lastIndexOf(customCodec));
            }
            mVideoEncoder = new VideoEncoder(videoWidth, videoHeight, nativeFramerate, recordQualityScale, useCustomBitrate, recordCustomBitrate, codecInName, currentProfileLevel);
        } else {
            mVideoEncoder = null;
        }

        if (recordMicrophone == false && recordAudio == false) {
            mAudioEncoder = null;
        } else {
            mAudioEncoder = new AudioPlaybackRecorder(recordMicrophone, recordAudio, sampleRateValue, channelsCountValue, audioPlaybackProjection, useCustomAudioCodec, customAudioCodec, mainContext);
        }

        if (mWorker != null && doRestart == false) {
            throw new IllegalStateException();
        }
        mWorker = new HandlerThread(TAG);
        mWorker.start();
        mHandler = new CallbackHandler(mWorker.getLooper());
        mHandler.sendEmptyMessage(MSG_START);
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    interface Callback {
        void onStop(Throwable error);

        void onStart();

        void onRecording(long presentationTimeUs);
    }

    private static final int MSG_START = 0;
    private static final int MSG_STOP = 1;
    private static final int MSG_ERROR = 2;
    private static final int STOP_WITH_EOS = 1;

    private class CallbackHandler extends Handler {
        CallbackHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_START) {
                try {
                    record();
                    if (mCallback != null) {
                        mCallback.onStart();
                    }
                } catch (Exception e) {
                    msg.obj = e;
                }
            } else if (msg.what == MSG_STOP || msg.what == MSG_ERROR) {
                stopEncoders();

                if (msg.arg1 != STOP_WITH_EOS) signalEndOfStream();
                if (mCallback != null) {
                    mCallback.onStop((Throwable) msg.obj);
                }
                if (mForceQuit.get() == true || useCustomCodec) {
                    release();
                } else {
                    restart();
                }
            }
        }
    }

    private void signalEndOfStream() {
        MediaCodec.BufferInfo eos = new MediaCodec.BufferInfo();
        ByteBuffer buffer = ByteBuffer.allocate(0);
        eos.set(0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        if (audioOnly == false) {
            if (mVideoTrackIndex != INVALID_INDEX) {
                writeSampleData(mVideoTrackIndex, eos, buffer);
            }
        }
        if (mAudioTrackIndex != INVALID_INDEX) {
            writeSampleData(mAudioTrackIndex, eos, buffer);
        }
        mVideoTrackIndex = INVALID_INDEX;
        mAudioTrackIndex = INVALID_INDEX;
    }

    private void record() {
        if (mIsRunning.get() || mForceQuit.get() || (mVirtualDisplay == null && audioOnly == false)) {
            throw new IllegalStateException();
        }

        mIsRunning.set(true);

        try {
            mMuxer = new MediaMuxer(mDstDesc, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            if (audioOnly == false) {
                prepareVideoEncoder();
            }
            prepareAudioEncoder();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (audioOnly == false) {
            mVirtualDisplay.setSurface(mVideoEncoder.getInputSurface());
        }
    }

    private void muxVideo(int index, MediaCodec.BufferInfo buffer) {
        if (!mIsRunning.get()) {
            return;
        }
        if (!mMuxerStarted || mVideoTrackIndex == INVALID_INDEX) {
            mPendingVideoEncoderBufferIndices.add(index);
            mPendingVideoEncoderBufferInfos.add(buffer);
            return;
        }

        if (mIsPaused.get() == false) {
            ByteBuffer encodedData = mVideoEncoder.getOutputBuffer(index);
            buffer.presentationTimeUs -= lastTimeout;
            writeSampleData(mVideoTrackIndex, buffer, encodedData);
        }

        mVideoEncoder.releaseOutputBuffer(index);

        if ((buffer.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            mVideoTrackIndex = INVALID_INDEX;
            signalStop(true);
        }
    }


    private void muxAudio(int index, MediaCodec.BufferInfo buffer) {
        if (!mIsRunning.get()) {
            return;
        }

        if (!mMuxerStarted || mAudioTrackIndex == INVALID_INDEX) {
            mPendingAudioEncoderBufferIndices.add(index);
            mPendingAudioEncoderBufferInfos.add(buffer);
            return;
        }

        if (mIsPaused.get() == false) {
            ByteBuffer encodedData = mAudioEncoder.getOutputBuffer(index);
            buffer.presentationTimeUs -= lastTimeout;
            writeSampleData(mAudioTrackIndex, buffer, encodedData);
        }

        mAudioEncoder.releaseOutputBuffer(index);

        if ((buffer.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            mAudioTrackIndex = INVALID_INDEX;
            signalStop(true);
        }
    }

    private void writeSampleData(int track, MediaCodec.BufferInfo buffer, ByteBuffer encodedData) {
        if ((buffer.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            buffer.size = 0;
        }
        boolean eos = (buffer.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
        if (buffer.size == 0 && !eos) {
            encodedData = null;
        } else {
            if (buffer.presentationTimeUs != 0) {
                if (track == mVideoTrackIndex) {
                    resetVideoPts(buffer);
                } else if (track == mAudioTrackIndex) {
                    resetAudioPts(buffer);
                }
            }
            if (!eos && mCallback != null) {
                mCallback.onRecording(buffer.presentationTimeUs);
            }
        }
        if (encodedData != null) {
            encodedData.position(buffer.offset);
            encodedData.limit(buffer.offset + buffer.size);
            mMuxer.writeSampleData(track, encodedData, buffer);
        }
    }

    private long mVideoPtsOffset, mAudioPtsOffset;

    private void resetAudioPts(MediaCodec.BufferInfo buffer) {
        if (mAudioPtsOffset == 0) {
            mAudioPtsOffset = buffer.presentationTimeUs;
            buffer.presentationTimeUs = 0;
        } else {
            buffer.presentationTimeUs -= mAudioPtsOffset;
        }
    }

    private void resetVideoPts(MediaCodec.BufferInfo buffer) {
        if (mVideoPtsOffset == 0) {
            mVideoPtsOffset = buffer.presentationTimeUs;
            buffer.presentationTimeUs = 0;
        } else {
            buffer.presentationTimeUs -= mVideoPtsOffset;
        }
    }

    private void resetVideoOutputFormat(MediaFormat newFormat) {
        if (mVideoTrackIndex >= 0 || mMuxerStarted) {
            throw new IllegalStateException();
        }
        mVideoOutputFormat = newFormat;
    }

    private void resetAudioOutputFormat(MediaFormat newFormat) {
        if (mAudioTrackIndex >= 0 || mMuxerStarted) {
            throw new IllegalStateException();
        }
        mAudioOutputFormat = newFormat;
    }

    private void startMuxerIfReady() {
        if (mMuxerStarted || (mVideoOutputFormat == null && audioOnly == false) || (mAudioEncoder != null && mAudioOutputFormat == null)) {
            return;
        }

        if (audioOnly == false) {
            mVideoTrackIndex = mMuxer.addTrack(mVideoOutputFormat);
        }
        mAudioTrackIndex = mAudioEncoder == null ? INVALID_INDEX : mMuxer.addTrack(mAudioOutputFormat);
        mMuxer.start();
        mMuxerStarted = true;
        if (mPendingVideoEncoderBufferIndices.isEmpty() && mPendingAudioEncoderBufferIndices.isEmpty()) {
            return;
        }
        MediaCodec.BufferInfo info;
        if (audioOnly == false) {
            while ((info = mPendingVideoEncoderBufferInfos.poll()) != null) {
                int index = mPendingVideoEncoderBufferIndices.poll();
                muxVideo(index, info);
            }
        }
        if (mAudioEncoder != null) {
            while ((info = mPendingAudioEncoderBufferInfos.poll()) != null) {
                int index = mPendingAudioEncoderBufferIndices.poll();
                muxAudio(index, info);
            }
        }
    }

    private void prepareVideoEncoder() throws IOException {
        VideoEncoder.Callback callback = new VideoEncoder.Callback() {

            @Override
            public void onOutputBufferAvailable(VideoEncoder codec, int index, MediaCodec.BufferInfo info) {
                try {
                    muxVideo(index, info);
                } catch (Exception e) {
                    Message.obtain(mHandler, MSG_ERROR, e).sendToTarget();
                }
            }

            @Override
            public void onError(Encoder codec, Exception e) {
                Message.obtain(mHandler, MSG_ERROR, e).sendToTarget();
            }

            @Override
            public void onOutputFormatChanged(VideoEncoder codec, MediaFormat format) {
                resetVideoOutputFormat(format);
                startMuxerIfReady();
            }
        };
        mVideoEncoder.setCallback(callback);
        mVideoEncoder.prepare();
    }

    private void prepareAudioEncoder() throws IOException {
        final AudioPlaybackRecorder micRecorder = mAudioEncoder;
        if (micRecorder == null) return;
        AudioEncoder.Callback callback = new AudioEncoder.Callback() {

            @Override
            public void onOutputBufferAvailable(AudioEncoder codec, int index, MediaCodec.BufferInfo info) {
                try {
                    muxAudio(index, info);
                } catch (Exception e) {
                    Message.obtain(mHandler, MSG_ERROR, e).sendToTarget();
                }
            }

            @Override
            public void onOutputFormatChanged(AudioEncoder codec, MediaFormat format) {
                resetAudioOutputFormat(format);
                startMuxerIfReady();
            }

            @Override
            public void onError(Encoder codec, Exception e) {
                Message.obtain(mHandler, MSG_ERROR, e).sendToTarget();
            }


        };
        micRecorder.setCallback(callback);
        micRecorder.prepare();
    }

    private void signalStop(boolean stopWithEOS) {
        Message msg = Message.obtain(mHandler, MSG_STOP, stopWithEOS ? STOP_WITH_EOS : 0, 0);
        mHandler.sendMessageAtFrontOfQueue(msg);
    }

    private void stopEncoders() {
        mIsRunning.set(false);
        mPendingAudioEncoderBufferInfos.clear();
        mPendingAudioEncoderBufferIndices.clear();
        mPendingVideoEncoderBufferInfos.clear();
        mPendingVideoEncoderBufferIndices.clear();
        try {
            if (mVideoEncoder != null) {
                mVideoEncoder.stop();
            }
        } catch (IllegalStateException e) {}
        try {
            if (mAudioEncoder != null) {
                mAudioEncoder.stop();
            }
        } catch (IllegalStateException e) {}

    }

    private void release() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay.setSurface(null);
            if (doRestart == false) {
                mVirtualDisplay.release();
                mVirtualDisplay = null;
            }
        }

        mVideoOutputFormat = mAudioOutputFormat = null;
        mVideoTrackIndex = mAudioTrackIndex = INVALID_INDEX;
        mMuxerStarted = false;

        if (mWorker != null) {
            mWorker.quitSafely();
            mWorker = null;
        }
        if (mVideoEncoder != null) {
            mVideoEncoder.release();
            mVideoEncoder = null;
        }
        if (mAudioEncoder != null) {
            mAudioEncoder.release();
            mAudioEncoder = null;
        }

        if (mMuxer != null) {
            try {
                mMuxer.stop();
                mMuxer.release();
            } catch (Exception e) {
            }
            mMuxer = null;
        }
        mHandler = null;
    }

    @Override
    protected void finalize() throws Throwable {
        if (mVirtualDisplay != null) {
            release();
        }
    }

}
