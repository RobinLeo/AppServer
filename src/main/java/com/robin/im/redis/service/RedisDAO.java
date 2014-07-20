package com.robin.im.redis.service;

import redis.clients.jedis.Tuple;

import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: liuhouxiang1986@gmail.com.
 * Date: 2014/7/3 15:02
 * Project: AppServer
 */
public interface RedisDAO {
    /**
     * 通过sessionId获取用户信息
     * @return
     */
    public String getUserInfoBySessionId(String sid);

    /**
     * 消息保存至redis
     * @param msgId
     * @param content
     * @return
     */
    public String saveMessage(String msgId, String content);

    /**
     * 根据msgId获取保存在redis里面的信息
     * @param msgId
     * @return
     */
    public String getMessageByMsgId(String msgId);
    public void removeMessageByMsgId(String msgId);
    /**
     * 发送成功后移除离线消息队列中的该条消息
     * @param userId
     * @param msgId
     */
    public void removeOfflineMsg(String userId, String msgId);

    /**
     * 将用户的消息离线消息放入离线消息队列
     * @param userId
     * @param msgId
     */
    public void putOfflineMsg(String userId, String msgId, long timeMillis);

    /**
     *
     * @param userId
     * @return
     */
    public Set<Tuple> getOfflineMsgIds(String userId);

    /**
     * 服务端产生msgid,和rpid
     * @return
     */
    public Long getMsgId();

    public String getUpdateStatus(String key);

    public void addWait4ACKMsg(long rpid, String jsonMsg);

    public String removeACKMsg(long rpid);


    public  boolean isPidInCache(String key, Long pid);

    public String getQuietTime(String timeKey);


}
