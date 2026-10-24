package com.iloelias.dimensionalanchors;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.SavedDataStorage;

public final class PlayerRoomSavedData extends SavedData {
    private static final int DATA_VERSION = 1;
    private static final Codec<PlayerRoomSavedData> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(DATA_VERSION, DATA_VERSION).fieldOf("data_version").forGetter(data -> DATA_VERSION),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, PlayerRoomSession.CODEC)
                    .fieldOf("sessions").forGetter(data -> data.sessions),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, PlayerTransition.CODEC)
                    .optionalFieldOf("transitions", Map.of()).forGetter(data -> data.transitions)
    ).apply(instance, (version, sessions, transitions) -> new PlayerRoomSavedData(sessions, transitions)));
    public static final Codec<PlayerRoomSavedData> CODEC = SavedDataSupport.strictCodec(RAW_CODEC);
    public static final SavedDataType<PlayerRoomSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(DimensionalAnchors.MODID, "player_rooms"),
            PlayerRoomSavedData::new, CODEC);

    private final Map<UUID, PlayerRoomSession> sessions;
    private final Map<UUID, PlayerTransition> transitions;
    private final Set<UUID> activeTravel = new HashSet<>();
    private final RoomMovementEnforcement movement = new RoomMovementEnforcement();

    private PlayerRoomSavedData() {
        this(Map.of(), Map.of());
    }

    private PlayerRoomSavedData(Map<UUID, PlayerRoomSession> sessions, Map<UUID, PlayerTransition> transitions) {
        this.sessions = new HashMap<>(sessions);
        this.transitions = new HashMap<>(transitions);
    }

    public static PlayerRoomSavedData get(ServerLevel level) {
        var server = level.getServer();
        return load(server.overworld().getDataStorage(), server.getWorldPath(LevelResource.DATA));
    }

    static PlayerRoomSavedData load(SavedDataStorage storage, Path directory) {
        return SavedDataSupport.load(storage, directory, TYPE);
    }

    public Optional<PlayerRoomSession> find(UUID playerId) {
        return Optional.ofNullable(sessions.get(Objects.requireNonNull(playerId, "playerId")));
    }

    public void enter(UUID playerId, UUID anchorId, ExteriorReturn exteriorReturn) {
        Objects.requireNonNull(playerId, "playerId");
        var session = new PlayerRoomSession(anchorId, exteriorReturn);
        if (sessions.putIfAbsent(playerId, session) != null) {
            throw new IllegalStateException("Player already has a room session: " + playerId);
        }
        setDirty();
    }

    public void changeRoom(UUID playerId, UUID anchorId) {
        var current = requireSession(playerId);
        var next = new PlayerRoomSession(anchorId, current.exteriorReturn());
        if (!next.equals(current)) {
            sessions.put(playerId, next);
            setDirty();
        }
    }

    public void completeExit(UUID playerId, UUID expectedAnchorId) {
        var current = requireSession(playerId);
        if (!current.anchorId().equals(Objects.requireNonNull(expectedAnchorId, "expectedAnchorId"))) {
            throw new IllegalStateException("Player changed rooms before exit completion: " + playerId);
        }
        sessions.remove(playerId);
        transitions.remove(playerId);
        movement.forget(playerId);
        setDirty();
    }

    void completeExitIfUnchanged(UUID playerId, PlayerRoomSession expectedSession) {
        // Identity rejects a session replaced during hooks, even with identical saved values.
        if (requireSession(playerId) != Objects.requireNonNull(expectedSession, "expectedSession")) {
            throw new IllegalStateException("Player session changed during travel: " + playerId);
        }
        sessions.remove(playerId);
        transitions.remove(playerId);
        movement.forget(playerId);
        setDirty();
    }

    Optional<PlayerTransition> transition(UUID playerId) {
        return Optional.ofNullable(transitions.get(Objects.requireNonNull(playerId, "playerId")));
    }

    void recordTransition(UUID playerId, PlayerTransition transition) {
        Objects.requireNonNull(transition, "transition");
        var session = find(playerId).orElse(null);
        if (transition.type() == PlayerTransition.Type.ENTRY ? session != null
                : session == null || !transition.sourceRoom().orElseThrow().equals(session.anchorId())) {
            throw new IllegalStateException("Transition source does not match the player's room: " + playerId);
        }
        var previous = transitions.get(playerId);
        if (previous != null && !previous.operationId().equals(transition.operationId())) {
            throw new IllegalStateException("Player has an unresolved transition: " + playerId);
        }
        transitions.put(playerId, transition);
        setDirty();
    }

    /** Applies a confirmed entry or room change and clears its journal entry in one step. */
    PlayerRoomSession completeTransition(UUID playerId, PlayerTransition transition) {
        if (!transition.equals(transitions.get(Objects.requireNonNull(playerId, "playerId")))) {
            throw new IllegalStateException("Transition is no longer current: " + playerId);
        }
        var target = transition.targetRoom().orElseThrow(() -> new IllegalStateException("Exit uses completeExit"));
        var current = find(playerId).orElse(null);
        boolean matches = transition.type() == PlayerTransition.Type.ENTRY ? current == null
                : current != null && current.anchorId().equals(transition.sourceRoom().orElseThrow());
        if (!matches) {
            throw new IllegalStateException("Player session changed during travel: " + playerId);
        }
        var next = new PlayerRoomSession(target, transition.exteriorReturn());
        sessions.put(playerId, next);
        transitions.remove(playerId);
        movement.forget(playerId);
        setDirty();
        return next;
    }

    void cancelTransition(UUID playerId, UUID operationId) {
        var current = transitions.get(Objects.requireNonNull(playerId, "playerId"));
        if (current != null && current.operationId().equals(operationId)) {
            transitions.remove(playerId);
            setDirty();
        }
    }

    boolean beginTravel(UUID playerId) {
        return activeTravel.add(Objects.requireNonNull(playerId, "playerId"));
    }

    boolean isTravelActive(UUID playerId) {
        return activeTravel.contains(playerId);
    }

    RoomMovementEnforcement movement() {
        return movement;
    }

    void endTravel(UUID playerId) {
        activeTravel.remove(playerId);
    }

    private PlayerRoomSession requireSession(UUID playerId) {
        return find(playerId).orElseThrow(() -> new IllegalStateException("Player has no room session: " + playerId));
    }
}
