package com.example.f1_ctvereckova_velka_cena;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Labeled;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;

public class F1Main extends Application {

    // Načte tučný F1 font. Pokud selže, dá tam obyčejný Arial.
    public static Font getF1BoldFont(double size) {
        InputStream stream = F1Main.class.getResourceAsStream("Formla1/Formula1-Bold_web_0.ttf");
        if (stream == null) {
            stream = F1Main.class.getResourceAsStream("Formula1/Formula1-Bold_web_0.ttf");
        }
        if (stream != null) {
            Font font = Font.loadFont(stream, size);
            if (font != null) return font;
        }
        //  Záloha
        return Font.font("Arial", FontWeight.BOLD, size);
    }

    // Načte normální F1 font (nebo Arial jako zálohu).
    public static Font getF1RegularFont(double size) {
        InputStream stream = F1Main.class.getResourceAsStream("Formla1/Formula1-Regular_web_0.ttf");
        if (stream == null) {
            stream = F1Main.class.getResourceAsStream("Formula1/Formula1-Regular_web_0.ttf");
        }
        if (stream != null) {
            Font font = Font.loadFont(stream, size);
            if (font != null) return font;
        }
        //  Záloha
        return Font.font("Arial", FontWeight.NORMAL, size);
    }

    // Najde všechny texty a tlačítka a nastaví jim F1 font.
    public static void applyF1FontToScene(Parent root) {
        for (Node node : root.lookupAll("*")) {
            if (node instanceof Labeled labeled) {
                // Zachová původní velikost písma, jinak dá defaultních 18
                double currentSize = (labeled.getFont() != null) ? labeled.getFont().getSize() : 18.0;
                labeled.setFont(getF1BoldFont(currentSize));
            }
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        // Vytváří a ukazuje samotné okno (Stage).
        getF1BoldFont(20);
        getF1RegularFont(20);

        // Úvodní menu z FXML
        FXMLLoader fxmlLoader = new FXMLLoader(F1Main.class.getResource("Menuview.fxml"));
        Parent root = fxmlLoader.load();
        applyF1FontToScene(root);

        // Nastaví velikost okna, nadpis a ukáže ho.
        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("Čtverečková Velká Cena - F1");
        stage.setScene(scene);
        stage.show();
    }

    // Hlavní tlačítko START celého programu.
    public static void main(String[] args) {
        launch(args);
    }
}