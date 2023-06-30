package webSocket.chatting.service.social;

import webSocket.chatting.dto.ChatUserDto;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
@Data
public class PrincipalDetails implements UserDetails, OAuth2User {
    private ChatUserDto userDto;
    private Map<String, Object> attributes;
    private String provider;

    // 일반 유저
    public PrincipalDetails(ChatUserDto user, String provider) {
        this.userDto = user;
        this.provider = provider;
    }

    // 소셜 로그인 유저
    public PrincipalDetails(ChatUserDto user, Map<String, Object> attributes, String provider){
        this.userDto = user;
        this.attributes = attributes;
        this.provider = provider;
    }

    @Override
    public String getName() {
        return userDto.getNickName();
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection< GrantedAuthority> role = new ArrayList<>();

        role.add(new GrantedAuthority(){

            @Override
            public String getAuthority() {
                return "user";
            }
        });

        return role;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return userDto.getNickName();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
