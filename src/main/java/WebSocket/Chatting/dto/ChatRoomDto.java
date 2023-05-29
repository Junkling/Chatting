package WebSocket.Chatting.dto;

import lombok.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatRoomDto {
    @NonNull
    private String roomId;
    private String roomName;
    private Integer userCount;
    private Integer maxUser;

    private String roomPW;
    private boolean secret;

    public ConcurrentMap<String, ?> userList = new ConcurrentHashMap<>();

}
