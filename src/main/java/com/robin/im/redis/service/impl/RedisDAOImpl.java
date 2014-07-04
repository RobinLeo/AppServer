package com.robin.im.redis.service.impl;

import com.robin.im.redis.KeyGeneration;
import com.robin.im.redis.RedisClientTemplate;
import com.robin.im.redis.service.RedisDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Tuple;

import java.util.Set;


@Repository("redisDAO")
public class RedisDAOImpl implements RedisDAO {

    @Autowired
    private RedisClientTemplate redisClient;

    @Override
    public String getUserInfoBySessionId(String sid) {
        String redisKey = KeyGeneration.sessionKey(sid);
        String userInfo = redisClient.get(redisKey);
        return userInfo;
    }

    @Override
    public String saveMessage(String msgId, String content) {

        return redisClient.setex(KeyGeneration.msgKey(msgId),
                KeyGeneration.MSG_EXPIRED_TIME, content);
    }

    @Override
    public void removeOfflineMsg(String userId, String msgId) {
        String key = KeyGeneration.offlineMsgKey(userId);
        redisClient.zrem(key, msgId);
    }

    @Override
    public String getMessageByMsgId(String msgId) {
        return redisClient.get(KeyGeneration.msgKey(msgId));
    }

    public void removeMessageByMsgId(String msgId) {
        redisClient.del(KeyGeneration.msgKey(msgId));

    }

    public void putOfflineMsg(String userId, String msgId, long timeMillis) {
        String key = KeyGeneration.offlineMsgKey(userId);
        redisClient.zadd(key, timeMillis, msgId);

    }

    public Set<Tuple> getOfflineMsgIds(String userId) {
        String key = KeyGeneration.offlineMsgKey(userId);
        return redisClient.zrangeWithScores(key, 0, -1);
    }



    @Override
    public Long getMsgId() {
        return redisClient.incr(KeyGeneration.MSG_ID_GENERATOR);
    }

    @Override
    public String getUpdateStatus(String key) {
        return redisClient.get(key);
    }

    @Override
    public void addWait4ACKMsg(long rpid, String jsonMsg) {
        String key = KeyGeneration.wait4ACK(rpid);
        redisClient.setex(key, KeyGeneration.RPID_EXPIRED_TIME, jsonMsg);
    }

    @Override
    public String removeACKMsg(long rpid) {

        return null;
    }


    public boolean isPidInCache(String uid, Long pid) {
        String key = KeyGeneration.userPid(uid);
        Long result = redisClient.sadd(key, pid.toString());
        redisClient.expire(key, 1800);
        return result == 0L;
    }

    @Override
    public String getQuietTime(String timeKey) {
        return redisClient.get(timeKey);
    }

}
