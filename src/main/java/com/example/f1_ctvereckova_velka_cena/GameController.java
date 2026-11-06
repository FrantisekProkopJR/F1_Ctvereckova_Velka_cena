package com.example.f1_ctvereckova_velka_cena;

// Nástroje pro grafiku, animace, klávesnici a okna.
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

// Hlavní mozek hry. Tady se počítá pohyb, časy i vykreslování trati.
public class GameController {

    @FXML private Pane gamePane;     // Rámeček pro okno
    @FXML private Canvas gameCanvas; // Plátno, na které malujeme grafiku

    private GraphicsContext gc;          // Náš virtuální štětec
    private List<Player> activePlayers;  // Seznam hrajících závodníků
    private AnimationTimer gameLoop;     // Smyčka, co běží 60x za vteřinu (pohybuje hrou)

    // Jak rychle mohou auta jet (90 milisekund mezi kroky; menší číslo = rychlejší hra)
    private static final long MOVE_INTERVAL_NS = 90_000_000L;

    // Seznam závodů v šampionátu
    private List<Track> championship;    // Tratě vybrané hráči v draftu
    private int currentTrackIndex = 0;   // Která trať v pořadí se zrovna jede (začíná od 0)
    private Track currentTrack;          // Aktuálně načtená trať

    // Písma pro různé velikosti textů na obrazovce
    private Font fontHeader;
    private Font fontHud;
    private Font fontTitle;
    private Font fontSubtitle;

    // Všechny části závodního víkendu
    public enum GamePhase {
        TRÉNINK, PŘED_KVALIFIKACÍ, KVALIFIKACE, PŘED_ZÁVODEM, START, ZÁVOD, VÝSLEDKY_ZÁVODU, KONEC_ŠAMPIONÁTU
    }
    private GamePhase currentPhase = GamePhase.TRÉNINK; // Začínáme tréninkem

    private int targetLaps = 2;            // Kolik kol se v dané části jede
    private long startLightsTimer = 0;     // Časovač pro červená světla na startu
    private String raceWinnerMessage = ""; // Text se jménem vítěze na konci závodu

    // Zapnutí hry - přijme hráče i 3 vybrané tratě z obrazovky výběru tratí
    public void setGameData(List<Player> players, List<Track> selectedTracks) {
        this.activePlayers = players;
        this.gc = gameCanvas.getGraphicsContext2D(); // Připravíme štětec na kreslení

        // Načtení F1 fontů v různých velikostech
        this.fontHeader = F1Main.getF1BoldFont(15);
        this.fontHud = F1Main.getF1RegularFont(11);
        this.fontTitle = F1Main.getF1BoldFont(30);
        this.fontSubtitle = F1Main.getF1RegularFont(16);

        // Uložíme si 3 vybrané tratě do šampionátu (pokud by seznam chyběl, dáme Monzu jako zálohu)
        if (selectedTracks != null && !selectedTracks.isEmpty()) {
            championship = selectedTracks;
        } else {
            championship = Arrays.asList(Track.getMonza());
        }
        currentTrack = championship.get(currentTrackIndex); // Načteme první vybranou trať

        // Vynulování bodů na začátku šampionátu
        for (Player p : activePlayers) {
            p.championshipPoints = 0;
            p.lastRacePoints = 0;
        }

        // Přizpůsobení plátna velikosti okna
        gameCanvas.widthProperty().bind(gamePane.widthProperty());
        gameCanvas.heightProperty().bind(gamePane.heightProperty());

        resetSession(true); // Postaví auta na start a vynuluje časy

        // Sledování klávesnice (stisk i puštění klávesy)
        gamePane.setOnKeyPressed(this::handleKeyPressed);
        gamePane.setOnKeyReleased(this::handleKeyReleased);

        // Spuštění hlavní herní smyčky (běží pořád dokola)
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                processContinuousMovement(now); // 1. Spočítá pohyb aut
                renderGame();                   // 2. Překreslí obrazovku
            }
        };
        gameLoop.start();
    }

    // Vyčistí stisknuté klávesy, vynuluje kola a postaví auta zpět na startovní rošt
    private void resetSession(boolean resetTimes) {
        long now = System.currentTimeMillis();
        for (int i = 0; i < activePlayers.size(); i++) {
            Player p = activePlayers.get(i);
            p.x = currentTrack.startX[i]; // Startovní sloupec
            p.y = currentTrack.startY[i]; // Startovní řádek
            p.sectorStage = 0;            // Začíná před prvním sektorem
            p.lapsCompleted = 0;          // Nula odjetých kol

            // V tréninku a kvalifikaci stojí stopky na nule, dokud se hráč sám nepohne.
            if (currentPhase == GamePhase.TRÉNINK || currentPhase == GamePhase.KVALIFIKACE) {
                p.lapStartTime = 0;
            } else {
                p.lapStartTime = now;
            }

            p.heldKeys.clear();           // Vymaže seznam držených kláves
            p.lastMoveTime = 0;           // Povolí okamžitý pohyb
            if (resetTimes) {
                p.bestLapTime = Long.MAX_VALUE; // Smaže rekord kola
            }
        }
    }

    // Odchycení stisku klávesy
    private void handleKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();

        // 1) ENTER slouží pro posun mezi fázemi (odkliknutí výsledků, přesun na kvaldu...)
        if (currentPhase == GamePhase.VÝSLEDKY_ZÁVODU && code == KeyCode.ENTER) {
            currentTrackIndex++; // Jdeme na další vybranou trať
            if (currentTrackIndex < championship.size()) {
                currentTrack = championship.get(currentTrackIndex);
                currentPhase = GamePhase.TRÉNINK;
                targetLaps = 2;
                resetSession(true);
            } else {
                currentPhase = GamePhase.KONEC_ŠAMPIONÁTU; // Odjely se všechny 3 vybrané tratě
            }
            return;
        }

        // Po konci šampionátu nás ENTER vrátí do hlavního menu
        if (currentPhase == GamePhase.KONEC_ŠAMPIONÁTU && code == KeyCode.ENTER) {
            returnToMenu(event);
            return;
        }

        // Spuštění kvalifikace po tréninku
        if (currentPhase == GamePhase.PŘED_KVALIFIKACÍ && code == KeyCode.ENTER) {
            currentPhase = GamePhase.KVALIFIKACE;
            targetLaps = 2;
            resetSession(true);
            return;
        }

        // Spuštění startovní procedury (světel) před závodem
        if (currentPhase == GamePhase.PŘED_ZÁVODEM && code == KeyCode.ENTER) {
            currentPhase = GamePhase.START;
            targetLaps = 3;      // Závod se jede na 3 kola
            resetSession(false); // Časy z kvalifikace nemazat!
            startLightsTimer = System.currentTimeMillis(); // Spustí odpočet světel
            return;
        }

        // 2) Pokud nejde o ENTER, uložíme drženou šipku/klávesu k danému hráči
        for (Player p : activePlayers) {
            if (code == p.up || code == p.down || code == p.left || code == p.right) {
                if (!p.heldKeys.contains(code)) {
                    p.heldKeys.add(code);
                    processContinuousMovement(System.nanoTime()); // Auto rovnou popojede
                }
            }
        }
    }

    // Při puštění klávesy ji smažeme ze seznamu držených kláves hráče
    private void handleKeyReleased(KeyEvent event) {
        KeyCode code = event.getCode();
        for (Player p : activePlayers) {
            p.heldKeys.remove(code);
        }
    }

    // HLAVNÍ FYZIKA - Zajišťuje plynulý pohyb aut
    private void processContinuousMovement(long nowNano) {
        // Pokud čteme texty v menu nebo svítí červená světla, auta stojí
        if (currentPhase == GamePhase.START ||
                currentPhase == GamePhase.VÝSLEDKY_ZÁVODU || currentPhase == GamePhase.KONEC_ŠAMPIONÁTU ||
                currentPhase == GamePhase.PŘED_KVALIFIKACÍ || currentPhase == GamePhase.PŘED_ZÁVODEM) {
            return;
        }

        for (Player p : activePlayers) {
            // Když má hráč v tréninku/kvaldě odjeto, auto už stojí a čeká na ostatní
            if (currentPhase != GamePhase.ZÁVOD && p.lapsCompleted >= targetLaps) continue;

            // Hráč zrovna nic nemačká -> auto stojí
            if (p.heldKeys.isEmpty()) continue;

            // Interval kroku pohybu
            if (nowNano - p.lastMoveTime < MOVE_INTERVAL_NS) continue;

            // Zjistíme, kam chce hráč jet podle poslední zmáčknuté klávesy
            KeyCode activeKey = p.heldKeys.get(p.heldKeys.size() - 1);
            int nextX = p.x;
            int nextY = p.y;

            // Změna souřadnic podle směru
            if (activeKey == p.up) nextY--;
            else if (activeKey == p.down) nextY++;
            else if (activeKey == p.left) nextX--;
            else if (activeKey == p.right) nextX++;

            p.lastMoveTime = nowNano; // Uložíme čas tohoto kroku

            if (nextX == p.x && nextY == p.y) continue;

            // Zabrání vyjetí úplně mimo pole mapy, kontrola vyšky a šířky
            if (nextY >= 0 && nextY < currentTrack.grid.length && nextX >= 0 && nextX < currentTrack.grid[0].length) {
                int targetCell = currentTrack.grid[nextY][nextX];
                int effectiveCell = (targetCell == 6) ? 1 : targetCell; // Startovní rošt (6) bereme jako asfalt (1)

                if (targetCell != 0) { // 0 = Tráva nebo zeď (tam se vjet nedá)
                    boolean canMove = true;

                    // Kolize - v samotném závodě do sebe auta naráží
                    if (currentPhase == GamePhase.ZÁVOD) {
                        for (Player other : activePlayers) {
                            if (other != p && other.x == nextX && other.y == nextY) {
                                canMove = false;
                                break;
                            }
                        }
                    }

                    // Pokud je cesta volná, auto se přesune na nové políčko
                    if (canMove) {
                        // Pokud hráči v tréninku/kvalifikaci ještě neběží stopky, zapnou se při tomto prvním pohybu!
                        if (p.lapStartTime == 0) {
                            p.lapStartTime = System.currentTimeMillis();
                        }

                        p.x = nextX;
                        p.y = nextY;
                        long currentTime = System.currentTimeMillis();

                        // OCHRANA PŘED PODVÁDĚNÍM: Sektory se musí projet popořadě: Červená (5) -> Modrá (3) -> Žlutá (4) -> Cíl (2)
                        if (effectiveCell == 5) {
                            p.sectorStage = 1; // Projel 1. sektorem
                        } else if (effectiveCell == 3) {
                            if (p.sectorStage == 1 || p.sectorStage == 2) p.sectorStage = 2; // Projel 2. sektorem
                            else p.sectorStage = 0; // Kolo se nepočíta!
                        } else if (effectiveCell == 4) {
                            if (p.sectorStage == 2 || p.sectorStage == 3) p.sectorStage = 3; // Projel 3. sektorem
                            else p.sectorStage = 0; // Kolo se nepočíta!
                        } else if (effectiveCell == 2) { // Cílová čára
                            if (p.sectorStage == 3) { // Projel poctivě celé kolo
                                p.lapsCompleted++;
                                p.sectorStage = 0; // Reset sektorů pro další kolo

                                // Výpočet času kola a případný zápis osobního rekordu
                                long lapTime = currentTime - p.lapStartTime;
                                if (lapTime < p.bestLapTime) {
                                    p.bestLapTime = lapTime;
                                }
                                p.lapStartTime = currentTime; // Reset stopek pro nové kolo

                                // Kontrola, jestli už není konec jízd
                                if (currentPhase == GamePhase.ZÁVOD) {
                                    if (p.lapsCompleted >= targetLaps) {
                                        advancePhase(p); // První v cíli vyhrává!
                                        return;
                                    }
                                } else {
                                    // V tréninku a kvalifikaci čekáme, až svá kola odjedou všichni
                                    boolean allFinished = true;
                                    for (Player player : activePlayers) {
                                        if (player.lapsCompleted < targetLaps) {
                                            allFinished = false;
                                            break;
                                        }
                                    }
                                    if (allFinished) {
                                        advancePhase(p);
                                        return;
                                    }
                                }
                            } else {
                                p.sectorStage = 0; // Přejel cíl pozpátku nebo bez sektorů
                            }
                        }
                    }
                }
            }
        }
    }

    // Přepne hru do další části víkendu (např. z Kvalifikace do Závodu)
    private void advancePhase(Player phaseWinner) {
        for (Player p : activePlayers) p.heldKeys.clear(); // Zastaví všechna auta

        if (currentPhase == GamePhase.TRÉNINK) {
            currentPhase = GamePhase.PŘED_KVALIFIKACÍ;
        } else if (currentPhase == GamePhase.KVALIFIKACE) {
            // Seřadí hráče od nejrychlejšího času po nejpomalejší (Pole Position)
            activePlayers.sort((p1, p2) -> Long.compare(p1.bestLapTime, p2.bestLapTime));
            // Naskládá je na startovní rošt podle výsledků kvalifikace
            for (int i = 0; i < activePlayers.size(); i++) {
                activePlayers.get(i).x = currentTrack.startX[i];
                activePlayers.get(i).y = currentTrack.startY[i];
            }
            currentPhase = GamePhase.PŘED_ZÁVODEM;
        } else if (currentPhase == GamePhase.ZÁVOD) {
            // Seřadí hráče v cíli: 1. je vítěz, zbytek podle ujetých kol a sektorů
            activePlayers.sort((p1, p2) -> {
                if (p1 == phaseWinner) return -1;
                if (p2 == phaseWinner) return 1;
                int lapCompare = Integer.compare(p2.lapsCompleted, p1.lapsCompleted);
                if (lapCompare != 0) return lapCompare;
                return Integer.compare(p2.sectorStage, p1.sectorStage);
            });

            // Rozdělení bodů za závod (25, 18, 15)
            int[] pointsSystem = {25, 18, 15};
            for (int i = 0; i < activePlayers.size(); i++) {
                int pts = (i < pointsSystem.length) ? pointsSystem[i] : 0;
                activePlayers.get(i).lastRacePoints = pts;
                activePlayers.get(i).championshipPoints += pts;
            }

            // Seřadí hráče podle celkových bodů v šampionátu
            activePlayers.sort((p1, p2) -> Integer.compare(p2.championshipPoints, p1.championshipPoints));

            currentPhase = GamePhase.VÝSLEDKY_ZÁVODU;
            raceWinnerMessage = phaseWinner.name + " (" + phaseWinner.selectedCar.name + ")";
        }
    }

    // Návrat z mistrovství zpět do hlavního menu
    private void returnToMenu(KeyEvent event) {
        try {
            if (gameLoop != null) gameLoop.stop();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Menuview.fxml"));
            Parent root = loader.load();
            F1Main.applyF1FontToScene(root);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            root.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Textová zkratka pro horní lištu (ukazuje, jakou čáru musí hráč teď projet)
    private String getSectorStatus(Player p) {
        if (p.sectorStage == 0) return "S1-Červ";
        if (p.sectorStage == 1) return "S2-Modr";
        if (p.sectorStage == 2) return "S3-Žlut";
        return "CÍL";
    }

    // --- VYKRESLOVÁNÍ GRAFIKY --- (Pouští se 60x za vteřinu)
    private void renderGame() {
        double width = gameCanvas.getWidth();
        double height = gameCanvas.getHeight();

        if (width <= 0 || height <= 0) return;

        // Pozadí (zelená tráva přes celou obrazovku)
        gc.setFill(Color.rgb(39, 174, 96));
        gc.fillRect(0, 0, width, height);

        // Výpočet volného místa pro trať
        double topPanelHeight = 50;
        double bottomPanelHeight = 50;
        double availableWidth = width - 40;
        double availableHeight = height - topPanelHeight - bottomPanelHeight - 20;

        // Vypočítá velikost jednoho čtverečku (TILE_SIZE), aby se trať vešla do okna
        double tileSizeX = availableWidth / currentTrack.grid[0].length;
        double tileSizeY = availableHeight / currentTrack.grid.length;
        double TILE_SIZE = Math.min(tileSizeX, tileSizeY);

        // Vystředění tratě doprostřed obrazovky
        double trackPixelWidth = currentTrack.grid[0].length * TILE_SIZE;
        double xOffset = (width - trackPixelWidth) / 2.0;
        double yOffset = topPanelHeight + 10;

        // Vykreslení samotné trati (čte mřížku čísel z Track.java políčko po políčku)
        for (int row = 0; row < currentTrack.grid.length; row++) {
            for (int col = 0; col < currentTrack.grid[0].length; col++) {
                int cellType = currentTrack.grid[row][col];
                double px = xOffset + (col * TILE_SIZE);
                double py = yOffset + (row * TILE_SIZE);

                if (cellType == 0) { // 0 = Tráva
                    gc.setFill(Color.rgb(39, 174, 96));
                    gc.fillRect(px, py, TILE_SIZE, TILE_SIZE);
                } else {
                    gc.setFill(Color.rgb(52, 73, 94)); // Tmavě šedý asfalt
                    gc.fillRect(px, py, TILE_SIZE, TILE_SIZE);

                    // Barvení speciálních čar na asfaltu
                    if (cellType == 2) { // Cíl (černobílá šachovnice)
                        gc.setFill(Color.WHITE);
                        gc.fillRect(px, py, TILE_SIZE / 2.0, TILE_SIZE / 2.0);
                        gc.fillRect(px + TILE_SIZE / 2.0, py + TILE_SIZE / 2.0, TILE_SIZE / 2.0, TILE_SIZE / 2.0);
                        gc.setFill(Color.BLACK);
                        gc.fillRect(px + TILE_SIZE / 2.0, py, TILE_SIZE / 2.0, TILE_SIZE / 2.0);
                        gc.fillRect(px, py + TILE_SIZE / 2.0, TILE_SIZE / 2.0, TILE_SIZE / 2.0);
                    } else if (cellType == 3) { // Modrý sektor
                        gc.setFill(Color.rgb(52, 152, 219));
                        gc.fillRect(px, py, TILE_SIZE, TILE_SIZE);
                    } else if (cellType == 4) { // Žlutý sektor
                        gc.setFill(Color.rgb(241, 196, 15));
                        gc.fillRect(px, py, TILE_SIZE, TILE_SIZE);
                    } else if (cellType == 5) { // Červený sektor
                        gc.setFill(Color.rgb(231, 76, 60));
                        gc.fillRect(px, py, TILE_SIZE, TILE_SIZE);
                    } else if (cellType == 6) { // Startovní rošt (žlutý obrys)
                        gc.setStroke(Color.YELLOW);
                        gc.setLineWidth(Math.max(1.5, TILE_SIZE / 10.0));
                        gc.strokeRect(px + 2, py + 2, TILE_SIZE - 4, TILE_SIZE - 4);
                    }
                }
            }
        }

        // Vykreslení formulek všech hráčů na trať
        for (Player p : activePlayers) {
            double px = xOffset + (p.x * TILE_SIZE);
            double py = yOffset + (p.y * TILE_SIZE);

            // V tréninku a kvaldě jsou auta poloprůhledná (0.5)
            double alpha = (currentPhase == GamePhase.ZÁVOD || currentPhase == GamePhase.START || currentPhase == GamePhase.PŘED_ZÁVODEM) ? 1.0 : 0.5;
            double s = TILE_SIZE / 20.0;

            // 1. Čtyři černá kola
            gc.setFill(Color.color(0, 0, 0, alpha));
            gc.fillRect(px + 2 * s, py + 2 * s, 4 * s, 6 * s);
            gc.fillRect(px + 14 * s, py + 2 * s, 4 * s, 6 * s);
            gc.fillRect(px + 2 * s, py + 12 * s, 4 * s, 6 * s);
            gc.fillRect(px + 14 * s, py + 12 * s, 4 * s, 6 * s);

            // 2. Karoserie formule (zaoblený obdélník v hlavní barvě týmu)
            gc.setFill(Color.color(p.selectedCar.bodyColor.getRed(), p.selectedCar.bodyColor.getGreen(), p.selectedCar.bodyColor.getBlue(), alpha));
            gc.fillRoundRect(px + 5 * s, py + 3 * s, 10 * s, 14 * s, 4 * s, 4 * s);

            // 3. Helma jezdce uprostřed auta (kolečko v doplňkové barvě týmu)
            gc.setFill(Color.color(p.selectedCar.accentColor.getRed(), p.selectedCar.accentColor.getGreen(), p.selectedCar.accentColor.getBlue(), alpha));
            gc.fillOval(px + 8 * s, py + 7 * s, 4 * s, 4 * s);
        }

        // Vykreslení horní bílé lišty
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, width, 50);
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, width, 4);
        gc.fillRect(0, 46, width, 4);

        // Vykreslení spodní bílé lišty pod tratí
        double trackBottomY = yOffset + (currentTrack.grid.length * TILE_SIZE);
        double bottomHeight = height - trackBottomY;
        if (bottomHeight > 0) {
            gc.setFill(Color.WHITE);
            gc.fillRect(0, trackBottomY, width, bottomHeight);
            gc.setFill(Color.BLACK);
            gc.fillRect(0, trackBottomY, width, 4);
            gc.fillRect(0, height - 4, width, 4);
        }

        // Hlavní nápis v horní liště
        gc.setFill(Color.BLACK);
        gc.setFont(fontHeader);
        gc.fillText(currentTrack.name + " (" + (currentTrackIndex + 1) + "/" + championship.size() + ") | FÁZE: " + currentPhase.name() + " (" + targetLaps + " KOLA) | START: " + currentTrack.startDirection, 15, 27);

        // Vykreslení statistik pro každého hráče vedle sebe
        double spacing = width / Math.max(1, activePlayers.size());
        int index = 0;
        for (Player p : activePlayers) {
            // Stopky stojí před startem, po dojetí, nebo dokud se hráč v tréninku/kvaldě nepohne (lapStartTime == 0)
            boolean timerPaused = (p.lapStartTime == 0 || currentPhase == GamePhase.START ||
                    currentPhase == GamePhase.PŘED_KVALIFIKACÍ || currentPhase == GamePhase.PŘED_ZÁVODEM ||
                    currentPhase == GamePhase.VÝSLEDKY_ZÁVODU || currentPhase == GamePhase.KONEC_ŠAMPIONÁTU ||
                    (currentPhase != GamePhase.ZÁVOD && p.lapsCompleted >= targetLaps));

            long currentLap = timerPaused ? 0 : System.currentTimeMillis() - p.lapStartTime;
            String bestText = (p.bestLapTime == Long.MAX_VALUE) ? "--:--" : String.format("%.1fs", p.bestLapTime / 1000.0);
            String currText = String.format("%.1fs", currentLap / 1000.0);

            double uiX = 15 + (index * spacing);

            gc.setFill(p.selectedCar.bodyColor);
            gc.setFont(fontHud);
            gc.fillText(p.name + " ", uiX, 43);

            gc.setFill(Color.BLACK);
            gc.fillText("(" + p.lapsCompleted + "/" + targetLaps + " | " + getSectorStatus(p) + ") " + currText + " | Nej:" + bestText + " | " + p.championshipPoints + "b", uiX + 45, 43);
            index++;
        }

        // Střed obrazovky pro vyskakovací okna
        double centerX = width / 2.0;
        double centerY = height / 2.0;

        // --- VYSKAKOVACÍ OKNA PODLE FÁZE HRY ---

        // 1. Okno po skončení tréninku
        if (currentPhase == GamePhase.PŘED_KVALIFIKACÍ) {
            gc.setFill(Color.rgb(0, 0, 0, 0.8));
            gc.fillRect(0, 0, width, height);
            gc.setFill(Color.WHITE);
            gc.setFont(fontTitle);
            gc.fillText("TRÉNINK DOKONČEN", centerX - 180, centerY - 30);
            gc.setFont(fontSubtitle);
            gc.fillText("Stiskni ENTER pro start Kvalifikace", centerX - 140, centerY + 20);

            // 2. Okno s výsledky kvalifikace
        } else if (currentPhase == GamePhase.PŘED_ZÁVODEM) {
            gc.setFill(Color.rgb(0, 0, 0, 0.8));
            gc.fillRect(0, 0, width, height);
            gc.setFill(Color.WHITE);
            gc.setFont(fontTitle);
            gc.fillText("KVALIFIKACE DOKONČENA", centerX - 210, centerY - 80);
            gc.setFont(fontSubtitle);
            gc.fillText("Pořadí na startovním roštu (Pole Position vpředu):", centerX - 200, centerY - 40);

            int yPos = (int) centerY;
            for (int i = 0; i < activePlayers.size(); i++) {
                Player p = activePlayers.get(i);
                String timeStr = (p.bestLapTime == Long.MAX_VALUE) ? "--:--" : String.format("%.2fs", p.bestLapTime / 1000.0);
                gc.setFill(p.selectedCar.bodyColor);
                gc.fillText((i + 1) + ". " + p.name + " (" + p.selectedCar.name + ") - " + timeStr, centerX - 100, yPos);
                yPos += 32;
            }
            gc.setFill(Color.WHITE);
            gc.fillText("Stiskni ENTER pro zahájení Závodu!", centerX - 140, yPos + 15);

            // 3. Startovní semafor (5 červených světel)
        } else if (currentPhase == GamePhase.START) {
            long elapsed = System.currentTimeMillis() - startLightsTimer;
            int lightsOn = (int) (elapsed / 1000);

            if (lightsOn >= 6) {
                currentPhase = GamePhase.ZÁVOD;
                long now = System.currentTimeMillis();
                for (Player p : activePlayers) {
                    p.lapStartTime = now;
                    p.lapsCompleted = 0;
                    p.sectorStage = 0;
                }
            } else {
                gc.setFill(Color.rgb(0, 0, 0, 0.85));
                gc.fillRoundRect(centerX - 160, centerY - 40, 320, 80, 20, 20);
                for (int i = 0; i < 5; i++) {
                    gc.setFill((i < lightsOn) ? Color.RED : Color.DARKGRAY);
                    gc.fillOval((centerX - 135) + (i * 55), centerY - 25, 40, 40);
                }
            }

            // 4. Tabulka výsledků po dojetí závodu
        } else if (currentPhase == GamePhase.VÝSLEDKY_ZÁVODU) {
            gc.setFill(Color.rgb(0, 0, 0, 0.88));
            gc.fillRoundRect(centerX - 290, centerY - 160, 580, 320, 20, 20);
            gc.setFill(Color.WHITE);
            gc.setFont(F1Main.getF1BoldFont(22));
            gc.fillText("VÍTĚZ ZÁVODU: " + raceWinnerMessage, centerX - 250, centerY - 110);

            gc.setFont(fontSubtitle);
            gc.fillText("Průběžné pořadí šampionátu:", centerX - 250, centerY - 70);

            int yPos = (int) centerY - 30;
            for (int i = 0; i < activePlayers.size(); i++) {
                Player p = activePlayers.get(i);
                gc.setFill(p.selectedCar.bodyColor);
                gc.fillText((i + 1) + ". " + p.name + " (" + p.selectedCar.name + ") - " + p.championshipPoints + " b. (+" + p.lastRacePoints + ")", centerX - 230, yPos);
                yPos += 32;
            }

            gc.setFill(Color.WHITE);
            gc.fillText("Stiskni ENTER pro přesun na další trať", centerX - 160, yPos + 15);

            // 5. Závěrečná obrazovka po odjetí všech vybraných tratí
        } else if (currentPhase == GamePhase.KONEC_ŠAMPIONÁTU) {
            gc.setFill(Color.rgb(0, 0, 0, 0.92));
            gc.fillRoundRect(centerX - 300, centerY - 170, 600, 340, 20, 20);
            gc.setFill(Color.GOLD);
            gc.setFont(F1Main.getF1BoldFont(26));
            gc.fillText("MISTROVSTVÍ SVĚTA UKONČENO!", centerX - 250, centerY - 115);

            Player champion = activePlayers.get(0);
            gc.setFill(Color.WHITE);
            gc.setFont(F1Main.getF1BoldFont(18));
            gc.fillText("ŠAMPION: " + champion.name + " (" + champion.selectedCar.name + ") - " + champion.championshipPoints + " bodů", centerX - 250, centerY - 75);

            gc.setFont(fontSubtitle);
            int yPos = (int) centerY - 35;
            for (int i = 0; i < activePlayers.size(); i++) {
                Player p = activePlayers.get(i);
                gc.setFill(p.selectedCar.bodyColor);
                gc.fillText((i + 1) + ". " + p.name + " (" + p.selectedCar.name + ") — " + p.championshipPoints + " bodů", centerX - 210, yPos);
                yPos += 30;
            }

            gc.setFill(Color.WHITE);
            gc.fillText("Stiskni ENTER pro návrat do výběru hráčů", centerX - 180, yPos + 15);
        }
    }
}