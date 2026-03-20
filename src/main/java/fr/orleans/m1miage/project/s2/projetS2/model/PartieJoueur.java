package fr.orleans.m1miage.project.s2.projetS2.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "partie_joueur")
public class PartieJoueur extends Player {

    @ManyToOne
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    public PartieJoueur() {
        super();
    }

    public PartieJoueur(Partie partie, Utilisateur utilisateur, String couleur) {
        super(partie, couleur);
        this.utilisateur = utilisateur;
    }

}
