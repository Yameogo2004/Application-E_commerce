package ui;

import Client.ClientSocketService;

import javax.swing.*;

public class MainUI {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientSocketService clientService = new ClientSocketService();
            new LoginFrame(clientService).setVisible(true);
        });
    }
}