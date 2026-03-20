package fr.orleans.m1miage.project.s2.projetS2.model;

public enum ModeJeu {
    SANS_TIMER("Mode classique"),
    AVEC_TIMER("Mode timer");

    private final String label;

    ModeJeu(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}



