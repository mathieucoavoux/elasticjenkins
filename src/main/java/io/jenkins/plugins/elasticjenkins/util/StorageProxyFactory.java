package io.jenkins.plugins.elasticjenkins.util;

import javax.naming.ConfigurationException;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StorageProxyFactory {

    private static final Logger LOGGER = Logger.getLogger(StorageProxyFactory.class.getName());

    /**
     * Instantiate a new Object specify in parameter
     * @return Instantiated object
     * @throws ClassNotFoundException if the interface to proxy is not found or is not an interface
     * @throws ConfigurationException if the plugin has not been configured properly we throw this exception
     */
    public static Object newInstance(Class interfaceName) throws ClassNotFoundException, ConfigurationException {
        //Check if the interface exists
        if(!interfaceName.isInterface()) {
            LOGGER.log(Level.SEVERE,"The interface "+interfaceName+" does not exists or is not an interface type");
            throw new ClassNotFoundException();
        }

        //Get the implementation class
        Object assetType = StorageLookup.getStorage(interfaceName);
        if(assetType == null) {
            LOGGER.log(Level.SEVERE,"Backend storage type has NOT been configured yet. Please configure the plugin correctly");
            throw new ConfigurationException();
        }
        try {
            return Proxy.newProxyInstance(assetType.getClass().getClassLoader(),new Class[]{interfaceName},new StorageHandler(assetType));
        }catch (Exception e) {
            LOGGER.log(Level.SEVERE,"Unhandled exception:",e);
            return null;
        }

    }
}
