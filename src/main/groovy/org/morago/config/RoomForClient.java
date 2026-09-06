package org.morago.config;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.ConnectListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomForClient implements ConnectListener {
    @Override
    public void onConnect(SocketIOClient client) {
        Long id = client.get("userId");

        if (id == null){
            client.disconnect();
            return;
        }

        String room = String.valueOf(id);
        client.joinRoom(room);
    }
}
