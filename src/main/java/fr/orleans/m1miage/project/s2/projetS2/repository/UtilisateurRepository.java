package fr.orleans.m1miage.project.s2.projetS2.repository;

import fr.orleans.m1miage.project.s2.projetS2.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID> {
    Utilisateur findByEmail(String email);

    @Query("SELECT u FROM Utilisateur u WHERE u.nom = :nom")
    Optional<Utilisateur> findByNomUtilisateur(String nom);


    Optional<Utilisateur> findById(UUID id);
}
