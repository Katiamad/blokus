package fr.orleans.m1miage.project.s2.projetS2.exeptions;

public class PieceInvalideException extends RuntimeException {
    public PieceInvalideException(String message) {

      super("Pièce invalide : " + message);
    }
}
