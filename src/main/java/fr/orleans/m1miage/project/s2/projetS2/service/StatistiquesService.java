package fr.orleans.m1miage.project.s2.projetS2.service;

import fr.orleans.m1miage.project.s2.projetS2.model.JoueurStatistique;

import java.util.UUID;

public interface StatistiquesService {
    JoueurStatistique getStatisticsForUser(UUID utilisateurId);
}
