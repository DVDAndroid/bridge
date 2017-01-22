package com.afollestad.bridge;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spanned;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

/**
 * A version of {@link AsResults} that throws exceptions for all methods. Used in {@link RequestBuilder}.
 *
 * @author Aidan Follestad (afollestad)
 */
interface AsResultsExceptions {

    @Nullable
    byte[] asBytes() throws BridgeException;

    void asBytes(@NonNull ResponseConvertCallback<byte[]> callback);

    @Nullable
    String asString() throws BridgeException;

    void asString(@NonNull ResponseConvertCallback<String> callback);

    Response asLineStream(@NonNull LineCallback cb) throws BridgeException;

    void asLineStream(@NonNull LineCallback cb, @NonNull Callback callback);

    @Nullable
    Spanned asHtml() throws BridgeException;

    void asHtml(@NonNull ResponseConvertCallback<Spanned> callback);

    @Nullable
    Bitmap asBitmap() throws BridgeException;

    void asBitmap(@NonNull ResponseConvertCallback<Bitmap> callback);

    @Nullable
    JSONObject asJsonObject() throws BridgeException;

    void asJsonObject(@NonNull ResponseConvertCallback<JSONObject> callback);

    @Nullable
    JSONArray asJsonArray() throws BridgeException;

    void asJsonArray(@NonNull ResponseConvertCallback<JSONArray> callback);

    void asFile(@NonNull File destination) throws BridgeException;

    void asFile(@NonNull File destination, @NonNull ResponseConvertCallback<File> callback);

    @Nullable
    <T> T asClass(@NonNull Class<T> cls) throws BridgeException;

    <T> void asClass(@NonNull Class<T> cls, @NonNull ResponseConvertCallback<T> callback);

    @Nullable
    <T> T[] asClassArray(@NonNull Class<T> cls) throws BridgeException;

    <T> void asClassArray(@NonNull Class<T> cls, @NonNull ResponseConvertCallback<T[]> callback);

    @Nullable
    <T> List<T> asClassList(@NonNull Class<T> cls) throws BridgeException;

    <T> void asClassList(@NonNull Class<T> cls, @NonNull ResponseConvertCallback<List<T>> callback);

    @Nullable
    Object asSuggested() throws BridgeException;

    void asSuggested(@NonNull ResponseConvertCallback<Object> callback);
}
