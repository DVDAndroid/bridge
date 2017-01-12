package com.afollestad.bridge.conversion.base;

import android.annotation.SuppressLint;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.afollestad.bridge.BridgeUtil;
import com.afollestad.bridge.Response;
import com.afollestad.bridge.annotations.Header;
import com.afollestad.bridge.annotations.JSON;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Aidan Follestad (afollestad)
 */
public abstract class ResponseConverter extends Converter {

    public ResponseConverter() {
    }

    @Nullable
    public final <T> T convertObject(@NonNull Response response, @NonNull Class<T> targetCls) {
        final byte[] responseContent = response.asBytes();
        if (responseContent == null || responseContent.length == 0) return null;
        T object = BridgeUtil.newInstance(targetCls);
        try {
            onPrepare(response, object);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to prepare ResponseConverter for target class %s: %s",
                    targetCls.getName(), e.getMessage()), e);
        }
        //noinspection unchecked
        object = (T) convertObject(object, targetCls, response);
        try {
            onFinish(response, object);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to finish ResponseConverter for target class %s: %s",
                    targetCls.getName(), e.getMessage()), e);
        }
        return object;
    }

    public final <T> T[] convertArray(@NonNull Response response, @NonNull Class<T> targetCls) {
        final byte[] responseContent = response.asBytes();
        if (responseContent == null || responseContent.length == 0) return null;
        final int size;
        try {
            size = getResponseArrayLength(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get the size of a response's array: " + e.getMessage(), e);
        }

        final Object array = Array.newInstance(targetCls, size);
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                Object value;
                try {
                    final Object originalValue = getValueFromResponseArray(response, i);
                    if (originalValue == null) {
                        value = null;
                    } else {
                        final ResponseConverter converter = spawnConverter(targetCls, originalValue, response);
                        value = converter.convertObject(response, targetCls);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to retrieve an element from response's array: " + e.getMessage(), e);
                }
                Array.set(array, i, value);
            }
        }

        //noinspection unchecked
        return (T[]) array;
    }

    @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "unchecked"})
    @SuppressLint("SwitchIntDef")
    private Object convertObject(@Nullable Object object, @NonNull Class<?> targetCls, @NonNull Response response) {
        if (object == null)
            object = BridgeUtil.newInstance(targetCls);
        final List<Field> fields = getAllFields(targetCls);
        for (Field field : fields) {
            field.setAccessible(true);
            boolean canConvert;
            try {
                canConvert = canConvertField(field);
            } catch (Exception e) {
                throw new RuntimeException(String.format("Failed to check if field %s can be converted by %s: %s",
                        field.getName(), getClass().getName(), e.getMessage()), e);
            }
            @FieldType
            final int fieldType = getFieldType(field.getType());

            JSON jsonAnnotation = field.getAnnotation(JSON.class);
            if (jsonAnnotation != null) {
                try {
                    field.set(object, getValueFromResponse("", fieldType, null));
                } catch (Throwable t) {
                    throw new RuntimeException(String.format("Failed to set the value of field %s of class %s: %s",
                            field.getName(), targetCls.getName(), t.getMessage()), t);
                }
                continue;
            }

            Header headerAnnotation = field.getAnnotation(Header.class);
            if (headerAnnotation != null) {
                final String headerValue = response.header(getHeaderName(field, headerAnnotation));
                try {
                    switch (fieldType) {
                        case Converter.FIELD_SHORT:
                            if (headerValue == null)
                                field.setShort(object, (short) 0);
                            else
                                field.setShort(object, Short.parseShort(headerValue));
                            break;
                        case Converter.FIELD_INTEGER:
                            if (headerValue == null)
                                field.setInt(object, 0);
                            else
                                field.setInt(object, Integer.parseInt(headerValue));
                            break;
                        case Converter.FIELD_LONG:
                            if (headerValue == null)
                                field.setLong(object, 0L);
                            else
                                field.setLong(object, Long.parseLong(headerValue));
                            break;
                        case Converter.FIELD_DOUBLE:
                            if (headerValue == null)
                                field.setDouble(object, 0d);
                            else
                                field.setDouble(object, Double.parseDouble(headerValue));
                            break;
                        case Converter.FIELD_FLOAT:
                            if (headerValue == null)
                                field.setFloat(object, 0f);
                            else
                                field.setFloat(object, Float.parseFloat(headerValue));
                            break;
                        case Converter.FIELD_BOOLEAN:
                            if (headerValue == null)
                                field.setBoolean(object, false);
                            else
                                field.setBoolean(object, headerValue.equalsIgnoreCase("true") || headerValue.equals("1"));
                            break;
                        case Converter.FIELD_STRING:
                            field.set(object, headerValue);
                            break;
                        default:
                            throw new Exception("Header annotations can only be applied to primitive field types.");
                    }
                } catch (Throwable t) {
                    throw new RuntimeException(String.format("Failed to set the value of field %s of class %s: %s",
                            field.getName(), targetCls.getName(), t.getMessage()), t);
                }
                continue;
            }

            if (canConvert) {
                String name;
                try {
                    name = getFieldInputName(field);
                } catch (Exception e) {
                    throw new RuntimeException(String.format("Failed to get input name for field %s of type %s: %s",
                            field.getName(), field.getType().getName(), e.getMessage()), e);
                }

                Object responseValue;
                try {
                    responseValue = getValueFromResponse(name, fieldType, field.getType());
                    if (name.contains(".") && responseValue == null)
                        responseValue = getValueFromResponse(name.split("\\."), fieldType, field.getType());
                } catch (Exception e) {
                    throw new RuntimeException(String.format("Failed to get value from response for field %s of type %s: %s",
                            field.getName(), field.getType().getName(), e.getMessage()), e);
                }

                Class<?> fieldCls = field.getType();
                if (responseValue != null && field.getType() == Object.class)
                    fieldCls = responseValue.getClass();

                try {
                    if (isPrimitive(fieldCls)) {
                        switch (fieldType) {
                            case FIELD_SHORT:
                                if (responseValue == null)
                                    field.setShort(object, (short) 0);
                                else
                                    field.setShort(object, (short) responseValue);
                                break;
                            case FIELD_INTEGER:
                                if (responseValue == null)
                                    field.setInt(object, 0);
                                else
                                    field.setInt(object, (int) responseValue);
                                break;
                            case FIELD_LONG:
                                if (responseValue == null)
                                    field.setLong(object, 0L);
                                else
                                    field.setLong(object, (long) responseValue);
                                break;
                            case FIELD_FLOAT:
                                if (responseValue == null)
                                    field.setFloat(object, 0f);
                                else
                                    field.setFloat(object, (float) responseValue);
                                break;
                            case FIELD_DOUBLE:
                                if (responseValue == null)
                                    field.setDouble(object, 0d);
                                else
                                    field.setDouble(object, (double) responseValue);
                                break;
                            case FIELD_BOOLEAN:
                                if (responseValue == null)
                                    field.setBoolean(object, false);
                                else if (responseValue instanceof Integer)
                                    field.setBoolean(object, (Integer) responseValue == 1);
                                else
                                    field.setBoolean(object, (boolean) responseValue);
                                break;
                            case FIELD_STRING:
                            default:
                                field.set(object, responseValue);
                                break;
                        }
                    } else if (isArray(fieldCls)) {
                        if (responseValue == null) {
                            field.set(object, null);
                        } else {
                            final int size = getResponseArrayLength(responseValue);
                            final Class<?> elementType = fieldCls.getComponentType();
                            final Object array = Array.newInstance(elementType, size);
                            for (int i = 0; i < size; i++) {
                                try {
                                    final Object value = getValueFromResponseArray(responseValue, i);
                                    if (value == null || isPrimitive(elementType)) {
                                        Array.set(array, i, value);
                                    } else {
                                        ResponseConverter converter = spawnConverter(elementType, value, response);
                                        Array.set(array, i, converter.convertObject(response, elementType));
                                    }
                                } catch (Throwable t) {
                                    throw new RuntimeException(String.format("Failed to get value from response array of elements %s: %s",
                                            elementType.getName(), t.getMessage()), t);
                                }
                            }
                            field.set(object, array);
                        }
                    } else if (isArrayList(fieldCls)) {
                        if (responseValue == null) {
                            field.set(object, null);
                        } else {
                            final int size = getResponseArrayLength(responseValue);
                            final Class<?> elementType = getArrayListType(field);
                            final List list = new ArrayList(size);
                            for (int i = 0; i < size; i++) {
                                try {
                                    final Object value = getValueFromResponseArray(responseValue, i);
                                    if (value == null || isPrimitive(elementType)) {
                                        list.add(value);
                                    } else {
                                        ResponseConverter converter = spawnConverter(elementType, value, response);
                                        list.add(converter.convertObject(response, elementType));
                                    }
                                } catch (Throwable t) {
                                    throw new RuntimeException(String.format("Failed to get value from response array of elements %s: %s",
                                            elementType.getName(), t.getMessage()), t);
                                }
                            }
                            field.set(object, list);
                        }
                    } else {
                        if (responseValue == null) {
                            field.set(object, null);
                        } else {
                            try {
                                ResponseConverter converter = spawnConverter(fieldCls, responseValue, response);
                                field.set(object, converter.convertObject(response, fieldCls));
                            } catch (Exception e) {
                                throw new RuntimeException(String.format("Failed to spawn a converter for field %s of type %s: %s",
                                        field.getName(), fieldCls.getName(), e.getMessage()), e);
                            }
                        }
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(String.format("Failed to set the value of field %s of class %s: %s",
                            field.getName(), field.getType().getName(), e.getMessage()), e);
                }
            }
        }
        return object;
    }

    public abstract void onPrepare(@NonNull Response response, @NonNull Object object) throws Exception;

    public abstract boolean canConvertField(@NonNull Field field) throws Exception;

    @NonNull
    public abstract String getFieldInputName(@NonNull Field field) throws Exception;

    @Nullable
    public abstract Object getValueFromResponse(@NonNull String name, @FieldType int fieldType, @Nullable Class<?> cls) throws Exception;

    @Nullable
    public abstract Object getValueFromResponse(@NonNull String[] nameParts, @FieldType int fieldType, @NonNull Class<?> cls) throws Exception;

    @IntRange(from = 0, to = Integer.MAX_VALUE)
    public abstract int getResponseArrayLength(@NonNull Response response) throws Exception;

    @Nullable
    public abstract Object getValueFromResponseArray(@NonNull Response response, @IntRange(from = 0, to = Integer.MAX_VALUE - 1) int index) throws Exception;

    @IntRange(from = 0, to = Integer.MAX_VALUE)
    public abstract int getResponseArrayLength(@NonNull Object array);

    @Nullable
    public abstract Object getValueFromResponseArray(@NonNull Object array, @IntRange(from = 0, to = Integer.MAX_VALUE - 1) int index) throws Exception;

    @NonNull
    public abstract ResponseConverter spawnConverter(@NonNull Class<?> forType, @NonNull Object responseValue, @NonNull Response response) throws Exception;

    public abstract void onFinish(@NonNull Response response, @NonNull Object object) throws Exception;
}