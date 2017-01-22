package com.afollestad.bridge;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.Spanned;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class Response implements AsResults, Serializable {

    private final String mUrl;
    private final byte[] mData;
    private int mCode = -1;
    private String mMessage;
    private transient Bitmap mBitmapCache;
    private transient JSONObject mJsonObjCache;
    private transient JSONArray mJsonArrayCache;
    private HashMap<String, List<String>> mHeaders;
    private boolean mDidRedirect;

    protected Response(byte[] data, String url, int code, String message, HashMap<String, List<String>> headers, boolean didRedirect) throws IOException {
        mData = data;
        mUrl = url;
        mCode = code;
        mMessage = message;
        mHeaders = headers;
        mDidRedirect = didRedirect;
    }

    public boolean didRedirect() {
        return mDidRedirect;
    }

    public String url() {
        return mUrl;
    }

    public int code() {
        return mCode;
    }

    public String phrase() {
        return mMessage;
    }

    @Nullable
    public String header(String name) {
        List<String> header = mHeaders.get(name);
        if (header == null || header.isEmpty())
            return null;
        return header.get(0);
    }

    public boolean headerEquals(String name, String value) {
        String headerVal = header(name);
        return headerVal == null && value == null ||
                (headerVal != null && headerVal.equals(value));
    }

    public Map<String, List<String>> headers() {
        return mHeaders;
    }

    @Nullable
    public List<String> headerList(String name) {
        return mHeaders.get(name);
    }

    public int contentLength() {
        String contentLength = header("Content-Length");
        if (contentLength == null) contentLength = header("content-length");
        if (contentLength == null) return -1;
        return Integer.parseInt(contentLength);
    }

    @Nullable
    public String contentType() {
        String contentType = header("Content-Type");
        if (contentType == null) contentType = header("content-type");
        return contentType;
    }

    public boolean isSuccess() {
        //noinspection PointlessBooleanExpression
        boolean success = mCode == -1 || mCode >= 200 && mCode <= 303;
        if (!success)
            Log2.d(this, "HTTP status %d was considered unsuccessful.", mCode);
        return success;
    }

    @Nullable
    public byte[] asBytes() {
        return mData;
    }

    @Nullable
    public String asString() {
        try {
            final byte[] bytes = asBytes();
            if (bytes == null || bytes.length == 0) return null;
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Should never happen
            throw new RuntimeException(e);
        }
    }

    @Nullable
    @Override
    public Spanned asHtml() {
        final String content = asString();
        if (content == null)
            return null;
        return Html.fromHtml(content);
    }

    @Nullable
    @Override
    public Bitmap asBitmap() {
        if (mBitmapCache == null) {
            final InputStream is = new ByteArrayInputStream(asBytes());
            mBitmapCache = BitmapFactory.decodeStream(is);
            BridgeUtil.closeQuietly(is);
        }
        return mBitmapCache;
    }

    @Nullable
    public JSONObject asJsonObject() throws BridgeException {
        final String content = asString();
        if (content == null) return null;
        try {
            if (mJsonObjCache == null)
                mJsonObjCache = new JSONObject(content);
            return mJsonObjCache;
        } catch (JSONException e) {
            throw new BridgeException(this, e, BridgeException.REASON_RESPONSE_UNPARSEABLE);
        }
    }

    @Nullable
    public JSONArray asJsonArray() throws BridgeException {
        final String content = asString();
        if (content == null) return null;
        try {
            if (mJsonArrayCache == null)
                mJsonArrayCache = new JSONArray(content);
            return mJsonArrayCache;
        } catch (JSONException e) {
            throw new BridgeException(this, e, BridgeException.REASON_RESPONSE_UNPARSEABLE);
        }
    }

    public void asFile(@NonNull File destination) throws BridgeException {
        final byte[] content = asBytes();
        if (content == null)
            throw new BridgeException(this, "No content was returned in this response.", BridgeException.REASON_RESPONSE_UNPARSEABLE);
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(destination);
            os.write(content);
            os.flush();
        } catch (IOException e) {
            throw new BridgeException(this, e, BridgeException.REASON_RESPONSE_IOERROR);
        } finally {
            BridgeUtil.closeQuietly(os);
        }
    }

    @Nullable
    public <T> T asClass(@NonNull Class<T> cls) throws BridgeException {
        String contentType = contentType();
        if (contentType == null) {
            String msg = String.format(Locale.getDefault(),
                    "Response has no Content-Type, cannot determine appropriate response converter. Response status: %d.", mCode);
            for (String key : mHeaders.keySet())
                msg += String.format(Locale.getDefault(), "\n    %s = %s", key, mHeaders.get(key).get(0));
            throw new BridgeException(this, msg, BridgeException.REASON_RESPONSE_UNPARSEABLE);
        }
        final long start = System.currentTimeMillis();
        T result = Bridge.config()
                .responseConverter(contentType)
                .convertObject(this, cls);
        final long diff = System.currentTimeMillis() - start;
        Log.d(this, "Response conversion to class %s took %d milliseconds (%d seconds).",
                cls.getName(), diff, diff / 1000);
        return result;
    }

    @Nullable
    public <T> T[] asClassArray(@NonNull Class<T> cls) throws BridgeException {
        String contentType = contentType();
        if (contentType == null) {
            String msg = String.format(Locale.getDefault(),
                    "Response has no Content-Type, cannot determine appropriate response converter. Response status: %d.", mCode);
            for (String key : mHeaders.keySet())
                msg += String.format(Locale.getDefault(), "\n    %s = %s", key, mHeaders.get(key).get(0));
            throw new BridgeException(this, msg, BridgeException.REASON_RESPONSE_UNPARSEABLE);
        }
        final long start = System.currentTimeMillis();
        T[] result = Bridge.config()
                .responseConverter(contentType)
                .convertArray(this, cls);
        final long diff = System.currentTimeMillis() - start;
        Log.d(this, "Response conversion to array of class %s took %d milliseconds (%d seconds).",
                cls.getName(), diff, diff / 1000);
        return result;
    }

    @Nullable
    @Override
    public <T> List<T> asClassList(@NonNull Class<T> cls) throws BridgeException {
        T[] array = asClassArray(cls);
        return array == null ? null : Arrays.asList(array);
    }

    @Nullable
    @Override
    public Object asSuggested() throws BridgeException {
        final String contentType = contentType();
        if (contentType == null) {
            return asBytes();
        } else if (contentType.startsWith("text/html")) {
            return asHtml();
        } else if (contentType.startsWith("text/")) {
            return asString();
        } else if (contentType.startsWith("application/json")) {
            try {
                final String contentStr = asString();
                if (contentStr == null) return asBytes();
                if (contentStr.startsWith("["))
                    return new JSONArray(contentStr);
                else return new JSONObject(contentStr);
            } catch (JSONException e) {
                throw new BridgeException(this, e, BridgeException.REASON_RESPONSE_UNPARSEABLE, true);
            }
        } else if (contentType.startsWith("image/")) {
            return asBitmap();
        }
        return asBytes();
    }

    @Nullable
    @Override
    public String toString() {
        Object suggested;
        try {
            suggested = asSuggested();
        } catch (BridgeException e) {
            suggested = asBytes();
        }
        String bodyDescriptor = suggested instanceof byte[] ?
                String.format("%d bytes", ((byte[]) suggested).length) :
                suggested != null ? suggested.toString() : "(null)";
        return String.format("%s, %d %s, %s", mUrl, mCode, mMessage, bodyDescriptor);
    }
}