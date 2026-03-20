package fr.orleans.m1miage.project.s2.projetS2.repository;

import fr.orleans.m1miage.project.s2.projetS2.model.PartieJoueur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PartieJoueurRepository extends JpaRepository<PartieJoueur, UUID> {
    List<PartieJoueur> findByUtilisateurId(UUID utilisateurId);
}
