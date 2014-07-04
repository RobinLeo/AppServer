/**
 */
package com.robin.im.netty.message;

import org.jboss.netty.channel.Channel;
import org.springframework.context.ApplicationContext;

public abstract class MessagePack {
	protected String msg;//消息体
	protected Channel channel;//消息通道
	protected String uid; // 发送给消息的用户id
	protected int fId;//服务唯一标识
	protected long rpid;//消息确认码
	protected int packRetryCount;

	public MessagePack() {
	}
	
	public MessagePack(String msg) {
	    this.msg = msg;
	}
	
	/**
	 * use for socket message
	 * 
	 * @param msg
	 * @param channel
	 */
	public MessagePack(String msg, Channel channel) {
		this.msg = msg;
		this.channel = channel;
	}

	
    public void setUid(String uid) {
        this.uid = uid;
    }
	
    public String getUid() {
        return uid;
    }
	
    public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	public abstract void onHandler();//实际的处理入口

	public abstract String getName();
	
	  
    /**
     * 消息成功下发后的后继处理
     */
    public abstract void onWriteSuccess();
    
    /**
     * 消息发送失败后继处理
     */
    public abstract void onWriteFailed();
    
    /**
     * 获取发送包号
     * @return  获取发送的包号
     */
    public abstract Long getRPID();

}
