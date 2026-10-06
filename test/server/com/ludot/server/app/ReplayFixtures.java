package com.ludot.server.app;

import com.ludot.protocol.BoardDto;
import com.ludot.protocol.Colour;
import com.ludot.protocol.Phase;
import com.ludot.protocol.RoomMode;
import com.ludot.protocol.RoomStatus;
import com.ludot.protocol.Variant;
import com.ludot.server.adapters.memory.InMemoryGameRepository;
import com.ludot.server.game.RoomGame;
import java.util.ArrayList;
import java.util.List;

/** Plays real games with the real engine, so replays are checked against what the engine genuinely did. */
final class ReplayFixtures {

    /** A finished game: what a database would hold, plus what the engine ended with. */
    record Played(ReplayData data, BoardDto finalBoard, Colour winner) {
    }

    private ReplayFixtures() {
    }

    /** Plays a whole game, always choosing the first offered option (deterministic for a seed). */
    static Played play(String gameId, Variant variant, long seed) {
        RoomGame game = new RoomGame(variant, seed);
        game.start();
        List<ReplayData.Step> steps = new ArrayList<>();
        int turn = 0;
        while (!game.isFinished() && steps.size() < 50_000) {
            Colour colour = game.currentColour();
            if (game.phase() == Phase.AWAITING_ROLL) {
                steps.add(new ReplayData.Step(++turn, colour, game.roll(colour).rolled(), null));
            } else {
                game.applyMove(colour, 0);
                steps.add(new ReplayData.Step(++turn, colour, null, 0));
            }
        }
        if (!game.isFinished()) {
            throw new AssertionError("The fixture game did not finish");
        }
        Colour winner = game.standings().get(0);
        return new Played(new ReplayData(gameId, variant, seed, winner, steps), game.board(), winner);
    }

    /** Writes the game into a repository the way the server would have while it was played. */
    static void store(InMemoryGameRepository repository, Played played) {
        ReplayData data = played.data();
        repository.record(new RoomRecord(data.gameId(), RoomMode.BOT_MATCH, data.variant(), data.seed(), "host"));
        for (ReplayData.Step step : data.steps()) {
            repository.record(new DecisionRecord(data.gameId(), step.turnNumber(), step.colour(), step.roll(),
                    step.optionIndex(), DecisionSource.AI));
        }
        repository.record(new RoomStatusRecord(data.gameId(), RoomStatus.FINISHED, data.winner(), data.steps().size()));
    }
}
