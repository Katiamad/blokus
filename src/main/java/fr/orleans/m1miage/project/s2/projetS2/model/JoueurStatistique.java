package fr.orleans.m1miage.project.s2.projetS2.model;

public class JoueurStatistique {
    private int nbPartiesJouees;
    private int nbPartiesGagnees;
    private double ratioPartiesGagnees;

    public JoueurStatistique() {}

    public JoueurStatistique(int nbPartiesJouees, int nbPartiesGagnees, double ratioPartiesGagnees) {
        this.nbPartiesJouees = nbPartiesJouees;
        this.nbPartiesGagnees = nbPartiesGagnees;
        this.ratioPartiesGagnees = ratioPartiesGagnees;
    }

    public int getNbPartiesJouees() {
        return nbPartiesJouees;
    }

    public void setNbPartiesJouees(int nbPartiesJouees) {
        this.nbPartiesJouees = nbPartiesJouees;
    }

    public int getNbPartiesGagnees() {
        return nbPartiesGagnees;
    }

    public void setNbPartiesGagnees(int nbPartiesGagnees) {
        this.nbPartiesGagnees = nbPartiesGagnees;
    }

    public double getRatioPartiesGagnees() {
        return ratioPartiesGagnees;
    }

    public void setRatioPartiesGagnees(double ratioPartiesGagnees) {
        this.ratioPartiesGagnees = ratioPartiesGagnees;
    }
}
