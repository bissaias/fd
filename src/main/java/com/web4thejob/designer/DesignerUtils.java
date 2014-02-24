package com.web4thejob.designer;

import org.springframework.util.MethodInvoker;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;

/**
 * @author Veniamin Isaias
 * @since 1.0.0
 */
public class DesignerUtils {

    public static void invokeSetter(Object instance, String property, Object[] args) throws NoSuchMethodException,
            ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        MethodInvoker methodInvoker = new MethodInvoker();
        methodInvoker.setTargetClass(instance.getClass());
        methodInvoker.setTargetObject(instance);
        methodInvoker.setTargetMethod("set" + StringUtils.capitalize(property));
        methodInvoker.setArguments(args);
        methodInvoker.prepare();
        methodInvoker.invoke();
    }

    public static Object invokeGetter(Object instance, String property, boolean isBoolean) throws NoSuchMethodException,
            ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        MethodInvoker methodInvoker = new MethodInvoker();
        methodInvoker.setTargetClass(instance.getClass());
        methodInvoker.setTargetObject(instance);
        methodInvoker.setTargetMethod((isBoolean ? "is" : "get") + StringUtils.capitalize(property));
        methodInvoker.prepare();
        return methodInvoker.invoke();
    }

}
