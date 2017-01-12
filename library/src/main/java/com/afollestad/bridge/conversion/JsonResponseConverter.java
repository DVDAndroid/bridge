package com.afollestad.bridge.conversion;

import android.annotation.SuppressLint;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.afollestad.bridge.Response;
import com.afollestad.bridge.annotations.Body;
import com.afollestad.bridge.conversion.base.ResponseConverter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;


/**
 * @author Aidan Follestad (afollestad)
 */
public class JsonResponseConverter extends ResponseConverter {

    private JSONObject mCurrentObject;

    public JsonResponseConverter() {
    }

    public JsonResponseConverter(JSONObject currentObject) {
        mCurrentObject = currentObject;
    }

    @Override
    public void onPrepare(@NonNull Response response, @NonNull Object object) throws Exception {
        if (mCurrentObject == null)
            mCurrentObject = response.asJsonObject();
    }

    @Override
    public boolean canConvertField(@NonNull Field field) throws Exception {
        return field.getAnnotation(Body.class) != null;
    }

    @SuppressLint("SwitchIntDef")
    @Nullable
    @Override
    public Object getValueFromResponse(@NonNull String name, @FieldType int fieldType, @Nullable Class<?> cls) throws Exception {
        if (name.isEmpty() || cls == null) {
            switch (fieldType) {
                case FIELD_STRING:
                    return mCurrentObject.toString();
                case FIELD_JSON:
                    return mCurrentObject;
                default:
                    return null;
            }
        }

        if (mCurrentObject.isNull(name)) return null;
        switch (fieldType) {
            case FIELD_SHORT:
                return ((Integer) mCurrentObject.getInt(name)).shortValue();
            case FIELD_INTEGER:
                return mCurrentObject.getInt(name);
            case FIELD_LONG:
                return mCurrentObject.getLong(name);
            case FIELD_FLOAT:
                return ((Double) mCurrentObject.getDouble(name)).floatValue();
            case FIELD_DOUBLE:
                return mCurrentObject.getDouble(name);
            case FIELD_BOOLEAN:
                Object value = mCurrentObject.get(name);
                if (value instanceof Integer)
                    return (Integer) value == 1;
                else if (value instanceof Boolean)
                    return value;
                else throw new Exception("Unexpected type for JSON field " + value);
            case FIELD_STRING:
                return mCurrentObject.getString(name);
            default:
                return mCurrentObject.get(name);
        }
    }

    @SuppressLint("SwitchIntDef")
    @Nullable
    @Override
    public Object getValueFromResponse(@NonNull String[] nameParts, @FieldType int fieldType, @NonNull Class<?> cls) throws Exception {
        JSONObject currentObj = null;
        for (int i = 0; i < nameParts.length - 1; i++) {
            Object val = currentObj != null ? currentObj.opt(nameParts[i]) : mCurrentObject.opt(nameParts[i]);
            if (val == null)
                return null;
            if (val instanceof JSONObject) {
                currentObj = (JSONObject) val;
            } else {
                throw new RuntimeException(String.format("Expected JSONObject for name part %s, but found %s.",
                        nameParts[i], val.getClass().getName()));
            }
        }

        final String name = nameParts[nameParts.length - 1];
        final JSONObject pullObj = currentObj != null ? currentObj : mCurrentObject;
        if (pullObj.isNull(name)) return null;
        switch (fieldType) {
            case FIELD_SHORT:
                return ((Integer) pullObj.getInt(name)).shortValue();
            case FIELD_INTEGER:
                return pullObj.getInt(name);
            case FIELD_LONG:
                return pullObj.getLong(name);
            case FIELD_FLOAT:
                return ((Double) pullObj.getDouble(name)).floatValue();
            case FIELD_DOUBLE:
                return pullObj.getDouble(name);
            case FIELD_BOOLEAN:
                Object value = pullObj.get(name);
                if (value instanceof Integer)
                    return (Integer) value == 1;
                else if (value instanceof Boolean)
                    return value;
                else throw new Exception("Unexpected type for JSON field " + value);
            case FIELD_STRING:
                return pullObj.getString(name);
            default:
                return pullObj.get(name);
        }
    }

    @NonNull
    @Override
    public String getFieldInputName(@NonNull Field field) throws Exception {
        final Body bodyAnnotation = field.getAnnotation(Body.class);
        return getName(field, bodyAnnotation);
    }

    @Override
    public int getResponseArrayLength(@NonNull Response response) throws Exception {
        //noinspection ConstantConditions
        return response.asJsonArray().length();
    }

    @Nullable
    @Override
    public Object getValueFromResponseArray(@NonNull Response response, @IntRange(from = 0, to = Integer.MAX_VALUE - 1) int index) throws Exception {
        //noinspection ConstantConditions
        return response.asJsonArray().get(index);
    }

    @Override
    public int getResponseArrayLength(@NonNull Object array) {
        return ((JSONArray) array).length();
    }

    @Nullable
    @Override
    public Object getValueFromResponseArray(@NonNull Object array, @IntRange(from = 0, to = Integer.MAX_VALUE - 1) int index) throws Exception {
        return ((JSONArray) array).get(index);
    }

    @NonNull
    @Override
    public ResponseConverter spawnConverter(@NonNull Class<?> forType, @NonNull Object responseValue, @NonNull Response response) throws Exception {
        return new JsonResponseConverter((JSONObject) responseValue);
    }

    @Override
    public void onFinish(@NonNull Response response, @NonNull Object object) throws Exception {
    }

    private String getName(@NonNull Field fld, @NonNull Body body) {
        if (body.name() == null || body.name().trim().isEmpty())
            return fld.getName();
        return body.name();
    }
}