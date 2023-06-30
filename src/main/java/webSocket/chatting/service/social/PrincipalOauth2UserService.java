package webSocket.chatting.service.social;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import webSocket.chatting.dto.ChatUserDto;

@Service
@Slf4j
public class PrincipalOauth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        log.info("clientRegistration 정보 [{}]", userRequest.getClientRegistration());
        log.info("accessToken 정보 [{}]", userRequest.getAccessToken().getTokenValue());

        return oAuth2UserLogin(userRequest, oAuth2User);
    }

    private OAuth2User oAuth2UserLogin(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        SocialLogin login = null;
        String provider = userRequest.getClientRegistration().getRegistrationId();
        if (provider.equals("kakao")) {
            login = new KakaoLogin(oAuth2User.getAttributes());
        } else if (provider.equals("naver")) {
            login = new NaverLogin(oAuth2User.getAttributes());
        }
        ChatUserDto user = ChatUserDto.builder()
                .nickName(login.getNickName())
                .email(login.getEmail())
                .provider(login.getProvider())
                .build();

        return new PrincipalDetails(user, oAuth2User.getAttributes(), "user");
    }
}
