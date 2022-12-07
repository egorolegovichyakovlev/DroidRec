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
import android.os.Looper;

import java.io.IOException;
import java.nio.ByteBuffer;

class AudioEncoder implements Encoder {

    private static MediaCodecInfo selectCodec(String mimeType) {
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
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    protected MediaFormat createMediaFormat() {
        MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 2);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_BIT_RATE, selectCodec(MediaFormat.MIMETYPE_AUDIO_AAC).getCapabilitiesForType(MediaFormat.MIMETYPE_AUDIO_AAC).getAudioCapabilities().getBitrateRange().getUpper());
        return format;
    }

    static abstract class Callback implements Encoder.Callback {
        void onInputBufferAvailable(AudioEncoder encoder, int index) {}
        void onOutputFormatChanged(AudioEncoder encoder, MediaFormat format) {}
        void onOutputBufferAvailable(AudioEncoder encoder, int index, MediaCodec.BufferInfo info) {}
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
        if (Looper.myLooper() == null || Looper.myLooper() == Looper.getMainLooper() || mEncoder != null) {
            throw new IllegalStateException();
        }

        MediaFormat format = createMediaFormat();

        String mimeType = format.getString(MediaFormat.KEY_MIME);
        final MediaCodec encoder = createEncoder(mimeType);

        try {
            if (this.mCallback != null) {
                encoder.setCallback(mCodecCallback);
            }
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder.start();
        } catch (MediaCodec.CodecException e) {
            throw e;
        }

        mEncoder = encoder;
    }

    private MediaCodec createEncoder(String type) throws IOException {
        return MediaCodec.createEncoderByType(type);
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

    public void release() {
        if (mEncoder != null) {
            mEncoder.release();
            mEncoder = null;
        }
    }

    public MediaCodec mEncoder;
    private Callback mCallback;

    private MediaCodec.Callback mCodecCallback = new MediaCodec.Callback() {
        @Override
        public void onInputBufferAvailable(MediaCodec codec, int index) {
            mCallback.onInputBufferAvailable(AudioEncoder.this, index);
        }

        @Override
        public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
            mCallback.onOutputBufferAvailable(AudioEncoder.this, index, info);
        }

        @Override
        public void onError(MediaCodec codec, MediaCodec.CodecException e) {
            mCallback.onError(AudioEncoder.this, e);
        }

        @Override
        public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
            mCallback.onOutputFormatChanged(AudioEncoder.this, format);
        }
    };

}
