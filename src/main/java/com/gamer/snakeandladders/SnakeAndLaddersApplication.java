package com.gamer.snakeandladders;

import com.google.gson.Gson;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SpringBootApplication
public class SnakeAndLaddersApplication {

    @Bean
    public Supplier<String> buildGame() {
        LinkedHashMap state = initPlayers();
        ArrayList<Integer> gameBoard = generateBoard(100, 5, 5);
        runGame(gameBoard, state);
        state.put("gameBoard", gameBoard);

        return () -> {
            return new Gson().toJson(state);
        };
    }

    private LinkedHashMap initPlayers() {
        LinkedHashMap player1 = new LinkedHashMap();
        player1.put("position", 1);
        player1.put("snakeBites", 0);
        player1.put("ladderClimb", 0);

        LinkedHashMap player2 = new LinkedHashMap();
        player2.put("position", 1);
        player2.put("snakeBites", 0);
        player2.put("ladderClimb", 0);

        LinkedHashMap event = new LinkedHashMap();
        event.put("roll", 0);
        event.put("player1", player1);
        event.put("player2", player2);

        LinkedHashMap state = new LinkedHashMap();
        state.put(0, event);

        return state;
    }


    private void runGame(ArrayList<Integer> gameBoard, LinkedHashMap state) {
        nextTurn(gameBoard, state);
        assignTrophy(state);
    }

    private void nextTurn(ArrayList<Integer> gameBoard, LinkedHashMap state) {
        int newPosition = 0;
        do {
            int turnNo = state.size();
            int diceRoll = diceRoll();

            String activePlayerId = (turnNo % 2 == 0) ? "player2" : "player1";

            LinkedHashMap lastGameState = (LinkedHashMap) state.get(turnNo - 1);
            LinkedHashMap prevActivePlayerState = (LinkedHashMap) lastGameState.get(activePlayerId);

            newPosition = calculatePosition(gameBoard, diceRoll, (int) prevActivePlayerState.get("position"));
            LinkedHashMap event = newEventState(newPosition, activePlayerId, prevActivePlayerState, lastGameState, diceRoll);
            state.put(turnNo, event);

        } while (newPosition != 100);
    }

    private int calculatePosition(ArrayList<Integer> gameBoard, int diceRoll, int currentPosition) {
        if (currentPosition + diceRoll <= 100) return gameBoard.get(currentPosition + diceRoll - 1);
        else return gameBoard.get(currentPosition);
    }

    private void addSnakeLadderHits(LinkedHashMap prevActivePlayerState, LinkedHashMap activePlayer, int newPosition, int diceRoll) {

        int movement = diceRoll + (int) prevActivePlayerState.get("position");
        if (movement > newPosition) {
            activePlayer.put("snakeBites", (int) prevActivePlayerState.get("snakeBites") + 1);
            activePlayer.put("ladderClimb", (int) prevActivePlayerState.get("ladderClimb"));
        } else if (movement < newPosition) {
            activePlayer.put("ladderClimb", (int) prevActivePlayerState.get("ladderClimb") + 1);
            activePlayer.put("snakeBites", (int) prevActivePlayerState.get("snakeBites"));
        } else {
            activePlayer.put("ladderClimb", (int) prevActivePlayerState.get("ladderClimb"));
            activePlayer.put("snakeBites", (int) prevActivePlayerState.get("snakeBites"));
        }
    }

    private LinkedHashMap newEventState(int newPosition, String activePlayerId, LinkedHashMap prevActivePlayerState, LinkedHashMap lastGameState, int diceRoll) {
        String inactivePlayerId = (activePlayerId == "player1") ? "player2" : "player1";
        LinkedHashMap activePlayer = new LinkedHashMap();
        newActivePlayerState(newPosition, prevActivePlayerState, activePlayer, diceRoll);

        LinkedHashMap event = new LinkedHashMap();
        event.put("roll", diceRoll);

        if (activePlayerId == "player1") {
            event.put("player1", activePlayer);
            event.put("player2", lastGameState.get(inactivePlayerId));
        } else {
            event.put("player1", lastGameState.get(inactivePlayerId));
            event.put("player2", activePlayer);
        }
        return event;
    }

    private LinkedHashMap newActivePlayerState(int position, LinkedHashMap prevActivePlayerState, LinkedHashMap activePlayer, int diceRoll) {
        activePlayer.put("position", position);
        addSnakeLadderHits(prevActivePlayerState, activePlayer, position, diceRoll);
        return activePlayer;
    }

    private void assignTrophy(LinkedHashMap state) {
        LinkedHashMap lastGameState = (LinkedHashMap) state.get(state.size() - 1);
        String winner = ((int) ((LinkedHashMap) lastGameState.get("player1")).get("position") == 100) ? "player1" : "player2";
        state.put("winner", winner);
    }

    private ArrayList<Integer> generateBoard(int boardSize, int numOfSnakes, int numOfLadders) {
        List<Integer> boardSpaces = IntStream.rangeClosed(1, boardSize)
                .boxed().collect(Collectors.toList());

        HashMap<Integer, Integer> snakeLocations = generateSnakes(boardSpaces, numOfSnakes);
        HashMap<Integer, Integer> ladderLocations = generateLadders(boardSpaces, numOfLadders, snakeLocations);
        HashMap<Integer, Integer> occupiedLocations = new HashMap<Integer, Integer>(snakeLocations);
        occupiedLocations.putAll(ladderLocations);

        ArrayList<Integer> gameBoard = new ArrayList(boardSpaces);
        for (Integer space : boardSpaces)
            if (occupiedLocations.containsKey(space))
                gameBoard.set(space, occupiedLocations.get(space));

        return gameBoard;
    }


    private HashMap<Integer, Integer> generateSnakes(List<Integer> gameBoard, int numOfSnakes) {
        HashMap<Integer, Integer> snakeLocations = new HashMap<Integer, Integer>();
        for (int currentSnake = 0; currentSnake < numOfSnakes; currentSnake++) {
            int snakeHead = getSnakeHead(gameBoard, snakeLocations);
            snakeLocations.put(snakeHead, getSnakeTail(snakeHead, snakeLocations));
        }

        return snakeLocations;
    }

    private int getSnakeHead(List<Integer> gameBoard, HashMap<Integer, Integer> snakeLocations) {
        return getRandomNumberInRange(gameBoard.size() / 10, gameBoard.size() - 1, snakeLocations);
    }

    private int getSnakeTail(int snakeHead, HashMap<Integer, Integer> snakeLocations) {
        return getRandomNumberInRange(0, snakeHead, snakeLocations);
    }

    private HashMap<Integer, Integer> generateLadders(List<Integer> gameBoard, int numOfLadders, HashMap<Integer, Integer> snakeLocations) {
        HashMap<Integer, Integer> ladderLocations = new HashMap<Integer, Integer>();
        for (int currentLadder = 0; currentLadder < numOfLadders; currentLadder++) {
            HashMap occupiedLocations = new HashMap<>(snakeLocations);
            occupiedLocations.putAll(ladderLocations);
            int ladderTail = getLadderTail(gameBoard, occupiedLocations);
            ladderLocations.put(ladderTail, getLadderHead(gameBoard, ladderTail, occupiedLocations));
        }
        return ladderLocations;
    }

    private int getLadderTail(List<Integer> gameBoard, HashMap<Integer, Integer> occupiedLocations) {
        return getRandomNumberInRange(1, gameBoard.size() - 10, occupiedLocations);
    }

    private int getLadderHead(List<Integer> gameBoard, int ladderTail, HashMap<Integer, Integer> ladderLocations) {
        return getRandomNumberInRange(ladderTail, gameBoard.size() - 1, ladderLocations);
    }

    private static int getRandomNumberInRange(int min, int max) {
        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    private static int getRandomNumberInRange(int min, int max, HashMap<Integer, Integer> exclude) {
        Random r = new Random();
        int selection = r.nextInt((max - min) + 1) + min;
        if (exclude.containsKey(selection) || exclude.containsValue(selection))
            selection = getRandomNumberInRange(min, max, exclude);
        return selection;
    }

    private int diceRoll() {
        return getRandomNumberInRange(1, 6);
    }

    public static void main(String[] args) {
        SpringApplication.run(SnakeAndLaddersApplication.class, args);
    }

}
