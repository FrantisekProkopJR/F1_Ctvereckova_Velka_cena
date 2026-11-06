module com.example.f1_ctvereckova_velka_cena {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.f1_ctvereckova_velka_cena to javafx.fxml;
    exports com.example.f1_ctvereckova_velka_cena;
}