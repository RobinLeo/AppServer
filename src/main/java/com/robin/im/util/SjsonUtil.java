package com.robin.im.util;

import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.fastjson.JSONObject;

/**
 * 获取json串中某个属性
 * 
 * @author bob
 */
public class SjsonUtil {

    private static final AtomicLong messageId   = new AtomicLong();

    private SjsonUtil(){
    }

    public static void main(String[] args) {
        String str = "{\"FID\":22,Data:{\"UAID\":\"dd@126.com\",\"PWD\":\"7030ee1b8aa5481643b9ac899638f8d7\",Type:0,AToken:\"111\",CType:2}}";
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            // getFIDFromMsg(str);
            JSONObject jsonObj = JSONObject.parseObject(str);
            jsonObj.getString("FID");
        }
        long end = System.currentTimeMillis();
        System.out.println(end - start);
    }

    /**
     * 对特定情况下的客户端请求截取FID.为了加快处理速度，避免速度慢的json串解析。
     * 
     * @param msg 形如{"FID":13,"PID":xxxx,...}
     * @return
     */
    public static int getFIDFromMsg(final String msg) {
        int result = -1;
        if (msg != null) {
            int startPos = msg.indexOf("\"FID\":");
            int endPos = msg.indexOf(',', startPos);
            if (endPos > 0 && startPos >= 0) {
                String strFid = msg.substring(6 + startPos, endPos);
                if (strFid.length() > 0) {
                    try {
                        result = Integer.parseInt(strFid);
                    } catch (NumberFormatException e) {
                        // TODO: handle exception
                        result = -2;
                    }

                }
            }
        }
        return result;
    }

    /**
     * 对特定情况下的客户端请求截取CMI.为了加快处理速度，避免速度慢的json串解析。
     * 
     * @param msg 形如{"CMI":13,"PID":xxxx,...}
     * @return
     */
    public static long getPIDFromMsg(final String msg) {
        long result = -1;
        int startPos = msg.indexOf("\"PID\":");
        int endPos = msg.indexOf(',', startPos);
        if (endPos == -1) {
            endPos = msg.indexOf("}", startPos);
        }
        if (endPos > 0 && startPos >= 0) {
            String strFid = msg.substring(6 + startPos, endPos);
            if (strFid.length() > 0) {
                try {
                    result = Long.parseLong(strFid);
                } catch (NumberFormatException e) {
                    // TODO: handle exception
                    result = -2;
                }

            }
        }
        return result;
    }

    /**
     * 剔除CMI信息
     * 
     * @param msg 形如{"PID":13,"PID":xxxx,...}
     * @return
     */
    public static String trimPID(final String msg) {
        int startPos = msg.indexOf("\"PID\":");
        int endPos = msg.indexOf(',', startPos);
        StringBuffer sb = null;
        if (startPos >= 0) {
            if (endPos > 0) {
                sb = new StringBuffer(msg.substring(0, startPos));
                sb.append(msg.substring(endPos + 1));
            } else {
                // PID在最后
                sb = new StringBuffer(msg.substring(0, startPos - 1));
                sb.append('}');
            }
            return sb.toString();
        }
        return msg;
    }

    /**
     * 获取应答响应序号.为了加快处理速度，避免速度慢的json串解析。
     * 
     * @param msg 形如{"RPID":12345}
     * @return
     */
    public static long getRPIDFromMsg(final String msg) {
        long result = -1;
        int startPos = msg.indexOf("\"RPID\":");
        int endPos = msg.indexOf('}');
        if (endPos > 0 && startPos >= 0) {
            String strFid = msg.substring(7 + startPos, endPos);
            if (strFid.length() > 0) {
                try {
                    result = Long.parseLong(strFid);
                } catch (NumberFormatException e) {
                    // TODO: handle exception
                    result = -2;
                    e.printStackTrace();
                }

            }
        }
        return result;
    }

    /**
     * 快速提取SID，避免速度慢的json串解析。
     * 
     * @param msg 形如{"FID":13,"SID":"xxxx",...}
     * @return
     */
    public static String getSIDFromMsg(final String msg) {
        int startPos = msg.indexOf("\"SID\":\"");
        if (startPos > 0) {
            int endPos = msg.indexOf('"', startPos + 7);
            if (endPos > 0) {
                return msg.substring(startPos + 7, endPos);
            }
        }
        return null;
    }


    public static Long getMessageId() {
        return messageId.incrementAndGet();
    }

    public static String addSendSequece(final String msg, long index) {
        if (msg == null) {
            return msg;
        }

        int i = msg.indexOf('{');
        if (-1 == i) {
            return msg;
        }
        StringBuffer sb = new StringBuffer("{\"RPID\":");
        sb.append(index).append(',').append(msg.substring(i + 1));
        return sb.toString();
    }

    public static int getIntValue(final String raw, final String key) {

        return 0;
    }

    public static String getStrValue(final String raw, final String key) {

        return "";
    }
}
