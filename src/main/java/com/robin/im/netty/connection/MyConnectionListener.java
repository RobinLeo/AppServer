package com.robin.im.netty.connection;

import org.jboss.netty.channel.ExceptionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MyConnectionListener {

    private static final Logger                               log                    = LoggerFactory.getLogger(MyConnectionListener.class);
    private static final ConcurrentMap<Integer, MyConnection> connections            = new ConcurrentHashMap<Integer, MyConnection>();
    private static final ConcurrentMap<String, MyConnection>  connectionsWithNameKey = new ConcurrentHashMap<String, MyConnection>();

    public static void connectionCreated(final MyConnection connection) {
        if (connections.putIfAbsent(connection.getID(), connection) != null) {
            throw new IllegalArgumentException("Connection already exists with id " + connection.getID());
        }

    }

    public static void connectionDestroyed(final Integer connectionID) {
        MyConnection connection = connections.get(connectionID);
        if (connection != null) {
            if (connection.getChName() != null) {
                MyConnection namedConnection = connectionsWithNameKey.get(connection.getChName());
                if (namedConnection != null && namedConnection.getID() == connection.getID()) {
                    connectionsWithNameKey.remove(connection.getChName());
                    if (log.isTraceEnabled()) {
                        log.trace("remove connection from connegetConnectionsctionsWithNameKey,conn="
                                  + connection.getChName());
                    }
                }
            }
        }
        connections.remove(connectionID);
        if (log.isTraceEnabled()) {
            log.trace("connections removed connectionID=" + connectionID);
        }
    }

    public static void connectionException(final Object connectionID, final ExceptionEvent e) {
        // Execute on different thread to avoid deadlocks
        MyConnection connection = connections.get(connectionID);
        if (connection != null) {
            connection.setValid(false);
        }
    }

    public static MyConnection getMyConnectionBySocketId(Integer connectionID) {
        return connections.get(connectionID);
    }

    /**
     * 把经过验证的长连接以用户名作为key保存 返回值null说明这个用户没有在其它地方登录，非空表示在其它地方登录着，推下线消息
     **/
    public static MyConnection addNamedConnection(final MyConnection connection) {
        MyConnection oldConnection = connectionsWithNameKey.get(connection.getChName());
        connectionsWithNameKey.put(connection.getChName(), connection);
        if (log.isTraceEnabled()) {
            log.trace("add connection=" + connection.getChName() + ",connection=" + connection.getRemoteAddress());
        }
        return oldConnection;
    }

    public static MyConnection getMyConnectionByName(String name) {
        return connectionsWithNameKey.get(name);
    }

    public static ConcurrentMap<Integer, MyConnection> getConnections() {
        return connections;
    }

    public static ConcurrentMap<String, MyConnection> getNamedConnections() {
        return connectionsWithNameKey;
    }

}
