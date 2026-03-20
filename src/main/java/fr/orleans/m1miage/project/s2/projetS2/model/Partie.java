package fr.orleans.m1miage.project.s2.projetS2.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
@Table(name = "partie", uniqueConstraints = {
        @UniqueConstraint(columnNames = "nom")
})
public class Partie {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Convert(converter = GrilleConverter.class)
    @Column(columnDefinition = "LONGTEXT")
    private int[][] grille;

    @OneToMany(mappedBy = "partie", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("points DESC")
    private List<PartieJoueur> joueurs;

    @OneToMany(mappedBy = "partie", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("points DESC")
    private Set<PartieRobot> robots = new HashSet<>();

    private String nom;
    private String mode;
    private int tour;
    private Status status;

    @ElementCollection
    private List<UUID> gagnants;

    @Enumerated(EnumType.STRING)
    private ModeJeu modeJeu;

    private int tempsMin;
    private UUID createurId;
    private LocalDateTime startTime;


    public Partie() {
        this.joueurs = new ArrayList<>();
        this.mode = "Aucun joueur";
        this.grille = new int[20][20];
    }

    public Partie(String nom) {
        this.nom = nom;
        this.mode = "Aucun joueur";
        this.grille = new int[20][20];
        this.joueurs = new ArrayList<>();
    }

    public void trouverGagnant() {
        List<Player> allPlayers = new ArrayList<>();
        allPlayers.addAll(joueurs);
        allPlayers.addAll(robots);

        int maxScore = allPlayers.stream()
                .mapToInt(Player::getPoints)
                .max()
                .orElse(0);

        List<Player> gagnantsList = allPlayers.stream()
                .filter(p -> p.getPoints() == maxScore)
                .toList();

        this.gagnants = gagnantsList.stream()
                .map(Player::getId)
                .collect(Collectors.toList());
    }


    public void ajouterJoueur(Utilisateur joueur, String couleur) {
        PartieJoueur partieJoueur = new PartieJoueur(this, joueur, couleur);
        joueurs.add(partieJoueur);
    }
}