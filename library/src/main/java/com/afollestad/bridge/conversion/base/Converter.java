package com.afollestad.bridge.conversion.base;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import com.afollestad.bridge.annotations.Header;

import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
class Converter {

    protected String getHeaderName(@NonNull Field fld, @NonNull Header header) {
        if (header.name() == null || header.name().trim().isEmpty())
            return fld.getName();
        return header.name();
    }

    protected List<Field> getAllFields(@NonNull Class<?> cls) {
        final List<Field> fields = new ArrayList<>(Arrays.asList(cls.getDeclaredFields()));
        while (cls.getSuperclass() != null) {
            cls = cls.getSuperclass();
            Collections.addAll(fields, cls.getDeclaredFields());
        }
        return fields;
    }

    @IntDef({FIELD_JSON, FIELD_OTHER, FIELD_SHORT, FIELD_INTEGER, FIELD_LONG,
            FIELD_FLOAT, FIELD_DOUBLE, FIELD_BOOLEAN, FIELD_STRING})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FieldType {
    }

    public static final int FIELD_JSON = -2;
    public static final int FIELD_OTHER = -1;
    public static final int FIELD_SHORT = 1;
    public static final int FIELD_INTEGER = 2;
    public static final int FIELD_LONG = 3;
    public static final int FIELD_FLOAT = 4;
    public static final int FIELD_DOUBLE = 5;
    public static final int FIELD_BOOLEAN = 6;
    public static final int FIELD_STRING = 7;

    @FieldType
    protected int getFieldType(@NonNull Class<?> cls) {
        if (cls == short.class || cls == Short.class)
            return FIELD_SHORT;
        else if (cls == int.class || cls == Integer.class)
            return FIELD_INTEGER;
        else if (cls == long.class || cls == Long.class)
            return FIELD_LONG;
        else if (cls == float.class || cls == Float.class)
            return FIELD_FLOAT;
        else if (cls == double.class || cls == Double.class)
            return FIELD_DOUBLE;
        else if (cls == boolean.class || cls == Boolean.class)
            return FIELD_BOOLEAN;
        else if (cls == String.class)
            return FIELD_STRING;
        else if (cls == JSONObject.class)
            return FIELD_JSON;
        return FIELD_OTHER;
    }

    protected boolean isPrimitive(@NonNull Class<?> cls) {
        return getFieldType(cls) != FIELD_OTHER;
    }

    protected boolean isArray(@NonNull Class<?> fieldType) {
        return fieldType.isArray();
    }

    protected boolean isArrayList(@NonNull Class<?> fieldType) {
        return fieldType == List.class || fieldType == ArrayList.class;
    }

    protected Class<?> getArrayListType(@NonNull Field field) {
        ParameterizedType stringListType = (ParameterizedType) field.getGenericType();
        return (Class<?>) stringListType.getActualTypeArguments()[0];
    }

//    protected Class<?> getArrayListType(@NonNull List<?> list) {
//        return list.toArray().getClass().getComponentType();
//    }
}