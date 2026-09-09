package org.morago.event;

import java.util.List;

public record SocketNotificationEvent(List<String> rooms, String eventName, Object[] data) {

    public static SocketNotificationEvent of(String room, String eventName, Object... data) {
        return new SocketNotificationEvent(List.of(room), eventName, data);
    }

    public static SocketNotificationEvent of(List<String> rooms, String eventName, Object... data) {
        return new SocketNotificationEvent(rooms, eventName, data);
    }
}