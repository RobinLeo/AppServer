package com.robin.im;

import com.robin.im.redis.service.RedisDAO;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created with IntelliJ IDEA.
 * User: liuhouxiang@1986@gmail.com.
 * Date: 2014/7/3 14:37
 * Project: AppServer
 */
public class AppServerBeanFactory {
    private static ApplicationContext context;

    private static byte[] bytesync = new byte[0];

    private static RedisDAO redisDAO;

    public static ApplicationContext getContextInstance(){
        if(null == context){
            synchronized (bytesync){
                if(context != null){
                    return context;
                }

                context = new ClassPathXmlApplicationContext("classpath:/applicationContext-task.xml");
            }
        }
        return context;
    }

    public static Object getBean(String beanName) {
        return AppServerBeanFactory.getContextInstance().getBean(beanName);
    }

    public static RedisDAO getRedisDAO(){
        if(redisDAO == null){
            redisDAO = (RedisDAO) AppServerBeanFactory.getBean("redisDAO");
        }
        return redisDAO;
    }
}
