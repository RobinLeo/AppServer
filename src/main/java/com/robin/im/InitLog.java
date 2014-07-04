package com.robin.im;

import com.alibaba.citrus.logconfig.LogConfigurator;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: liuhouxiang@1986@gmail.com.
 * Date: 2014/7/3 13:17
 * Project: AppServer
 */
public class InitLog {

    public void init(){
        Map<String, String> params;
        String[] logSystems = new String[]{"logback"};
        String[] resources = new String[]{"logback.xml"};
        LogConfigurator[] logConfigurators = LogConfigurator.getConfigurators(logSystems);

        for(int i = 0; i < logConfigurators.length ; i++){
            URL logConfigurationResource = this.getClass().getClassLoader().getResource(resources[i]);

            params = logConfigurators[i].getDefaultProperties();

            logConfigurators[i].configure(logConfigurationResource,params);

            System.out.println("log system ["+logSystems[i]+"] is inited");
        }
    }

}
