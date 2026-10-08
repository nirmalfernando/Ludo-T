package com.ludot.protocol;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** One valid instance of every message type, shared by the protocol tests. */
final class Samples {

    static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private Samples() {
    }

    static PieceDto piece() {
        return new PieceDto(Colour.RED, 0, Location.TRACK, 30, 0, Direction.CLOCKWISE,
                EffectType.NONE, 0, 0, false);
    }

    static BoardDto board() {
        return new BoardDto(List.of(piece()), List.of(new MysteryCellDto(7, 3)),
                Map.of(Colour.RED, 1), 4);
    }

    static TurnInfo turn() {
        return new TurnInfo(12, Colour.RED, Phase.AWAITING_ROLL, null, 1_700_000_000_000L);
    }

    static ServerMessage.ChatMessage chat() {
        return new ServerMessage.ChatMessage("R-0001", 303, "p1", "alice", Role.PLAYER,
                Colour.RED, "gg", 1L, false);
    }

    static MoveOption option() {
        return new MoveOption(0, MoveKind.CAPTURE, 2, -1, Location.TRACK, 30, Location.TRACK, 33,
                true, 1, false, false, false, false, false, "R3 captures G1");
    }

    static List<ClientMessage> clientMessages() {
        return List.of(
                new ClientMessage.Ping(),
                new ClientMessage.Hello(ID, "alice"),
                new ClientMessage.Reconnect(ID, "token"),
                new ClientMessage.SignOut(ID),
                new ClientMessage.Hello(ID, "maya", true),
                new ClientMessage.ListRooms(ID),
                new ClientMessage.CreateRoom(ID, RoomMode.BOT_MATCH, Variant.LUDO_T, 2069L),
                new ClientMessage.CreateRoom(ID, RoomMode.PLAY, Variant.CLASSIC, null),
                new ClientMessage.SendChat(ID, null, "hello lobby"),
                new ClientMessage.ListFinishedGames(ID, 0),
                new ClientMessage.StartReplay(ID, "G-1", Speed.FAST),
                new ClientMessage.ControlReplay(ID, "RP-1", ReplayAction.PAUSE, null),
                new ClientMessage.JoinRoom(ID, "R-0001"),
                new ClientMessage.LeaveRoom(ID, "R-0001"),
                new ClientMessage.ClaimSeat(ID, "R-0001", Colour.GREEN),
                new ClientMessage.ReleaseSeat(ID, "R-0001"),
                new ClientMessage.ReclaimSeat(ID, "R-0001"),
                new ClientMessage.StartGame(ID, "R-0001"),
                new ClientMessage.Roll(ID, "R-0001", 12),
                new ClientMessage.ChooseMove(ID, "R-0001", 12, 1),
                new ClientMessage.PauseGame(ID, "R-0001"),
                new ClientMessage.ResumeGame(ID, "R-0001"),
                new ClientMessage.SetSpeed(ID, "R-0001", Speed.SLOW),
                new ClientMessage.RequestSnapshot(ID, "R-0001"));
    }

    static List<ServerMessage> serverMessages() {
        RoomSummary summary = new RoomSummary("R-0001", RoomMode.PLAY, Variant.LUDO_T,
                RoomStatus.WAITING, 1, 3, 2, "alice");
        SeatDto seat = new SeatDto(Colour.RED, Controller.HUMAN, "p1", "alice", true);
        MemberDto member = new MemberDto("p1", "alice", Role.PLAYER, true);
        EventDto event = new EventDto(EventType.CAPTURE, Colour.RED, 2, 30, 33, "R3 captures G1");
        EventDto rollOff = new EventDto(EventType.ROLL_OFF, Colour.GREEN, null, null, null, "Green rolls 5", 5);
        EventDto coinToss = new EventDto(EventType.COIN_TOSS, Colour.BLUE, 1, null, null, "B2 coin toss", 0);
        return List.of(
                new ServerMessage.Welcome("p1", "alice", "token"),
                Response(),
                ServerMessage.Response.error(ID, ErrorCode.STALE_TURN, "turn moved on"),
                new ServerMessage.Pong(5L),
                new ServerMessage.LobbyUpdate(9, List.of(summary)),
                new ServerMessage.RoomSnapshot("R-0001", 120, RoomMode.PLAY, Variant.LUDO_T, 7L,
                        RoomStatus.RUNNING, "p1", false, Speed.NORMAL, List.of(seat), board(), turn(),
                        List.of(member), List.of("line"), List.of(chat()), 42L),
                new ServerMessage.RoomSnapshot("R-0001", 120, RoomMode.PLAY, Variant.LUDO_T, 7L,
                        RoomStatus.RUNNING, "p1", false, Speed.NORMAL, List.of(seat), board(), turn(),
                        List.of(member), List.of("line"), List.of(chat()), 42L,
                        List.of(Colour.GREEN, Colour.YELLOW, Colour.BLUE, Colour.RED)),
                new ServerMessage.SeatsUpdated("R-0001", 121, List.of(seat)),
                new ServerMessage.StateUpdate("R-0001", 122, turn(), List.of(event), board(), 43L),
                new ServerMessage.StateUpdate("R-0001", 123, turn(), List.of(rollOff, coinToss), board(), 44L),
                new ServerMessage.YourTurn("R-0001", 0, Phase.ROLL_OFF, null, List.of(), null),
                new ServerMessage.YourTurn("R-0001", 12, Phase.AWAITING_MOVE, 6, List.of(option()), null),
                new ServerMessage.RoomStateChanged("R-0001", 123, RoomStatus.PAUSED, true, Speed.FAST, "p1"),
                new ServerMessage.MembersChanged("R-0001", 124, List.of(member)),
                new ServerMessage.GameOver("R-0001", 125, List.of(Colour.RED, Colour.BLUE), "G-1"),
                chat(),
                new ServerMessage.ReplayEvent("RP-1", 0, 10, List.of(event), board()),
                new ServerMessage.ReplayEnded("RP-1", ReplayEndReason.COMPLETED));
    }

    private static ServerMessage Response() {
        FinishedGame game = new FinishedGame("G-1", RoomMode.BOT_MATCH, Variant.LUDO_T, 2069L,
                Colour.RED, Instant.parse("2026-10-05T10:00:00Z"), 310);
        return ServerMessage.Response.ok(ID, new FinishedGameList(List.of(game), 0, false));
    }
}
