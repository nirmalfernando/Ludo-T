package com.ludot.server.app;

import com.ludot.protocol.ErrorCode;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** Hands out identities and keeps nicknames unique among connected people. Safe to call from any connection thread. */
public final class PlayerDirectory {

    private static final Pattern NICKNAME = Pattern.compile("[A-Za-z0-9_]{3,16}");

    private final Map<String, PlayerIdentity> byNickname = new ConcurrentHashMap<>();

    /** Registers a nickname (case-insensitively unique) or throws INVALID_NICKNAME / NICKNAME_IN_USE. */
    public PlayerIdentity register(String nickname) {
        if (!NICKNAME.matcher(nickname).matches()) {
            throw new RequestRejected(ErrorCode.INVALID_NICKNAME, "Use 3 to 16 letters, digits or underscores");
        }
        PlayerIdentity identity = new PlayerIdentity(UUID.randomUUID().toString(), nickname, UUID.randomUUID().toString());
        // putIfAbsent is atomic, so two simultaneous Hellos for one nickname cannot both succeed
        if (byNickname.putIfAbsent(key(nickname), identity) != null) {
            throw new RequestRejected(ErrorCode.NICKNAME_IN_USE, nickname + " is already in use");
        }
        return identity;
    }

    /** Frees the nickname when its owner disconnects. */
    public void release(PlayerIdentity identity) {
        byNickname.remove(key(identity.nickname()), identity);
    }

    public int connectedCount() {
        return byNickname.size();
    }

    private static String key(String nickname) {
        return nickname.toLowerCase(Locale.ROOT);
    }
}
