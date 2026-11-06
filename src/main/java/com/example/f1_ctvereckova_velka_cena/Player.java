package com.example.f1_ctvereckova_velka_cena;

import javafx.scene.input.KeyCode;
import java.util.ArrayList;
import java.util.List;

// Šablona pro každého závodníka. Pamatuje si úplně všechno o daném hráči.
public class Player {

    public String name;                       // Jméno (např. "Hráč 1")
    public KeyCode up, down, left, right;     // Klávesy pro řízení
    public CarTemplate selectedCar;           // Vybraná formule z menu

    public int x;                             // Kde na mapě (sloupec) zrovna stojí
    public int y;                             // Kde na mapě (řádek) zrovna stojí

    public int lapsCompleted = 0;             // Počet odjetých kol
    public int sectorStage = 0;               // Kterým sektorem naposledy projel (ochrana proti zkratkám)
    public long bestLapTime = Long.MAX_VALUE; // Nejrychlejší kolo (začíná na max. čísle, aby šlo překonat)
    public long lapStartTime = 0;             // Kdy odstartoval aktuální kolo

    public int championshipPoints = 0;        // Celkové body v mistrovství
    public int lastRacePoints = 0;            // Kolik bodů dostal za poslední závod

    // Pro plynulou jízdu (aby auto necukalo, když držíš šipku)
    public List<KeyCode> heldKeys = new ArrayList<>(); // Pamatuje si, které klávesy hráč zrovna drží
    public long lastMoveTime = 0;                      // Čas, kdy auto naposledy popojelo

    // Konstruktor. Když vytváříme hráče v menu, rovnou mu dáme jméno a nastavíme ovládání.
    public Player(String name, KeyCode up, KeyCode down, KeyCode left, KeyCode right) {
        this.name = name;
        this.up = up;
        this.down = down;
        this.left = left;
        this.right = right;
    }
}