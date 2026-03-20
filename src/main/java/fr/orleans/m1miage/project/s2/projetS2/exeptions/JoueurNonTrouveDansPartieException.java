package fr.orleans.m1miage.project.s2.projetS2.exeptions;

public class JoueurNonTrouveDansPartieException extends RuntimeException {
    public JoueurNonTrouveDansPartieException(String message) {
        super("Joueur non trouvé dans la partie.");
    }
}
