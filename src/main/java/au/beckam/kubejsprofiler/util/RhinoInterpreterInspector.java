package au.beckam.kubejsprofiler.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RhinoInterpreterInspector {
    private static final Logger LOGGER = LoggerFactory.getLogger("kubejs-profiler");
    private static final AtomicBoolean DUMPED = new AtomicBoolean(false);

    public static void dumpOnce() {
        if (!DUMPED.compareAndSet(false, true)) {
            return;
        }

        dumpInterpreterMethods();
        dumpInterpreterNestedClasses();
    }

    private static void dumpInterpreterMethods() {
        try {
            Class<?> interpreterClass = Class.forName("dev.latvian.mods.rhino.Interpreter");

            LOGGER.warn("==== KubeJS Profiler: Rhino Interpreter methods ====");

            for (Method method : interpreterClass.getDeclaredMethods()) {
                LOGGER.warn(
                        "{} {}({}) -> {}",
                        Modifier.toString(method.getModifiers()),
                        method.getName(),
                        Arrays.toString(method.getParameterTypes()),
                        method.getReturnType().getName()
                );
            }

            LOGGER.warn("==== End Rhino Interpreter methods ====");
        } catch (Throwable throwable) {
            LOGGER.error("Failed to dump Rhino Interpreter methods", throwable);
        }
    }

    private static void dumpInterpreterNestedClasses() {
        try {
            Class<?> interpreterClass = Class.forName("dev.latvian.mods.rhino.Interpreter");

            LOGGER.warn("==== KubeJS Profiler: Rhino Interpreter nested classes ====");

            for (Class<?> nestedClass : interpreterClass.getDeclaredClasses()) {
                LOGGER.warn("Nested class: {}", nestedClass.getName());

                LOGGER.warn("  Fields:");
                for (Field field : nestedClass.getDeclaredFields()) {
                    LOGGER.warn(
                            "    {} {}",
                            field.getType().getName(),
                            field.getName()
                    );
                }

                LOGGER.warn("  Methods:");
                for (Method method : nestedClass.getDeclaredMethods()) {
                    LOGGER.warn(
                            "    {} {}({}) -> {}",
                            Modifier.toString(method.getModifiers()),
                            method.getName(),
                            Arrays.toString(method.getParameterTypes()),
                            method.getReturnType().getName()
                    );
                }
            }

            LOGGER.warn("==== End Rhino Interpreter nested classes ====");
        } catch (Throwable throwable) {
            LOGGER.error("Failed to dump Rhino Interpreter nested classes", throwable);
        }
    }
}