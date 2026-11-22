package geeks;

import geeks.controller.LoginController;

import javax.swing.SwingUtilities;

public class MainGeeks {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginController::new);
    }
}
