package au.beckam.kubejsprofiler.util;

import java.lang.reflect.Field;

public final class RhinoFunctionIntrospector {

    public static String getSourceName(Object function) {
        Object idata = getIdata(function);

        String source = getStringField(idata, "itsSourceFile");

        if (isUseful(source)) {
            return source;
        }

        return "<unknown source>";
    }

    public static String getFunctionName(Object function) {
        Object idata = getIdata(function);

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

    public static int getLineNumber(Object function) {
        Object idata = getIdata(function);

        Integer firstLine = getIntField(idata, "firstLinePC");

        if (firstLine != null && firstLine > 0) {
            return firstLine;
        }

        return -1;
    }

    public static boolean isTopLevel(Object function) {
        Object idata = getIdata(function);

        Boolean topLevel = getBooleanField(idata, "topLevel");

        return Boolean.TRUE.equals(topLevel);
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

        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (Throwable ignored) {
            }
        }

        return null;
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
        String source = getStringField(idata, "itsSourceFile");

        if (isUseful(source)) {
            return source;
        }

        return "<unknown source>";
    }

    public static String getFunctionNameFromInterpreterData(Object idata) {
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

    public static int getLineNumberFromInterpreterData(Object idata) {
        Integer firstLine = getIntField(idata, "firstLinePC");

        if (firstLine != null && firstLine > 0) {
            return firstLine;
        }

        return -1;
    }

    private static boolean isUseful(String value) {
        return value != null
                && !value.isBlank()
                && !"null".equalsIgnoreCase(value);
    }
}