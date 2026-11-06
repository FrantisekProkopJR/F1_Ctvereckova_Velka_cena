package com.example.f1_ctvereckova_velka_cena;

import javafx.scene.paint.Color;

// Jednoduchá šablona, která určuje, jak formule vypadá.
public class CarTemplate {

    public String name;         // Název týmu
    public Color bodyColor;     // Hlavní barva auta
    public Color accentColor;   // Doplňková barva (na detaily a nápisy)

    // Konstruktor. Když vytváříme nové auto, dáme mu jméno a dvě barvy a on si je navždy zapamatuje.
    public CarTemplate(String name, Color bodyColor, Color accentColor) {
        this.name = name;
        this.bodyColor = bodyColor;
        this.accentColor = accentColor;
    }
}