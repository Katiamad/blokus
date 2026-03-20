package fr.orleans.m1miage.project.s2.projetS2.repository;

import fr.orleans.m1miage.project.s2.projetS2.model.Partie;
import fr.orleans.m1miage.project.s2.projetS2.model.Status;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PartieRepository extends JpaRepository<Partie, UUID> {

    Optional<Partie> findById(UUID id);

    Optional<Partie> findByNom(String nom);

    List<Partie> findByJoueursUtilisateurIdAndStatus(UUID utilisateurId, Status status);

    List<Partie> findByJoueursUtilisateurId(UUID id);
}
