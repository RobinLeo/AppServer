package com.robin.im.redis;

public final class KeyGeneration {

    public static final String MSG_ID_GENERATOR     = "MSG_ID_GEN";

    public static int          SESSION_EXPIRED_TIME = 18000;       // 60*60

    public static int          MSG_EXPIRED_TIME     = 604800;      // (7*24*60*60)一周

    public static int          RPID_EXPIRED_TIME    = 60;

    enum KeyConstant {
        SESSION_KEY("S_K:"), OFFLINE_MSG_KEY("OFFMSG_K:"), HOST_MSG_KEY("MSG_K:"), USER_PIDS("UP#"),WAIT_4_ACK("RPID_K:");

        private String v;

        KeyConstant(String v){
            this.v = v;
        }
    }


    /**
     * Map<sessionId,uid>
     * 
     * @param sessionId
     * @return
     */
    public static String sessionKey(String sessionId) {
        StringBuilder buf = new StringBuilder();
        buf.append(KeyConstant.SESSION_KEY.v);
        buf.append(sessionId);
        return buf.toString();
    }


    /**
     * 离线消息key
     * 
     * @param uid
     * @return
     */
    public static String offlineMsgKey(String uid) {
        StringBuilder buf = new StringBuilder(KeyConstant.OFFLINE_MSG_KEY.v);
        buf.append(uid);
        return buf.toString();
    }

    public static String msgKey(String msgId) {
        StringBuilder buf = new StringBuilder();
        buf.append(KeyConstant.HOST_MSG_KEY.v).append(msgId);
        return buf.toString();
    }


    private static String keyGenerator(KeyConstant constant, String param) {
        StringBuilder sb = new StringBuilder(constant.v);
        return sb.append(param).toString();
    }


    public static String wait4ACK(long rpid) {
        StringBuilder buf = new StringBuilder(KeyConstant.WAIT_4_ACK.v);
        return buf.append(rpid).toString();
    }

    public static String userPid(String uid) {
        StringBuilder buf = new StringBuilder(KeyConstant.USER_PIDS.v);
        buf.append(uid);
        return buf.toString();
    }

    public static void main(String[] args) {

    }
}
