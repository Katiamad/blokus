

package fr.orleans.m1miage.project.s2.projetS2.service;

import fr.orleans.m1miage.project.s2.projetS2.exeptions.*;
import fr.orleans.m1miage.project.s2.projetS2.model.Partie;
import fr.orleans.m1miage.project.s2.projetS2.model.PartieJoueur;
import fr.orleans.m1miage.project.s2.projetS2.model.Status;
import fr.orleans.m1miage.project.s2.projetS2.model.Utilisateur;
import fr.orleans.m1miage.project.s2.projetS2.repository.PartieJoueurRepository;
import fr.orleans.m1miage.project.s2.projetS2.repository.PartieRepository;
import fr.orleans.m1miage.project.s2.projetS2.repository.UtilisateurRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UtilisateurServiceImpl implements UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final PartieRepository partieRepository;
    private final PasswordEncoder passwordEncoder;
    private final PartieService partieService;
    private final PartieJoueurRepository partieJoueurRepository;


    public UtilisateurServiceImpl(UtilisateurRepository utilisateurRepository, PartieRepository partieRepository, PasswordEncoder passwordEncoder, PartieService partieService, PartieJoueurRepository partieJoueurRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.partieRepository = partieRepository;
        this.passwordEncoder = passwordEncoder;
        this.partieService = partieService;
        this.partieJoueurRepository = partieJoueurRepository;
    }


    @PostConstruct
    public void postConstruct() {
        String encodedPasswordLylia= passwordEncoder.encode("securepass456");
        Utilisateur lylia = new Utilisateur("BossLyliiia", "lylia@gmail.com",encodedPasswordLylia);
        utilisateurRepository.save(lylia);

        String encodedPasswordKatia= passwordEncoder.encode("mypassword789");
        Utilisateur katia = new Utilisateur("KatiaQueen", "katia@gmail.com", encodedPasswordKatia);
        utilisateurRepository.save(katia);

        String encodedPasswordLucie = passwordEncoder.encode("Lucie2903");
        Utilisateur lucie = new Utilisateur("lulu", "lucie@gmail.com", encodedPasswordLucie);
        utilisateurRepository.save(lucie);

        partieService.seedFinishedGamesForUser(lylia,lucie);
    }

    @Override
    public Utilisateur findUtilisateurByEmail(String email) {
        Utilisateur user = utilisateurRepository.findByEmail(email);
        if (user == null) {
            throw new UtilisateurNonTrouveException("Aucun utilisateur trouvé avec l'email : " + email);
        }
        return user;
    }

    @Override
    public Utilisateur findUtilisateurByUsername(String username) {
        return utilisateurRepository.findByNomUtilisateur(username)
                .orElseThrow(() -> new UtilisateurNonTrouveException("Aucun utilisateur trouvé avec le nom : " + username));
    }

    @Override
    public Utilisateur findUtilisateurByUsernameOuEmail(String nomOuEmail) {
        return utilisateurRepository.findByNomUtilisateur(nomOuEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable pour : " + nomOuEmail));
    }

    @Override
    public void ajouterUtilisateur(Utilisateur utilisateur) throws InvalidEmailFormatException {
        if (utilisateur.getEmail() == null || utilisateur.getEmail().trim().isEmpty()) {
            throw new InvalidEmailFormatException("L'email est requis.");
        }
        if (!utilisateur.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new InvalidEmailFormatException("Le format de l'email est invalide.");
        }

        if (utilisateur.getMotDePasse() == null || utilisateur.getMotDePasse().trim().isEmpty()) {
            throw new InvalidPasswordException("Le mot de passe est requis.");
        }
        if (!utilisateur.getMotDePasse().matches("^(?=.*[0-9])(?=.*[A-Za-z])(?=.*[A-Z]).{8,}$")) {
            throw new InvalidPasswordException("Le mot de passe doit contenir au moins 8 caractères, dont une majuscule, une minuscule et un chiffre.");
        }

        if (utilisateurRepository.findByEmail(utilisateur.getEmail()) != null) {
            throw new EmailDejaExistantException("Un utilisateur avec cet email existe déjà : " + utilisateur.getEmail());
        }

        if (utilisateurRepository.findByNomUtilisateur(utilisateur.getNom()).isPresent()) {
            throw new EmailDejaExistantException("Le nom d'utilisateur est déjà pris : " + utilisateur.getNom());
        }

        String encodedPassword = passwordEncoder.encode(utilisateur.getMotDePasse());
        Utilisateur nouvelUtilisateur = new Utilisateur(utilisateur.getNom(), utilisateur.getEmail(), encodedPassword);
        utilisateurRepository.save(nouvelUtilisateur);
    }

    @Override
    public void modifierNomEmailUtilisateur(Utilisateur utilisateur) throws InvalidEmailFormatException, UtilisateurDejaExistantException {
        Utilisateur utilisateurExistant = utilisateurRepository.findById(utilisateur.getId())
                .orElseThrow(() -> new UtilisateurNonTrouveException("Utilisateur introuvable pour l'ID : " + utilisateur.getId()));

        if (utilisateur.getNom() != null && !utilisateur.getNom().trim().isEmpty() &&
                !utilisateurExistant.getNom().equals(utilisateur.getNom())) {
            if (utilisateurRepository.findByNomUtilisateur(utilisateur.getNom()).isPresent()) {
                throw new UtilisateurDejaExistantException("Le nom d'utilisateur est déjà pris : " + utilisateur.getNom());
            }
            utilisateurExistant.setNom(utilisateur.getNom());
        }

        if (utilisateur.getEmail() != null && !utilisateur.getEmail().trim().isEmpty() &&
                !utilisateurExistant.getEmail().equals(utilisateur.getEmail())) {
            if (!utilisateur.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new InvalidEmailFormatException("Le format de l'email est invalide.");
            }
            if (utilisateurRepository.findByEmail(utilisateur.getEmail()) != null) {
                throw new EmailDejaExistantException("Un utilisateur avec cet email existe déjà : " + utilisateur.getEmail());
            }
            utilisateurExistant.setEmail(utilisateur.getEmail());
        }

        utilisateurRepository.save(utilisateurExistant);
    }

    @Override
    public void modifierMotDePasseUtilisateur(Utilisateur utilisateur) {
        Utilisateur utilisateurExistant = utilisateurRepository.findById(utilisateur.getId())
                .orElseThrow(() -> new UtilisateurNonTrouveException("Utilisateur introuvable pour l'ID : " + utilisateur.getId()));

        if (utilisateur.getMotDePasse() != null && !utilisateur.getMotDePasse().trim().isEmpty()) {
            if (!utilisateur.getMotDePasse().matches("^(?=.*[0-9])(?=.*[A-Za-z])(?=.*[A-Z]).{8,}$")) {
                throw new InvalidPasswordException("Le mot de passe doit contenir au moins 8 caractères, dont une majuscule, une minuscule et un chiffre.");
            }
            if (passwordEncoder.matches(utilisateur.getMotDePasse(), utilisateurExistant.getMotDePasse())) {
                throw new InvalidPasswordException("Le nouveau mot de passe doit être différent de l'ancien.");
            }
            utilisateurExistant.setMotDePasse(passwordEncoder.encode(utilisateur.getMotDePasse()));
            utilisateurRepository.save(utilisateurExistant);
        }
    }

    @Transactional
    @Override
    public void deleteUtilisateur(Utilisateur utilisateur) {
        if (!utilisateurRepository.existsById(utilisateur.getId())) {
            throw new UtilisateurNonTrouveException("Utilisateur introuvable pour suppression, ID : " + utilisateur.getId());
        }
        List<Partie> parties = partieRepository.findByJoueursUtilisateurId(utilisateur.getId());
        for (Partie partie : parties) {
            if (partie.getStatus() == Status.EN_ATTENTE || partie.getStatus() == Status.EN_COURS) {
                partieRepository.delete(partie);
            }
        }
        List<PartieJoueur> joueurs = partieJoueurRepository.findByUtilisateurId(utilisateur.getId());
        for (PartieJoueur pj : joueurs) {
            Partie partie = pj.getPartie();
            if (partie.getStatus() == Status.TERMINEE) {
                partieJoueurRepository.delete(pj);
            }
        }
        utilisateurRepository.delete(utilisateur);
    }

}
