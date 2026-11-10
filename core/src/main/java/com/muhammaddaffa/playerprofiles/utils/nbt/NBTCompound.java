package com.muhammaddaffa.playerprofiles.utils.nbt;

import java.util.*;

public class NBTCompound {

    private final Map<String, Object> values;

    public NBTCompound(Map<String, Object> values) {
        this.values = values != null ? values : Collections.emptyMap();
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public Set<String> keySet() {
        return values.keySet();
    }

    public Object get(String key) {
        return values.get(key);
    }

    public String getString(String key) {
        Object val = values.get(key);
        return val instanceof String s ? s : "";
    }

    public int getInt(String key) {
        return getInt(key, 0);
    }

    public int getInt(String key, int def) {
        Object val = values.get(key);
        if (val instanceof Number n) return n.intValue();
        return def;
    }

    public byte getByte(String key) {
        Object val = values.get(key);
        if (val instanceof Number n) return n.byteValue();
        return 0;
    }

    public short getShort(String key) {
        Object val = values.get(key);
        if (val instanceof Number n) return n.shortValue();
        return 0;
    }

    public long getLong(String key) {
        Object val = values.get(key);
        if (val instanceof Number n) return n.longValue();
        return 0L;
    }

    public float getFloat(String key) {
        Object val = values.get(key);
        if (val instanceof Number n) return n.floatValue();
        return 0f;
    }

    public double getDouble(String key) {
        Object val = values.get(key);
        if (val instanceof Number n) return n.doubleValue();
        return 0.0;
    }

    public NBTCompound getCompound(String key) {
        Object val = values.get(key);
        return val instanceof NBTCompound comp ? comp : null;
    }

    @SuppressWarnings("unchecked")
    public List<Object> getList(String key) {
        Object val = values.get(key);
        return val instanceof List<?> list ? (List<Object>) list : Collections.emptyList();
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
