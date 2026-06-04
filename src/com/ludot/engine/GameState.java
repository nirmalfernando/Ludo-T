package com.ludot.engine;

import com.ludot.model.Board;
import com.ludot.model.Player;
import java.util.List;

@Deprecated
public class GameState extends com.ludot.model.GameState {

    
    public GameState(List<Player> players, Board board) {
        super(players, board);
    }

    
    public GameState(List<Player> players, Board board, int turnNumber) {
        super(players, board, turnNumber);
    }
}
