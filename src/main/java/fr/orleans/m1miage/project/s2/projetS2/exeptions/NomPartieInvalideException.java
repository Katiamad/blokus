package fr.orleans.m1miage.project.s2.projetS2.exeptions;

public class NomPartieInvalideException extends RuntimeException {
    public NomPartieInvalideException(String message) {

        super("Nom de la partie invalide : " + message);
    }
}
