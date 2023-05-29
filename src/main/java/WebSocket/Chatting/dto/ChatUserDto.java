package WebSocket.Chatting.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatUserDto {
    private Long id;
    private String nickName;
    private String password;
    private String email;
    private String provider;
}
