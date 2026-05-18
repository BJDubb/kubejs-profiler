package au.beckam.kubejsprofiler.util;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

public final class RhinoFunctionIntrospector {

    private RhinoFunctionIntrospector() {
    }

    private static final Field MISSING_FIELD;

    static {
        try {
            MISSING_FIELD = RhinoFunctionIntrospector.class.getDeclaredField("MISSING_FIELD");
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final ClassValue<ConcurrentHashMap<String, Field>> FIELD_CACHE = new ClassValue<>() {
        @Override
        protected ConcurrentHashMap<String, Field> computeValue(Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    public static String getSourceName(Object function) {
        Object idata = getIdata(function);
        return resolveSourceName(idata);
    }

    public static String getFunctionName(Object function) {
        Object idata = getIdata(function);
        return resolveFunctionName(idata);
    }

    public static int getLineNumber(Object function) {
        Object idata = getIdata(function);
        return resolveLineNumber(idata);
    }

    public static boolean isTopLevel(Object function) {
        Object idata = getIdata(function);
        Boolean topLevel = getBooleanField(idata, "topLevel");
        return Boolean.TRUE.equals(topLevel);
    }

    public static Object getInterpreterDataFromCallFrame(Object callFrame) {
        if (callFrame == null) {
            return null;
        }

        Object idata = getField(callFrame, "idata");

        if (idata != null) {
            return idata;
        }

        Object fnOrScript = getField(callFrame, "fnOrScript");

        if (fnOrScript != null) {
            return getField(fnOrScript, "idata");
        }

        return null;
    }

    public static String getSourceNameFromInterpreterData(Object idata) {
        return resolveSourceName(idata);
    }

    public static String getFunctionNameFromInterpreterData(Object idata) {
        return resolveFunctionName(idata);
    }

    public static int getLineNumberFromInterpreterData(Object idata) {
        return resolveLineNumber(idata);
    }

    private static String resolveSourceName(Object idata) {
        String source = getStringField(idata, "itsSourceFile");

        if (isUseful(source)) {
            return source;
        }

        return "<unknown source>";
    }

    private static String resolveFunctionName(Object idata) {
        String name = getStringField(idata, "itsName");

        if (isUseful(name)) {
            return name;
        }

        Boolean topLevel = getBooleanField(idata, "topLevel");

        if (Boolean.TRUE.equals(topLevel)) {
            return "<top-level>";
        }

        return "<anonymous>";
    }

    private static int resolveLineNumber(Object idata) {
        Integer firstLine = getIntField(idata, "firstLinePC");

        if (firstLine != null && firstLine > 0) {
            return firstLine;
        }

        return -1;
    }

    private static Object getIdata(Object function) {
        if (function == null) {
            return null;
        }

        return getField(function, "idata");
    }

    private static String getStringField(Object target, String fieldName) {
        Object value = getField(target, fieldName);

        if (value instanceof String stringValue) {
            return stringValue;
        }

        if (value != null) {
            return value.toString();
        }

        return null;
    }

    private static Integer getIntField(Object target, String fieldName) {
        Object value = getField(target, fieldName);

        if (value instanceof Number number) {
            return number.intValue();
        }

        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    private static Boolean getBooleanField(Object target, String fieldName) {
        Object value = getField(target, fieldName);

        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }

        if (value != null) {
            return Boolean.parseBoolean(value.toString());
        }

        return null;
    }

    private static Object getField(Object target, String fieldName) {
        if (target == null) {
            return null;
        }

        Field field = resolveField(target.getClass(), fieldName);

        if (field == null) {
            return null;
        }

        try {
            return field.get(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Field resolveField(Class<?> startClass, String fieldName) {
        ConcurrentHashMap<String, Field> cache = FIELD_CACHE.get(startClass);

        Field cached = cache.get(fieldName);

        if (cached != null) {
            return cached == MISSING_FIELD ? null : cached;
        }

        Field found = lookupField(startClass, fieldName);

        cache.put(fieldName, found != null ? found : MISSING_FIELD);

        return found;
    }

    private static Field lookupField(Class<?> startClass, String fieldName) {
        for (Class<?> type = startClass; type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable ignored) {
                return null;
            }
        }

        return null;
    }

    private static boolean isUseful(String value) {
        return value != null
                && !value.isBlank()
                && !"null".equalsIgnoreCase(value);
    }
}
