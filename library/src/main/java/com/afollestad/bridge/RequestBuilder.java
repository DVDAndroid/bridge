package com.afollestad.bridge;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.text.Spanned;

import com.afollestad.bridge.conversion.base.RequestConverter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Aidan Follestad (afollestad)
 */
public final class RequestBuilder implements AsResultsExceptions, Serializable {

    protected final Bridge mContext;
    protected String mUrl;
    @Request.MethodInt
    protected final int mMethod;
    protected HashMap<String, Object> mHeaders;
    protected byte[] mBody;
    protected Pipe mPipe;
    protected int mConnectTimeout;
    protected int mReadTimeout;
    protected int mBufferSize;
    private Request mRequest;
    protected boolean mCancellable = true;
    protected Object mTag;
    protected boolean mThrowIfNotSuccess = false;
    protected ResponseValidator[] mValidators;
    protected ProgressCallback mUploadProgress;
    protected InfoCallback mInfoCallback;
    protected LineCallback mLineCallback;
    protected boolean mDidRedirect = false;

    protected void prepareRedirect(String url) {
        mUrl = url;
        mDidRedirect = true;
    }

    protected RequestBuilder(String url, @Request.MethodInt int method, Bridge context) {
        mContext = context;
        final Config cf = Bridge.config();
        if (!url.startsWith("http") && cf.mHost != null)
            url = cf.mHost + url;

        Log.d(this, "%s %s", Method.name(method), url);
        mUrl = url;
        mMethod = method;

        mHeaders = cf.mDefaultHeaders;
        mConnectTimeout = cf.mConnectTimeout;
        mReadTimeout = cf.mReadTimeout;
        mBufferSize = cf.mBufferSize;
        mValidators = cf.mValidators;
    }

    public RequestBuilder header(@NonNull String name, @NonNull Object value) {
        mHeaders.put(name, value);
        return this;
    }

    public RequestBuilder headers(@NonNull Map<String, ? extends Object> headers) {
        mHeaders.putAll(headers);
        return this;
    }

    public RequestBuilder contentType(@NonNull String contentType) {
        return header("Content-Type", contentType);
    }

    public RequestBuilder connectTimeout(int timeout) {
        if (timeout <= 0)
            throw new IllegalArgumentException("Connect timeout must be greater than 0.");
        mConnectTimeout = timeout;
        return this;
    }

    public RequestBuilder readTimeout(int timeout) {
        if (timeout <= 0)
            throw new IllegalArgumentException("Read timeout must be greater than 0.");
        mReadTimeout = timeout;
        return this;
    }

    public RequestBuilder bufferSize(int size) {
        if (size <= 0)
            throw new IllegalArgumentException("Buffer size must be greater than 0.");
        mBufferSize = size;
        return this;
    }

    public RequestBuilder validators(ResponseValidator... validators) {
        mValidators = validators;
        return this;
    }

    public RequestBuilder body(@Nullable byte[] rawBody) {
        if (rawBody == null) {
            mBody = null;
            return this;
        }
        mBody = rawBody;
        return this;
    }

    public RequestBuilder body(@Nullable String textBody) {
        Log.d(this, "Body: %s", textBody);
        if (textBody == null) {
            mBody = null;
            return this;
        }
        contentType("text/plain");
        try {
            mBody = textBody.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Should never happen
            throw new RuntimeException(e);
        }
        return this;
    }

    public RequestBuilder body(@Nullable JSONObject json) {
        if (json == null) {
            mBody = null;
            return this;
        }
        body(json.toString());
        contentType("application/json");
        return this;
    }

    public RequestBuilder body(@Nullable JSONArray json) {
        if (json == null) {
            mBody = null;
            return this;
        }
        body(json.toString());
        contentType("application/json");
        return this;
    }

    public RequestBuilder body(@Nullable Form form) {
        if (form == null) {
            mBody = null;
            return this;
        }
        body(form.toString());
        contentType("application/x-www-form-urlencoded");
        return this;
    }

    public RequestBuilder body(@Nullable MultipartForm form) {
        if (form == null) {
            mBody = null;
            return this;
        }
        body(form.data());
        contentType(String.format("multipart/form-data; boundary=%s", form.BOUNDARY));
        return this;
    }

    public RequestBuilder body(@NonNull Pipe pipe) {
        mPipe = pipe;
        contentType(pipe.contentType());
        return this;
    }

    public RequestBuilder body(@NonNull File file) {
        return body(Pipe.forFile(file));
    }

    public RequestBuilder body(@Nullable Object object) {
        if (object instanceof List) {
            //noinspection unchecked
            return body((List) object);
        }
        if (object == null) {
            mBody = null;
        } else {
            final long start = System.currentTimeMillis();
            final String contentType = RequestConverter.getContentType(object.getClass(), mHeaders.get("Content-Type"));
            contentType(contentType);
            RequestConverter converter = Bridge.config().requestConverter(contentType);
            mBody = converter.convertObject(object, this);
            final long diff = System.currentTimeMillis() - start;
            Log.d(this, "Request conversion took %dms (%d seconds) for object of type %s.",
                    diff, diff / 1000, object.getClass().getName());
        }
        return this;
    }

    public RequestBuilder body(@Nullable Object[] objects) {
        if (objects == null || objects.length == 0) {
            mBody = null;
        } else {
            final long start = System.currentTimeMillis();
            final String contentType = RequestConverter.getContentType(objects[0].getClass(), mHeaders.get("Content-Type"));
            contentType(contentType);
            RequestConverter converter = Bridge.config().requestConverter(contentType);
            mBody = converter.convertArray(objects, this);
            final long diff = System.currentTimeMillis() - start;
            Log.d(this, "Request conversion took %dms (%d seconds) for array of %s objects.",
                    diff, diff / 1000, objects[0].getClass().getName());
        }
        return this;
    }

    public RequestBuilder body(@Nullable List<Object> objects) {
        if (objects == null || objects.size() == 0) {
            mBody = null;
        } else {
            final long start = System.currentTimeMillis();
            final String contentType = RequestConverter.getContentType(objects.get(0).getClass(), mHeaders.get("Content-Type"));
            contentType(contentType);
            RequestConverter converter = Bridge.config().requestConverter(contentType);
            mBody = converter.convertList(objects, this);
            final long diff = System.currentTimeMillis() - start;
            Log.d(this, "Request conversion took %dms (%d seconds) for list of %s objects.",
                    diff, diff / 1000, objects.get(0).getClass().getName());
        }
        return this;
    }

    public RequestBuilder uploadProgress(@NonNull ProgressCallback callback) {
        mUploadProgress = callback;
        return this;
    }

    public RequestBuilder infoCallback(@NonNull InfoCallback callback) {
        mInfoCallback = callback;
        return this;
    }

    public RequestBuilder cancellable(boolean cancelable) {
        mCancellable = cancelable;
        return this;
    }

    public RequestBuilder tag(@Nullable Object tag) {
        mTag = tag;
        return this;
    }

    @WorkerThread
    public Request request() throws BridgeException {
        return new Request(this).makeRequest();
    }

    public RequestBuilder throwIfNotSuccess() {
        mThrowIfNotSuccess = true;
        return this;
    }

    @UiThread
    public Request request(Callback callback) {
        mRequest = new Request(this);
        if (mContext.pushCallback(mRequest, callback)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mRequest.makeRequest();
                        if (mRequest.mCancelCallbackFired) return;
                        final Response response = mRequest.response();
                        mContext.fireCallbacks(mRequest, response, null);
                    } catch (final BridgeException e) {
                        if (mRequest.mCancelCallbackFired) return;
                        mContext.fireCallbacks(mRequest, mRequest.response(), e);
                    }
                }
            }).start();
        }
        return mRequest;
    }

    // Shortcut methods

    @Nullable
    @WorkerThread
    public Response response() throws BridgeException {
        return request().response();
    }

    @Nullable
    @WorkerThread
    public byte[] asBytes() throws BridgeException {
        throwIfNotSuccess();
        Response response = response();
        if (response == null) return null;
        return response.asBytes();
    }

    @Override
    public void asBytes(final @NonNull ResponseConvertCallback<byte[]> callback) {
        request(new Callback() {
            @Override
            public void response(Request request, Response response, BridgeException e) {
                if (e != null)
                    callback.onResponse(response, null, e);
                else callback.onResponse(response, response.asBytes(), null);
            }
        });
    }

    @Nullable
    @WorkerThread
    @Override
    public String asString() throws BridgeException {
        throwIfNotSuccess();
        Response response = response();
        if (response == null) return null;
        return response.asString();
    }

    @Override
    public void asString(final @NonNull ResponseConvertCallback<String> callback) {
        request(new Callback() {
            @Override
            public void response(Request request, Response response, BridgeException e) {
                if (e != null)
                    callback.onResponse(response, null, e);
                else callback.onResponse(response, response.asString(), null);
            }
        });
    }

    @Override
    public Response asLineStream(@NonNull LineCallback cb) throws BridgeException {
        mLineCallback = cb;
        return response();
    }

    @Override
    public void asLineStream(@NonNull LineCallback cb, @NonNull Callback callback) {
        mLineCallback = cb;
        request(callback);
    }

    @Nullable
    @WorkerThread
    @Override
    public Spanned asHtml() throws BridgeException {
        throwIfNotSuccess();
        Response response = response();
        if (response == null) return null;
        return response.asHtml();
    }

    @Override
    public void asHtml(final @NonNull ResponseConvertCallback<Spanned> callback) {
        request(new Callback() {
            @Override
            public void response(Request request, Response response, BridgeException e) {
                if (e != null)
                    callback.onResponse(response, null, e);
                else callback.onResponse(response, response.asHtml(), null);
            }
        });
    }

    @Nullable
    @WorkerThread
    @Override
    public Bitmap asBitmap() throws BridgeException {
        throwIfNotSuccess();
        Response response = response();
        if (response == null) return null;
        return response.asBitmap();
    }

    @Override
    public void asBitmap(final @NonNull ResponseConvertCallback<Bitmap> callback) {
        request(new Callback() {
            @Override
            public void response(Request request, Response response, BridgeException e) {
                if (e != null)
                    callback.onResponse(response, null, e);
                else callback.onResponse(response, response.asBitmap(), null);
            }
        });
    }

    @Nullable
    @WorkerThread
    public JSONObject asJsonObject() throws BridgeException {
        throwIfNotSuccess();
        Response response = response();
        if (response == null) return null;
        return response.asJsonObject();
    }

    @Override
    public void asJsonObject(final @NonNull ResponseConvertCallback<JSONObject> callback) {
        request(new Callback() {
            @Override
            public void response(Request request, Response response, BridgeException e) {
                if (e != null) {
                    callback.onResponse(response, null, e);
                } else {
                    try {
                        callback.onResponse(response, response.asJsonObject(), null);
                    } catch (BridgeException e1) {
                        callback.onResponse(response, null, e1);
                    }
                }
            }
        });
    }

    @Nullable
    @WorkerThread
    public JSONArray asJsonArray() throws BridgeException {
        throwIfNotSuccess();
        Response response = response();
        if (response == null) return null;
        return response.asJsonArray();
    }

    @Override
    public void asJsonArray(final @NonNull ResponseConvertCallback<JSONArray> callback) {
        request(new Callback() {
            @Override
            public void response(Request request, Response response, BridgeException e) {
                if (e != null) {
                    callback.onResponse(response, null, e);
                } else {
                    try {
                        callback.onResponse(response, response.asJsonArray(), null);
                    } catch (BridgeException e1) {
                        callback.onResponse(response, null, e1);
                    }
                }
            }
        });
    }

    @Nullable
    @Override
    public <T> List<T> asClassList(@NonNull Class<T> cls) throws BridgeException {
        throwIfNotSuccess();
        Response response = response();
        if (response == null) return null;
        return response.asClassList(cls);
    }

    @Override
    public <T> void asClassList(final @NonNull Class<T> cls, final @NonNull ResponseConvertCallback<List<T>> callback) {
        request(new Callback() {
            @Override
            public void response(Request request, Response response, BridgeException e) {
                if (e != null) {
                    callback.onResponse(response, null, e);
                } else {
                    try {
                        callback.onResponse(response, response.asClassList(cls), null);
                    } catch (BridgeException e1) {
                        callback.onResponse(response, null, e1);
                    }
                }
            }
        });
    }

    @WorkerThread
    public void asFile(@NonNull File destination) throws BridgeException {
        throwIfNotSuccess();
        Response response = response();
        if (response == null)
            throw new BridgeException(request(), "No response was returned to save into a file.", BridgeException.REASON_RESPONSE_UNPARSEABLE);
        response.asFile(destination);
    }

    @Override
    public void asFile(final @NonNull File destination, final @NonNull ResponseConvertCallback<File> callback) {
        request(new Callback() {
            @Override
            public void response(Request request, Response response, BridgeException e) {
                if (e != null) {
                    callback.onResponse(response, null, e);
                } else {
                    try {
                        response.asFile(destination);
                        callback.onResponse(response, destination, null);
                    } catch (BridgeException e1) {
                        callback.onResponse(response, null, e1);
                    }
                }
            }
        });
    }

    @Nullable
    @Override
    public <T> T asClass(@NonNull Class<T> cls) throws BridgeException {
        throwIfNotSuccess();
        Response response = response();
        if (response == null) return null;
        return response.asClass(cls);
    }

    @Override
    public <T> void asClass(final @NonNull Class<T> cls, final @NonNull ResponseConvertCallback<T> callback) {
        request(new Callback() {
            @Override
            public void response(Request request, Response response, BridgeException e) {
                if (e != null) {
                    callback.onResponse(response, null, e);
                } else {
                    try {
                        callback.onResponse(response, response.asClass(cls), null);
                    } catch (BridgeException e1) {
                        callback.onResponse(response, null, e1);
                    }
                }
            }
        });
    }

    @Nullable
    @Override
    public <T> T[] asClassArray(@NonNull Class<T> cls) throws BridgeException {
        throwIfNotSuccess();
        Response response = response();
        if (response == null) return null;
        return response.asClassArray(cls);
    }

    @Override
    public <T> void asClassArray(final @NonNull Class<T> cls, final @NonNull ResponseConvertCallback<T[]> callback) {
        request(new Callback() {
            @Override
            public void response(Request request, Response response, BridgeException e) {
                if (e != null) {
                    callback.onResponse(response, null, e);
                } else {
                    try {
                        callback.onResponse(response, response.asClassArray(cls), null);
                    } catch (BridgeException e1) {
                        callback.onResponse(response, null, e1);
                    }
                }
            }
        });
    }

    @Nullable
    @WorkerThread
    @Override
    public Object asSuggested() throws BridgeException {
        throwIfNotSuccess();
        Response response = response();
        if (response == null) return null;
        return response.asSuggested();
    }

    @Override
    public void asSuggested(final @NonNull ResponseConvertCallback<Object> callback) {
        request(new Callback() {
            @Override
            public void response(Request request, Response response, BridgeException e) {
                if (e != null) {
                    callback.onResponse(response, null, e);
                } else {
                    try {
                        callback.onResponse(response, response.asSuggested(), null);
                    } catch (BridgeException e1) {
                        callback.onResponse(response, null, e1);
                    }
                }
            }
        });
    }
}