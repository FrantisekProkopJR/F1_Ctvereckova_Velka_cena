package com.example.f1_ctvereckova_velka_cena;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Řídí úplně první obrazovku hry (hlavní menu).
public class MenuController {

    private List<Player> allPossiblePlayers; // Všichni hráči, kteří teoreticky můžou hrát
    private List<Player> activePlayers = new ArrayList<>(); // Ti, kteří teď reálně hrají

    // Spustí se samo hned při zapnutí menu.
    @FXML
    public void initialize() {
        // Vytvoří 3 hráče do zásoby a rovnou jim rozdá klávesy (Šipky, WASD, IJKL).
        allPossiblePlayers = Arrays.asList(
                new Player("Hráč 1", KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT),
                new Player("Hráč 2", KeyCode.W, KeyCode.S, KeyCode.A, KeyCode.D),
                new Player("Hráč 3", KeyCode.I, KeyCode.K, KeyCode.J, KeyCode.L)
        );
    }

    // Tlačítka z menu. Zjistí, na co jsi kliknul, a pošlou dál číslo 1, 2, nebo 3.
    @FXML
    protected void onOnePlayerClick(ActionEvent event) throws IOException {
        setupActivePlayers(1, event);
    }

    @FXML
    protected void onTwoPlayersClick(ActionEvent event) throws IOException {
        setupActivePlayers(2, event);
    }

    @FXML
    protected void onThreePlayersClick(ActionEvent event) throws IOException {
        setupActivePlayers(3, event);
    }

    // Převezme vybrané číslo a přesune nás do výběru aut.
    private void setupActivePlayers(int count, ActionEvent event) throws IOException {
        activePlayers.clear(); // Promaže paměť z předchozí hry

        // Zkopíruje ze zásoby jen tolik hráčů, kolik chceme.
        for (int i = 0; i < count; i++) {
            activePlayers.add(allPossiblePlayers.get(i));
        }

        // Načte vzhled další obrazovky (Výběr aut).
        FXMLLoader loader = new FXMLLoader(getClass().getResource("CarSelectionView.fxml"));
        Parent root = loader.load();
        F1Main.applyF1FontToScene(root); // Nasadí náš hezký F1 font

        // "Haló, další obrazovko, tady ti posílám tyhle hráče!"
        CarSelectionController controller = loader.getController();
        controller.setGameData(activePlayers);

        // Fyzicky přepne divadelní scénu v okně na Výběr aut.
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 800, 600));
        root.requestFocus(); // Zajistí, aby nová obrazovka hned vnímala klávesnici
    }
}