package com.waterdrop.mybatisreactive.toolkit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * A common delegate for detecting Kotlin's presence and for identifying Kotlin types.
 */
public abstract class KotlinDetector {

    private static final Class<? extends Annotation> kotlinMetadata;

    private static final boolean kotlinReflectPresent;

    static {
        Class<?> metadata;
        ClassLoader classLoader = KotlinDetector.class.getClassLoader();
        try {
            metadata = ClassUtils.forName("kotlin.Metadata", classLoader);
        }
        catch (ClassNotFoundException ex) {
            // Kotlin API not available - no Kotlin support
            metadata = null;
        }
        kotlinMetadata = (Class<? extends Annotation>) metadata;
        kotlinReflectPresent = ClassUtils.isPresent("kotlin.reflect.full.KClasses", classLoader);
    }


    /**
     * Determine whether Kotlin is present in general.
     */
    public static boolean isKotlinPresent() {
        return (kotlinMetadata != null);
    }

    /**
     * Determine whether Kotlin reflection is present.
     * @since 5.1
     */
    public static boolean isKotlinReflectPresent() {
        return kotlinReflectPresent;
    }

    /**
     * Determine whether the given {@code Class} is a Kotlin type
     * (with Kotlin metadata present on it).
     */
    public static boolean isKotlinType(Class<?> clazz) {
        return (kotlinMetadata != null && clazz.getDeclaredAnnotation(kotlinMetadata) != null);
    }

    /**
     * Return {@code true} if the method is a suspending function.
     * @since 5.3
     */
    public static boolean isSuspendingFunction(Method method) {
        if (KotlinDetector.isKotlinType(method.getDeclaringClass())) {
            Class<?>[] types = method.getParameterTypes();
            if (types.length > 0 && "kotlin.coroutines.Continuation".equals(types[types.length - 1].getName())) {
                return true;
            }
        }
        return false;
    }
}
