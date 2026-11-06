package com.example.f1_ctvereckova_velka_cena;

// Nástroje pro tvorbu tlačítek, nápisů a přepínání okna.
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Ovládá obrazovku, kde si hráči na střídačku vybírají 3 tratě do šampionátu.
public class TrackSelectionController {

    @FXML private Label turnInfoLabel;  // Nápis nahoře, který říká, kdo teď vybírá
    @FXML private TilePane tracksPane;  // Mřížka 3x3, kam naskládáme tlačítka s tratěmi

    private List<Player> activePlayers;                   // Kdo hraje
    private List<Track> allTracks;                        // Všech 9 tratí v nabídce
    private List<Track> chosenTracks = new ArrayList<>(); // Sem se postupně uloží 3 vybrané tratě

    private int currentPlayerIndex = 0; // Který hráč je zrovna na řadě (0 = první, 1 = druhý...)
    private int targetTrackCount = 3;   // Celkový počet tratí, které chceme vybrat

    // Převezme hráče z předchozí obrazovky a načte nabídku všech 9 tratí.
    public void setGameData(List<Player> players) {
        this.activePlayers = players;
        this.allTracks = Track.getAllTracks();
        updateUI();
    }

    // Vykreslí tlačítka tratí a aktualizuje nápis, kdo je na tahu.
    private void updateUI() {
        tracksPane.getChildren().clear(); // Vyčistí stará tlačítka před překreslením

        // Jakmile máme vybrané všechny 3 tratě, jdeme rovnou závodit!
        if (chosenTracks.size() >= targetTrackCount) {
            startChampionship();
            return;
        }

        // Zjistíme, který hráč je na řadě, a vypíšeme jeho jméno v barvě jeho týmu.
        Player currentP = activePlayers.get(currentPlayerIndex);
        turnInfoLabel.setText("Vybírá " + currentP.name + " (" + currentP.selectedCar.name + ") — Trať " + (chosenTracks.size() + 1) + " ze " + targetTrackCount);
        turnInfoLabel.setTextFill(currentP.selectedCar.bodyColor);

        // Vytvoříme tlačítko pro každou z 9 tratí.
        for (Track track : allTracks) {
            Button btn = new Button();
            btn.setPrefSize(200, 95);
            btn.setAlignment(Pos.CENTER);
            btn.setTextAlignment(TextAlignment.CENTER);
            btn.setFont(F1Main.getF1BoldFont(14));

            // Pokud už tuto trať někdo vybral dříve, tlačítko vypneme a označíme.
            if (chosenTracks.contains(track)) {
                int order = chosenTracks.indexOf(track) + 1;
                btn.setText(track.name + "\n[ " + order + ". ZÁVOD ]");
                btn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 10;");
                btn.setDisable(true);
            } else {
                // Trať je volná – hráč na ni může kliknout.
                btn.setText(track.name + "\nStart: " + track.startDirection);
                btn.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-background-radius: 10; -fx-border-color: #e74c3c; -fx-border-width: 2; -fx-border-radius: 10;");

                // Co se stane po kliknutí na trať:
                btn.setOnAction(e -> {
                    chosenTracks.add(track); // Uložíme trať do kalendáře
                    currentPlayerIndex = (currentPlayerIndex + 1) % activePlayers.size(); // Předáme slovo dalšímu hráči
                    updateUI(); // Překreslíme obrazovku
                });
            }

            tracksPane.getChildren().add(btn);
        }
    }

    // Přepne okno na samotnou hru a pošle jí hráče i 3 vybrané tratě.
    private void startChampionship() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("GameView.fxml"));
            Parent root = loader.load();
            F1Main.applyF1FontToScene(root);

            GameController controller = loader.getController();
            controller.setGameData(activePlayers, chosenTracks); // Posíláme hráče + vybrané 3 tratě

            Stage stage = (Stage) tracksPane.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            root.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}