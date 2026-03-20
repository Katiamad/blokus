package fr.orleans.m1miage.project.s2.projetS2.model;

import java.util.UUID;

public interface ModeJeuStrategy {
    Partie startGame(Partie partie);
    void stopGame(UUID idPartie);

}
