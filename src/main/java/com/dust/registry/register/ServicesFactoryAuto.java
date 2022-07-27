package com.dust.registry.register;

import com.dust.config.Config;
import com.dust.registry.NacosServiceRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 提供service本地注册查询和nacos注册方法
 */
public class ServicesFactoryAuto {

    static Properties properties;
    static Map<Class<?>, Object> map = new ConcurrentHashMap<>();

    //本地注册和nacos注册
    public static void register(Class<?> interfaceClass, Class<?> instanceClass){

        try(InputStream in = Config.class.getResourceAsStream("/application.properties")) {
            properties = new Properties();
            properties.load(in);
            map.put(interfaceClass, instanceClass.newInstance());
            new NacosServiceRegistry().register(
                    //通过配置文件获取注册服务名称
                    interfaceClass.getCanonicalName(),
                    //通过配置文件获取注册服务地址
                    new InetSocketAddress(properties.getProperty("server.hostname"),
                            Integer.parseInt(properties.getProperty("server.port")))
            );
        } catch (InstantiationException | IllegalAccessException | IOException e) {
            e.printStackTrace();
        }


    }

    public static <T> T getService(Class<T> interfaceClass) {
        return (T) map.get(interfaceClass);
    }
}
