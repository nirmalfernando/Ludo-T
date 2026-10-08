package com.ludot.client;

import javafx.application.Application;

// it is a separate class from the Application subclass so the client also starts when run from a plain classpath
public final class ClientMain {

    private ClientMain() {
    }

    public static void main(String[] args) {
        Application.launch(ClientApp.class, args);
    }
}
