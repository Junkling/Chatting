package webSocket.chatting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class FileUploadDto {
    private MultipartFile file;
    private String originFileName;
    private String transaction;
    private String chatRoom;
    private String DateUrl;
    private String fileDir;

}
