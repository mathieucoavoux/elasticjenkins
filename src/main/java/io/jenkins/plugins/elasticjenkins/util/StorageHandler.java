package io.jenkins.plugins.elasticjenkins.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StorageHandler implements InvocationHandler {

    private static final Logger LOGGER = Logger.getLogger(StorageHandler.class.getName());

    private Object classInstance;

    public StorageHandler(Object classInstance) {
        this.classInstance = classInstance;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = null;
        try {
            result = method.invoke(classInstance, args);
        }catch (Exception e){
            LOGGER.log(Level.SEVERE,"Can not invoke method. Exception:",e);
        }
        return result;
    }
}
