package fr.orleans.m1miage.project.s2.projetS2.model;

public class Piece {

    private int id;
    private String couleur;
    private int[][] forme;
    private int size;
    private PartieJoueur joueur;
    private PartieRobot partieRobot;

    public Piece(int id, String couleur, int[][] forme, int size) {
        this.id = id;
        this.couleur = couleur;
        this.forme = forme;
        this.size = size;
    }

    public Piece() {
    }

    public Piece(int[][] ints) {
        this.forme = ints;
    }

    public String getCouleur() {
        return couleur;
    }

    public int[][] getForme() {
        return forme;
    }

    public void setForme(int[][] forme) {
        this.forme = forme;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

}

