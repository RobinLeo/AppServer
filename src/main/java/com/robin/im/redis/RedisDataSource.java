package com.robin.im.redis;

import redis.clients.jedis.ShardedJedis;

/**
 * 
 * @author jeffrey
 *
 */
public interface RedisDataSource {

    public abstract ShardedJedis getRedisClient();
    public void returnResource(ShardedJedis shardedJedis);
    public void returnResource(ShardedJedis shardedJedis, boolean broken);

}
