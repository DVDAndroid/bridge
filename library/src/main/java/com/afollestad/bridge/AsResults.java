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
 * @author Aidan Follestad (afollestad)
 */
interface AsResults {

    @Nullable
    byte[] asBytes();

    @Nullable
    String asString();

    @Nullable
    Spanned asHtml();

    @Nullable
    Bitmap asBitmap();

    @Nullable
    JSONObject asJsonObject() throws BridgeException;

    @Nullable
    JSONArray asJsonArray() throws BridgeException;

    void asFile(File destination) throws BridgeException;

    @Nullable
    <T> T asClass(@NonNull Class<T> cls) throws BridgeException;

    @Nullable
    <T> T[] asClassArray(@NonNull Class<T> cls) throws BridgeException;

    @Nullable
    <T> List<T> asClassList(@NonNull Class<T> cls) throws BridgeException;

    @Nullable
    Object asSuggested() throws BridgeException;
}
