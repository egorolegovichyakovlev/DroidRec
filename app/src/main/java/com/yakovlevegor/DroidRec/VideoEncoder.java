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

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaCodecList;
import android.media.MediaCodecInfo;
import android.view.Surface;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;

import java.io.IOException;
import java.nio.ByteBuffer;

class VideoEncoder implements Encoder {
    private Surface mSurface;

    private static final float BPP = 0.25f;
    private static int width = 1080;
    private static int height = 1920;

    private MediaCodec mEncoder;
    private Callback mCallback;

    private String codecName;

    private MediaCodecInfo.CodecProfileLevel codecProfileLevel;

    private int screenFramerate;

    private int usedBitrate;

    VideoEncoder(int inWidth, int inHeight, int inFramerate, float inQuality, boolean customBitrate, int inBitrate, String inCodec, MediaCodecInfo.CodecProfileLevel inProfileLevel) {
        width = inWidth;
        height = inHeight;
        screenFramerate = inFramerate;
        codecName = inCodec;
        codecProfileLevel = inProfileLevel;
        usedBitrate = (int)(BPP*screenFramerate*width*height*inQuality);
        if (customBitrate == true) {
            usedBitrate = inBitrate;
        }
    }

    public void suspendCodec(int suspend) {
        Bundle params = new Bundle();
        params.putInt(MediaCodec.PARAMETER_KEY_SUSPEND, suspend);
        mEncoder.setParameters(params);
    }

    protected void onEncoderConfigured(MediaCodec encoder) {
        mSurface = encoder.createInputSurface();
    }

    protected MediaFormat createMediaFormat() {
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, usedBitrate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, screenFramerate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        if (codecProfileLevel != null && codecProfileLevel.profile != 0 && codecProfileLevel.level != 0) {
            format.setInteger(MediaFormat.KEY_PROFILE, codecProfileLevel.profile);
            format.setInteger("level", codecProfileLevel.level);
        }
        return format;
    }

    Surface getInputSurface() {
        return mSurface;
    }

    public void release() {
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        if (mEncoder != null) {
            mEncoder.release();
            mEncoder = null;
        }
    }

    static abstract class Callback implements Encoder.Callback {
        void onInputBufferAvailable(VideoEncoder encoder, int index) {}

        void onOutputFormatChanged(VideoEncoder encoder, MediaFormat format) {}

        void onOutputBufferAvailable(VideoEncoder encoder, int index, MediaCodec.BufferInfo info) {}
    }

    public void setCallback(Encoder.Callback callback) {
        if (!(callback instanceof Callback)) {
            throw new IllegalArgumentException();
        }
        this.setCallback((Callback) callback);
    }

    void setCallback(Callback callback) {
        if (this.mEncoder != null) {
            throw new IllegalStateException();
        }
        this.mCallback = callback;
    }

    public void prepare() throws IOException {
        if (Looper.myLooper() == null || Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException();
        }

        if (mEncoder != null) {
            throw new IllegalStateException();
        }

        MediaFormat format = createMediaFormat();

        final MediaCodec encoder = MediaCodec.createByCodecName(codecName);
        try {
            if (this.mCallback != null) {
                encoder.setCallback(mCodecCallback);
            }
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            onEncoderConfigured(encoder);
            encoder.start();
        } catch (MediaCodec.CodecException e) {
            throw e;
        }
        mEncoder = encoder;
    }

    public final ByteBuffer getOutputBuffer(int index) {
        return mEncoder.getOutputBuffer(index);
    }

    public final ByteBuffer getInputBuffer(int index) {
        return mEncoder.getInputBuffer(index);
    }

    public final void queueInputBuffer(int index, int offset, int size, long pstTs, int flags) {
        mEncoder.queueInputBuffer(index, offset, size, pstTs, flags);
    }

    public final void releaseOutputBuffer(int index) {
        mEncoder.releaseOutputBuffer(index, false);
    }

    public void stop() {
        if (mEncoder != null) {
            mEncoder.stop();
        }
    }

    private MediaCodec.Callback mCodecCallback = new MediaCodec.Callback() {
        @Override
        public void onInputBufferAvailable(MediaCodec codec, int index) {
            mCallback.onInputBufferAvailable(VideoEncoder.this, index);
        }

        @Override
        public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
            mCallback.onOutputBufferAvailable(VideoEncoder.this, index, info);
        }

        @Override
        public void onError(MediaCodec codec, MediaCodec.CodecException e) {
            mCallback.onError(VideoEncoder.this, e);
        }

        @Override
        public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
            mCallback.onOutputFormatChanged(VideoEncoder.this, format);
        }
    };

}
