package webSocket.chatting.service.social;

import java.util.HashMap;
import java.util.Map;

public class KakaoLogin implements SocialLogin {
    private Map<String, Object> kakaoAttributes;

    public KakaoLogin(Map<String, Object> kakaoAttributes) {
        this.kakaoAttributes = kakaoAttributes;
    }

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getEmail() {
        HashMap<String, Object> account = (HashMap<String, Object>) kakaoAttributes.get("kakao_account");
        String email = (String) account.get("email");

        return email;
    }

    @Override
    public String getNickName() {
        HashMap<String, Object> properties = (HashMap<String, Object>) kakaoAttributes.get("properties");
        String nickName = (String) properties.get("nickname");
        return nickName;
    }
}
