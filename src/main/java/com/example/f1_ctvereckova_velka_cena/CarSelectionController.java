package com.example.f1_ctvereckova_velka_cena;

// Nástroje pro okno a klávesnici.
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

// Ovládá obrazovku, kde si hráči vybírají formule.
public class CarSelectionController {

    // @FXML znamená, že se tyto věci načtou rovnou z našeho grafického návrhu.
    @FXML private VBox mainPane;
    @FXML private VBox player1Box; @FXML private Label p1NameLabel; @FXML private Label p1CarLabel; @FXML private Label p1ControlsLabel;
    @FXML private VBox player2Box; @FXML private Label p2NameLabel; @FXML private Label p2CarLabel; @FXML private Label p2ControlsLabel;
    @FXML private VBox player3Box; @FXML private Label p3NameLabel; @FXML private Label p3CarLabel; @FXML private Label p3ControlsLabel;

    private List<Player> activePlayers;      // Kdo hraje
    private List<CarTemplate> availableCars; // Nabídka aut

    // Jaké auto má kdo vybrané. Každý má na startu jiné, ať se nehádají.
    private int[] selectedIndices = new int[]{0, 1, 2};

    // Spustí se samo při načtení. Vytvoří nabídku aut a jejich barev.
    public void initialize() {
        availableCars = Arrays.asList(
                new CarTemplate("Red Bull", Color.NAVY, Color.RED),
                new CarTemplate("Ferrari", Color.RED, Color.YELLOW),
                new CarTemplate("McLaren", Color.ORANGE, Color.BLACK),
                new CarTemplate("Mercedes", Color.DARKGRAY, Color.TURQUOISE),
                new CarTemplate("Aston Martin", Color.DARKGREEN, Color.LIGHTGREEN),
                new CarTemplate("Alpine", Color.HOTPINK, Color.BLUE)
        );
    }

    // Sem nám předchozí obrazovka pošle seznam hráčů.
    public void setGameData(List<Player> players) {
        this.activePlayers = players;

        // Schová panely pro hráče, kteří nehrají.
        player1Box.setVisible(players.size() >= 1);
        player2Box.setVisible(players.size() >= 2);
        player3Box.setVisible(players.size() >= 3);

        updateUI(); // Zobrazí texty
        mainPane.setOnKeyPressed(this::handleKeyPress); // Začne poslouchat klávesnici
    }

    // Zjistí, kdo z hráčů zmáčkl svoji klávesu, a posune mu výběr (doleva = -1, doprava = 1).
    private void handleKeyPress(KeyEvent event) {
        if (activePlayers.size() >= 1) {
            if (event.getCode() == activePlayers.get(0).left) changeCar(0, -1);
            if (event.getCode() == activePlayers.get(0).right) changeCar(0, 1);
        }
        if (activePlayers.size() >= 2) {
            if (event.getCode() == activePlayers.get(1).left) changeCar(1, -1);
            if (event.getCode() == activePlayers.get(1).right) changeCar(1, 1);
        }
        if (activePlayers.size() >= 3) {
            if (event.getCode() == activePlayers.get(2).left) changeCar(2, -1);
            if (event.getCode() == activePlayers.get(2).right) changeCar(2, 1);
        }
    }

    // Změní hráči auto. Pokud je už zabrané, automaticky ho přeskočí.
    private void changeCar(int playerIndex, int direction) {
        boolean valid;
        do {
            selectedIndices[playerIndex] += direction; // Posune výběr o 1

            // Když dojde na konec seznamu, skočí na začátek (a naopak).
            if (selectedIndices[playerIndex] < 0) selectedIndices[playerIndex] = availableCars.size() - 1;
            else if (selectedIndices[playerIndex] >= availableCars.size()) selectedIndices[playerIndex] = 0;

            valid = true;
            // Zkontroluje, jestli tohle auto nemá vybrané někdo jiný.
            for (int i = 0; i < activePlayers.size(); i++) {
                if (i != playerIndex && selectedIndices[i] == selectedIndices[playerIndex]) {
                    valid = false; // Auto už někdo má, cyklus se zopakuje a hledá dál.
                }
            }
        } while (!valid);

        updateUI(); // Ukáže nové auto na obrazovce.
    }

    // Zaktualizuje všechny viditelné boxy hráčů.
    private void updateUI() {
        if (activePlayers.size() >= 1) updatePlayerBox(0, p1NameLabel, p1CarLabel, p1ControlsLabel);
        if (activePlayers.size() >= 2) updatePlayerBox(1, p2NameLabel, p2CarLabel, p2ControlsLabel);
        if (activePlayers.size() >= 3) updatePlayerBox(2, p3NameLabel, p3CarLabel, p3ControlsLabel);
    }

    // Vypíše jméno hráče a jeho nové auto v barvě týmu.
    private void updatePlayerBox(int index, Label name, Label car, Label controls) {
        Player p = activePlayers.get(index);
        CarTemplate c = availableCars.get(selectedIndices[index]);
        name.setText(p.name);
        car.setText(c.name);
        car.setTextFill(c.bodyColor); // Obarví nápis barvou auta
        controls.setText("Listování: " + p.left.getName() + " / " + p.right.getName());
    }

    // Tlačítko pro potvrzení aut a přechod na výběr tratí.
    @FXML
    protected void onStartWeekendClick(ActionEvent event) {
        try {
            // Uloží vybraná auta přímo k hráčům.
            for (int i = 0; i < activePlayers.size(); i++) {
                activePlayers.get(i).selectedCar = availableCars.get(selectedIndices[i]);
            }

            // Načte obrazovku pro výběr tratí (TrackSelectionView.fxml).
            FXMLLoader loader = new FXMLLoader(getClass().getResource("TrackSelectionView.fxml"));
            Parent root = loader.load();
            F1Main.applyF1FontToScene(root);

            // Předá hráče do obrazovky výběru tratí.
            TrackSelectionController controller = loader.getController();
            controller.setGameData(activePlayers);

            // Přepne okno na výběr tratí.
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            root.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}