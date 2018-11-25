package io.jenkins.plugins.elasticjenkins.util;

import java.lang.reflect.Proxy;
import java.lang.reflect.Type;

public class StorageProxyFactory {

    /**
     * Instanciate a new Object specify in parameter
     * @return Instantiated object
     */
    public static Object newInstance() {
        //Get the implementation class
        Object assetType = StorageLookup.getLogStorage();
        try {
            return Proxy.newProxyInstance(assetType.getClass().getClassLoader(),new Class[]{StorageInterface.class},new StorageHandler(assetType));
        }catch (Exception e) {
            return null;
        }

    }
}
