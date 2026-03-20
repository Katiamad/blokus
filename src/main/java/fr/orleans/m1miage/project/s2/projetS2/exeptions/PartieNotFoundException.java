package fr.orleans.m1miage.project.s2.projetS2.exeptions;

public class PartieNotFoundException extends RuntimeException {
    public PartieNotFoundException(String  idPartie) {

        super("Partie avec l'id " + idPartie + " introuvable.");
    }
}
