package fr.orleans.m1miage.project.s2.projetS2.exeptions;

public class PositionsInvalidesException extends RuntimeException {
    public PositionsInvalidesException(String message) {

        super("Coordonnées invalides : " + message);
    }
}
