package fr.orleans.m1miage.project.s2.projetS2.model;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@MappedSuperclass
public abstract class Player {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "partie_id")
    private Partie partie;

    @Convert(converter = PieceConverter.class)
    @Column(columnDefinition = "LONGTEXT")
    private List<Piece> pieces;

    @Convert(converter = PositionConverter.class)
    @Column(columnDefinition = "LONGTEXT")
    private List<int[]> positionsValidesPourCoin;

    @Column(nullable = false)
    private int points ;

    private String couleur;
    private String nom;
    private int tour;
    private String rang;

    private int lastPieceSize;

    public Player() {
        points = 0;
        pieces = PieceFactory.creerToutesLesPieces();
    }

    public Player(Partie partie, String couleur) {
        this.partie = partie;
        this.couleur = couleur;
        this.pieces = PieceFactory.creerToutesLesPieces();
        this.points = 0;
    }

    public void ajouterPointsPiece(Piece piece) {
        this.points += piece.getSize();
    }

    public void initialiserPositionsPremierTour() {
        this.positionsValidesPourCoin = new ArrayList<>();
    }
}
