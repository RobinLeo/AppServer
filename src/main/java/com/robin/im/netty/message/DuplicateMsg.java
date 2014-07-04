package com.robin.im.netty.message;

import com.alibaba.fastjson.JSONObject;
import com.robin.im.AppServerBeanFactory;
import com.robin.im.netty.connection.MyConnection;
import com.robin.im.netty.connection.MyConnectionListener;
import com.robin.im.redis.RedisClientTemplate;
import com.robin.im.redis.service.RedisDAO;
import com.robin.im.send.ResponseMsg;
import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: liuhouxiang1986@gmail.com
 * Date: 13-10-14
 * Time: 下午3:36
 * Project: Syezon
 * File Package: com.sz.lvban.task.netty.message.recv
 */
public class DuplicateMsg extends MessagePack {
	private static final String MESSAGE_NAME = "DuplicateMsg";                             // 消息名称
	private Logger logger = LoggerFactory.getLogger(DuplicateMsg.class);
	private RedisDAO redisDao;
	private RedisClientTemplate redisClientTemplate;
	private int rc = 1;
	private JSONObject resultJson   = new JSONObject();
	public DuplicateMsg(String msg, Channel channel){
		super(msg, channel);
	}
	@Override
	public void onHandler() {
		MyConnection myConnection = null;
		redisDao =  AppServerBeanFactory.getRedisDAO();
		redisClientTemplate = (RedisClientTemplate) AppServerBeanFactory.getBean("redisClientTemplate");
		rc = 0;
		uid = null;
		// 从MyConnectionListener中的connections属性中查询该socketId是否已经存在长连接，如果存在则说明该用户已经在某个客户端登录
		myConnection = MyConnectionListener.getMyConnectionBySocketId(channel.getId());// 根据socketId获取当前用户是否已经在一台机器上登录
		if (myConnection != null) {
			try {
				uid = myConnection.getChName();
				JSONObject jsonObj = JSONObject.parseObject(msg);
				Long tms = jsonObj.getLong("TMS");
				int FID = jsonObj.getIntValue("FID");
				JSONObject data = jsonObj.getJSONObject("Data");
				int type = data.getIntValue("Type");
				resultJson.put("RC", rc);
				resultJson.put("TMS", tms);
				resultJson.put("FID", FID);
				JSONObject resultDataJson = new JSONObject();
				resultDataJson.put("type",type);
				resultJson.put("Data", resultDataJson);
				if (logger.isDebugEnabled()) {
					if (0 == rc) {
						logger.debug("create duplicate success,uid=" + uid + ",socketId=" + channel.getId() + ",addr="
								          + channel.getRemoteAddress());
					} else if (1 == rc) {
						logger.debug("create duplicate failed,msg=" + msg + ",client=" + channel.getRemoteAddress());
					} else {
						logger.debug("create duplicate error,msg=" + msg);
					}
				}
				MessageManager.addSendMessage(new ResponseMsg(resultJson.toJSONString(), uid));

			} catch (Exception e) {
				logger.error("create duplicate message exception， e:" , e);
			}
		}
	}

	@Override
	public String getName() {
		return MESSAGE_NAME;
	}

	/**
	 * 消息成功下发后的后继处理
	 */
	@Override
	public void onWriteSuccess() {
	}

	/**
	 * 消息发送失败后继处理
	 */
	@Override
	public void onWriteFailed() {
	}

	/**
	 * 获取发送包号
	 *
	 * @return 获取发送的包号
	 */
	@Override
	public Long getRPID() {
		return null;
	}
}
