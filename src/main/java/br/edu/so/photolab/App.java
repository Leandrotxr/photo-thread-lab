package br.edu.so.photolab;

import br.edu.so.photolab.ui.MainView;
import javafx.application.Application;

public final class App {

    private App() {
    }

    public static void main(String[] args) {
        Application.launch(MainView.class, args);
    }
}
