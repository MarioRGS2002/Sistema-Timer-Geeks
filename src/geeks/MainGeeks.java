package geeks;

import geeks.controller.GeeksController;

import javax.swing.SwingUtilities;

public class MainGeeks {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(GeeksController::new);
    }
}
