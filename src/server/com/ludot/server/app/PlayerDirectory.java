package com.ludot.server.app;

import com.ludot.protocol.ErrorCode;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class PlayerDirectory {

    private static final Pattern NICKNAME = Pattern.compile("[A-Za-z0-9_]{3,16}");

    private static final class Entry {
        final PlayerIdentity identity;
        final boolean reconnectable;
        ClientSession session;      // the connection using this identity now; null before attach and while detached
        volatile boolean detached;  // dropped, waiting out the grace period
        boolean removed;
        ScheduledFuture<?> expiry;

        Entry(PlayerIdentity identity, boolean reconnectable) {
            this.identity = identity;
            this.reconnectable = reconnectable;
        }
    }

    private final Map<String, Entry> byNickname = new ConcurrentHashMap<>();
    private final Map<String, Entry> byToken = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;
    private final long graceMs;

    public PlayerDirectory() {
        this(null, 0);
    }

    public PlayerDirectory(ScheduledExecutorService scheduler, long graceMs) {
        this.scheduler = scheduler;
        this.graceMs = graceMs;
    }

    public PlayerIdentity register(String nickname) {
        return register(nickname, false);
    }

    public PlayerIdentity register(String nickname, boolean reconnectable) {
        if (!NICKNAME.matcher(nickname).matches()) {
            throw new RequestRejected(ErrorCode.INVALID_NICKNAME, "Use 3 to 16 letters, digits or underscores");
        }
        PlayerIdentity identity = new PlayerIdentity(UUID.randomUUID().toString(), nickname, UUID.randomUUID().toString());
        Entry entry = new Entry(identity, reconnectable);
        // putIfAbsent is atomic, so two simultaneous Hellos for one nickname cannot both succeed
        if (byNickname.putIfAbsent(key(nickname), entry) != null) {
            throw new RequestRejected(ErrorCode.NICKNAME_IN_USE, nickname + " is already in use");
        }
        byToken.put(identity.token(), entry);
        return identity;
    }

    void attach(PlayerIdentity identity, ClientSession session) {
        Entry entry = entryOf(identity);
        if (entry != null) {
            synchronized (entry) {
                entry.session = session;
            }
        }
    }

    // if it was the identity's current connection and the identity may reconnect, keep it for the grace period; otherwise free it
    boolean detach(PlayerIdentity identity, ClientSession session) {
        Entry entry = entryOf(identity);
        if (entry == null) {
            return false;
        }
        synchronized (entry) {
            if (entry.detached || (entry.session != null && entry.session != session)) {
                return true; // already waiting, or taken over by a newer connection
            }
            if (!entry.reconnectable || graceMs <= 0 || scheduler == null) {
                remove(entry);
                return false;
            }
            entry.session = null;
            entry.detached = true;
            entry.expiry = scheduler.schedule(() -> expire(entry), graceMs, TimeUnit.MILLISECONDS);
            return true;
        }
    }

    // if the old connection is still attached (a half-open socket the server has not noticed is dead) it is closed
    Optional<PlayerIdentity> reattach(String token, ClientSession session) {
        Entry entry = byToken.get(token);
        if (entry == null || !entry.reconnectable) {
            return Optional.empty();
        }
        ClientSession previous;
        synchronized (entry) {
            if (entry.removed) {
                return Optional.empty();
            }
            cancel(entry);
            entry.detached = false;
            previous = entry.session;
            entry.session = session;
        }
        if (previous != null && previous != session) {
            previous.sink().close(); // outside the lock: closing runs that connection's own clean-up
        }
        return Optional.of(entry.identity);
    }

    // frees the identity now (a polite sign-out, or a client that cannot reconnect)
    public void release(PlayerIdentity identity) {
        Entry entry = entryOf(identity);
        if (entry != null) {
            synchronized (entry) {
                remove(entry);
            }
        }
    }

    public int connectedCount() {
        return (int) byNickname.values().stream().filter(entry -> !entry.detached).count();
    }

    public int detachedCount() {
        return (int) byNickname.values().stream().filter(entry -> entry.detached).count();
    }

    private void expire(Entry entry) {
        synchronized (entry) {
            if (entry.detached && !entry.removed) {
                remove(entry);
            }
        }
    }

    // callers hold the entry's lock
    private void remove(Entry entry) {
        entry.removed = true;
        cancel(entry);
        byNickname.remove(key(entry.identity.nickname()), entry);
        byToken.remove(entry.identity.token(), entry);
    }

    private static void cancel(Entry entry) {
        if (entry.expiry != null) {
            entry.expiry.cancel(false);
            entry.expiry = null;
        }
    }

    private Entry entryOf(PlayerIdentity identity) {
        Entry entry = byNickname.get(key(identity.nickname()));
        return entry != null && entry.identity == identity ? entry : null;
    }

    private static String key(String nickname) {
        return nickname.toLowerCase(Locale.ROOT);
    }
}
