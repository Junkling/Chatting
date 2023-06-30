package webSocket.chatting.dto;

import webSocket.chatting.rct.KurentoUserSession;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.misc.NotNull;
import org.kurento.client.Continuation;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
@Slf4j
public class KurentoRoomDto extends ChatRoomDto implements Closeable {

    private KurentoClient kurento;

    private MediaPipeline pipeline;

    @NotNull
    private String roomId;
    private String roomName;
    private Integer userCount;
    private Integer maxUser;
    private boolean secret;

    private ConcurrentMap<String, KurentoUserSession> participants;

    public void createPipeline() {
        this.pipeline = this.kurento.createMediaPipeline();
    }

    @PreDestroy
    public void shutdown() throws IOException {
        this.close();
    }

    public Collection<KurentoUserSession> getParticipants() {
        return participants.values();
    }

    public KurentoUserSession getParticipant(String name) {
        return participants.get(name);
    }

    public KurentoUserSession join(String userName, WebSocketSession session) throws IOException {
        log.info("ROOM {}: adding participant {}", this.roomId, userName);
        final KurentoUserSession participant = new KurentoUserSession(userName, this.roomId, session, this.pipeline);
        joinRoom(participant);
        participants.put(participant.getName(), participant);
        sendParticipantNames(participant);
        userCount++;
        return participant;
    }

    public void sendParticipantNames(KurentoUserSession user) throws IOException {
        final JsonArray participantsArray = new JsonArray();

        for (final KurentoUserSession participant : this.getParticipants()) {
            if (!participant.equals(user)) {
                final JsonElement participantName = new JsonPrimitive(participant.getName());
                participantsArray.add(participantName);
            }
        }

        final JsonObject existingParticipantsMsg = new JsonObject();

        existingParticipantsMsg.addProperty("id", "existingParticipants");
        existingParticipantsMsg.add("data", participantsArray);
        log.debug("PARTICIPANT {}: sending a list of {} participants", user.getName(),
                participantsArray.size());

        user.sendMessage(existingParticipantsMsg);
    }

    private Collection<String> joinRoom(KurentoUserSession participant) {
        final JsonObject newParticipantMsg = new JsonObject();
        newParticipantMsg.addProperty("id", "newParticipantArrived");
        newParticipantMsg.addProperty("name", participant.getName());
        final List<String> participantsList = new ArrayList<>(participants.values().size());
        for (final KurentoUserSession p : participants.values()) {
            try {
                p.sendMessage(newParticipantMsg);
            } catch (IOException e) {
                log.debug("ROOM {} : participant {} could not be notified", roomId, p.getName(), e);
            }
            participantsList.add(p.getName());
        }
        return participantsList;
    }

    private void removeParticipant(String name) {
        participants.remove(name);
        log.debug("ROOM {}  notifying all users that {} is leaving the room", this.roomId, name);
        final List<String> unNotifiedParticipants = new ArrayList<>();
        final JsonObject participantLeftJson = new JsonObject();
        participantLeftJson.addProperty("id", "participantLeft");
        participantLeftJson.addProperty("name", name);
        for (final KurentoUserSession participant : participants.values()) {
            try {
                // 나간 유저의 video 를 cancel 하기 위한 메서드
//                participant.cancelVideoFrom(name);

                // 다른 유저들에게 현재 유저가 나갔음을 알리는 jsonMsg 를 전달
                participant.sendMessage(participantLeftJson);

            } catch (final IOException e) {
                unNotifiedParticipants.add(participant.getName());
            }
        }
        if (!unNotifiedParticipants.isEmpty()) {
            log.debug("ROOM {}: The users {} could not be notified that {} left the room", this.roomId,
                    unNotifiedParticipants, name);
        }
    }
    public void leave(KurentoUserSession user) throws IOException {
        log.debug("PARTICIPANT {}: Leaving room {}", user.getName(), this.roomId);
        this.removeParticipant(user.getName());

        log.info("PARTICIPANTS {} ", this.participants);

        user.close();
    }

    @Override
    public void close() throws IOException {
        for (final KurentoUserSession user : participants.values()) {
            try {
                // 유저 close
                user.close();
            } catch (IOException e) {
                log.debug("ROOM {}: Could not invoke close on participant {}", this.roomId, user.getName(),
                        e);
            }
        }

        participants.clear();

        pipeline.release(new Continuation<Void>() {

            @Override
            public void onSuccess(Void result) throws Exception {
                log.trace("ROOM {}: Released Pipeline", KurentoRoomDto.this.roomId);
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.warn("PARTICIPANT {}: Could not release Pipeline", KurentoRoomDto.this.roomId);
            }
        });

        log.debug("Room {} closed", this.roomId);
    }
}
