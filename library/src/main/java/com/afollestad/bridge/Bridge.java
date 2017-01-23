package com.afollestad.bridge;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;

/**
 * @author Aidan Follestad (afollestad)
 */
public class Bridge implements Serializable {

    private static Bridge mBridge;
    private static Config mConfig;

    protected static final Object LOCK = new Object();
    protected HashMap<String, CallbackStack> mRequestMap;

    protected boolean pushCallback(Request request, Callback callback) {
        synchronized (LOCK) {
            if (mRequestMap == null)
                mRequestMap = new HashMap<>();
            final String key = CallbackStack.createKey(request);
            CallbackStack cbs = mRequestMap.get(key);
            if (cbs != null) {
                Log.d(this, "Pushing callback to EXISTING stack for %s", key);
                cbs.push(callback, request);
                return false;
            } else {
                Log.d(this, "Pushing callback to NEW stack for %s", key);
                cbs = new CallbackStack();
                cbs.push(callback, request);
                mRequestMap.put(key, cbs);
                return true;
            }
        }
    }

    protected void fireProgress(Request request, int current, int total) {
        synchronized (LOCK) {
            if (mRequestMap == null) return;
            final String key = CallbackStack.createKey(request);
            final CallbackStack cbs = mRequestMap.get(key);
            if (cbs != null)
                cbs.fireAllProgress(request, current, total);
        }
    }

    protected void fireCallbacks(final Request request, final Response response, final BridgeException error) {
        synchronized (LOCK) {
            final String key = CallbackStack.createKey(request);
            Log.d(this, "Attempting to fire callbacks for %s", key);
            if (mRequestMap == null) {
                Log.d(this, "Request map is null, can't fire callbacks.");
                return;
            }
            final CallbackStack cbs = mRequestMap.get(key);
            if (cbs != null) {
                Log.d(this, "Firing %d callback(s) for %s", cbs.size(), key);
                cbs.fireAll(response, error);
                mRequestMap.remove(key);
                if (mRequestMap.size() == 0)
                    mRequestMap = null;
            } else {
                Log.d(this, "No callback stack found for %s", key);
            }
        }
    }

    private Bridge() {
        mConfig = new Config();
    }

    @NonNull
    private static Bridge client() {
        if (mBridge == null)
            mBridge = new Bridge();
        return mBridge;
    }

    @NonNull
    public static Config config() {
        if (mBridge == null)
            mBridge = new Bridge();
        if (mConfig == null)
            mConfig = new Config();
        return mConfig;
    }

    private static String processUrl(String url, @Nullable Object... formatArgs) {
        if (formatArgs != null && formatArgs.length > 0) {
            for (int i = 0; i < formatArgs.length; i++) {
                if (formatArgs[i] instanceof String) {
                    try {
                        formatArgs[i] = URLEncoder.encode((String) formatArgs[i], "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        // this should never happen
                        throw new RuntimeException(e);
                    }
                }
            }
            return String.format(url, formatArgs);
        } else return url;
    }

    public static RequestBuilder parseJson(@NonNull String json) {
        return new RequestBuilder(json, Method.JSON, client());
    }

    public static RequestBuilder get(@NonNull String url, @Nullable Object... formatArgs) {
        return new RequestBuilder(processUrl(url, formatArgs), Method.GET, client());
    }

    public static RequestBuilder post(@NonNull String url, @Nullable Object... formatArgs) {
        return new RequestBuilder(processUrl(url, formatArgs), Method.POST, client());
    }

    public static RequestBuilder put(@NonNull String url, @Nullable Object... formatArgs) {
        return new RequestBuilder(processUrl(url, formatArgs), Method.PUT, client());
    }

    public static RequestBuilder delete(@NonNull String url, @Nullable Object... formatArgs) {
        return new RequestBuilder(processUrl(url, formatArgs), Method.DELETE, client());
    }

    public static CancelCriteria cancelAll() {
        return new CancelCriteria(client(), LOCK);
    }

    public static void destroy() {
        if (mBridge != null) {
            if (mConfig != null) {
                mConfig.destroy();
                mConfig = null;
            }
            cancelAll().commit();
            Log.d(mBridge, "Bridge singleton was destroyed.");
        }
    }
}