package webSocket.chatting.rct;

import com.google.gson.JsonObject;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@RequiredArgsConstructor
@Slf4j
@Data
public class KurentoUserSession implements Closeable {
    private final String name;
    private final String roomName;
    private final WebSocketSession session;
    private final MediaPipeline pipeline;

    private final WebRtcEndpoint output;

    private final ConcurrentMap<String, WebRtcEndpoint> input = new ConcurrentHashMap<>();

    public KurentoUserSession(String name, String roomName, WebSocketSession session, MediaPipeline pipeline) {
        this.pipeline = pipeline;
        this.session = session;
        this.name = name;
        this.roomName = roomName;
        this.output = new WebRtcEndpoint.Builder(pipeline).build();
        this.output.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>(){
            @Override
            public void onEvent(IceCandidateFoundEvent event) {
                // JsonObject 생성
                JsonObject response = new JsonObject();
                // id : iceCnadidate, id 는 ice후보자 선정
                response.addProperty("id", "iceCandidate");
                // name : 유저명
                response.addProperty("name", name);

                // add 랑 addProperty 랑 차이점?
                // candidate 를 key 로 하고, IceCandidateFoundEvent 객체를 JsonUtils 를 이용해
                // json 형태로 변환시킨다 => toJsonObject 는 넘겨받은 Object 객체를 JsonObject 로 변환
                response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));

                try {
                    /** synchronized 안에는 동기화 필요한 부분 지정*/
                    // 먼저 동기화는 프로세스(스레드)가 수행되는 시점을 조절하여 서로가 알고 있는 정보가 일치하는 것
                    // 여기서는 쉽게 말해 onEvent 를 통해서 넘어오는 모든 session 객체에게 앞에서 생성한 response json 을
                    // 넘겨주게되고 이를 통해서 iceCandidate 상태를 '일치' 시킨다? ==> 여긴 잘 모르겟어요...
                    synchronized (session) {
                        session.sendMessage(new TextMessage(response.toString()));
                    }
                } catch (IOException e) {
                    log.debug(e.getMessage());
                }
            }
        });
    }


    @Override
    public void close() throws IOException {
        log.debug("PARTICIPANT {}: ", this.name);
        for (final String s : input.keySet()) {
            log.trace("PARTICIPANT {}: Released incoming EP for{}", this.name, s);
            final WebRtcEndpoint ep = this.input.get(s);
            ep.release(new Continuation<Void>() {
                @Override
                public void onSuccess(Void result) throws Exception {
                    log.trace("PARTICIPANT {} : Released successfully input EP for {}", KurentoUserSession.this.name, s);
                }

                @Override
                public void onError(Throwable cause) throws Exception {
                    log.warn("PARTICIPANT {} : Released fail input EP for {}", KurentoUserSession.this.name, s);
                }
            });
        }
        output.release(new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                log.trace("PARTICIPANT {} : Released output EP for {}", KurentoUserSession.this.name);
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.warn("PARTICIPANT {} : Released fail output EP for {}", KurentoUserSession.this.name);
            }
        });
    }

    public void sendMessage(JsonObject message) throws IOException {
        log.debug("USER {}: SendingMessage {}", name, message);
        synchronized (session) {
            session.sendMessage(new TextMessage(message.toString()));
        }
    }

    public void addCandidate(IceCandidate candidate, String name) {
        if (this.name.compareTo(name) == 0) {
            output.addIceCandidate(candidate);
        }else {
            WebRtcEndpoint webRtc = input.get(name);
            if (webRtc != null) {
                webRtc.addIceCandidate(candidate);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KurentoUserSession that = (KurentoUserSession) o;
        return Objects.equals(getName(), that.getName()) && Objects.equals(getRoomName(), that.getRoomName()) && getSession().equals(that.getSession());
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + name.hashCode();
        result = 31 * result + roomName.hashCode();
        return result;
    }
}
