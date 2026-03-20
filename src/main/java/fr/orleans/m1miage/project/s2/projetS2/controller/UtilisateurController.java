package fr.orleans.m1miage.project.s2.projetS2.controller;

import fr.orleans.m1miage.project.s2.projetS2.exeptions.InvalidEmailFormatException;
import fr.orleans.m1miage.project.s2.projetS2.exeptions.UtilisateurDejaExistantException;
import fr.orleans.m1miage.project.s2.projetS2.exeptions.UtilisateurNonTrouveException;
import fr.orleans.m1miage.project.s2.projetS2.model.Utilisateur;
import fr.orleans.m1miage.project.s2.projetS2.service.UtilisateurService;
import fr.orleans.m1miage.project.s2.projetS2.service.UtilisateurServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/blokus")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;
    private final PasswordEncoder passwordEncoder;

    public UtilisateurController(UtilisateurService utilisateurService, PasswordEncoder passwordEncoder) {
        this.utilisateurService = utilisateurService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/connexion")
    public String afficherPageConnexion(
            @RequestParam(value="error",      required=false) String errorParam,
            @RequestParam(value="success",    required=false) String successParam,
            @RequestParam(value="deconnexion",required=false) String logoutParam,
            @RequestParam(value="suppression",required=false) String deletionParam,
            @RequestParam(value="modification",required=false) String modificationParam,
            Model model) {

        if (errorParam != null) {
            model.addAttribute("error", "Identifiants invalides. Veuillez réessayer.");
        } else if (successParam != null) {
            model.addAttribute("error", "Votre compte a bien été créé ! Vous pouvez maintenant vous connecter !");
        } else if (logoutParam != null) {
            model.addAttribute("error", "Vous avez été déconnecté.");
        } else if (deletionParam != null) {
            model.addAttribute("error", "Votre compte a bien été supprimé !");
        } else if (modificationParam != null) {
            model.addAttribute("error", "Votre compte a bien été modifié !");
        }
        return "connexion";
    }


    @GetMapping("/inscription")
    public String afficherPageIncription(Model model) {
        model.addAttribute("utilisateur", new Utilisateur());
        return "inscription";
    }

    @PostMapping("/inscription")
    public String inscrireUtilisateur(@ModelAttribute Utilisateur user) throws Exception, InvalidEmailFormatException {
        utilisateurService.ajouterUtilisateur(user);
        return "redirect:/blokus/connexion?success";
    }

    @GetMapping("/home")
    public String afficherHomePage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String username = userDetails.getUsername();
        Utilisateur user = utilisateurService.findUtilisateurByUsername(username);
        model.addAttribute("utilisateur", user);
        return "home";
    }

    @GetMapping("/compte")
    public String afficherCompte(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String username = userDetails.getUsername();
        Utilisateur user = utilisateurService.findUtilisateurByUsername(username);

        if (user == null) {
            model.addAttribute("error", "Utilisateur non trouvé");
            return "redirect:/blokus/connexion?error";
        }

        model.addAttribute("utilisateur", user);
        return "compte";
    }

    @PostMapping("/modificationNom")
    public String modifierNomUtilisateur(@AuthenticationPrincipal UserDetails userDetails, @RequestParam String nom, Model model) throws Exception, UtilisateurDejaExistantException, InvalidEmailFormatException {
        String username = userDetails.getUsername();
        Utilisateur user = utilisateurService.findUtilisateurByUsername(username);

        if (nom != null && !nom.trim().isEmpty()) {
            user.setNom(nom);
        }

        utilisateurService.modifierNomEmailUtilisateur(user);
        model.addAttribute("utilisateur", user);
        return "redirect:/blokus/connexion?modification";  // Redirection vers la page compte
    }


    @PostMapping("/modificationEmail")
    public String modifierEmailUtilisateur(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String email,
            Model model) {

        String username = userDetails.getUsername();
        Utilisateur user = utilisateurService.findUtilisateurByUsername(username);

        if (user == null) {
            model.addAttribute("error", "Utilisateur non trouvé");
            return "redirect:/blokus/compte?error";
        }

        try {
            user.setEmail(email);
            utilisateurService.modifierNomEmailUtilisateur(user);
            return "redirect:/blokus/compte?success";
        } catch (Exception | UtilisateurDejaExistantException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/blokus/compte?error";
        }
    }

    @PostMapping("/modificationMotDePasse")
    public String modifierMotDePasseUtilisateur(@AuthenticationPrincipal UserDetails userDetails,
                                                @RequestParam String currentPassword,
                                                @RequestParam String motDePasse,
                                                Model model) throws Exception {
        String username = userDetails.getUsername();
        Utilisateur user = utilisateurService.findUtilisateurByUsername(username);

        if (user == null) {
            model.addAttribute("error", "Utilisateur non trouvé");
            return "redirect:/blokus/compte?error";
        }

        if (!passwordEncoder.matches(currentPassword, user.getMotDePasse())) {
            model.addAttribute("error", "Ancien mot de passe incorrect");
            return "redirect:/blokus/compte?error";
        }

        if (passwordEncoder.matches(motDePasse, user.getMotDePasse())) {
            model.addAttribute("error", "Le nouveau mot de passe doit être différent de l'ancien");
            return "redirect:/blokus/compte?error";
        }

        utilisateurService.modifierMotDePasseUtilisateur(user);

        model.addAttribute("utilisateur", user);
        return "redirect:/blokus/connexion?modification";
    }

    @PostMapping("/suppression")
    public String supprimerUtilisateur(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        String username = userDetails.getUsername();
        try {
            Utilisateur user = utilisateurService.findUtilisateurByUsername(username);

            if (user == null) {
                throw new UtilisateurNonTrouveException(username);
            }

            utilisateurService.deleteUtilisateur(user);
            return "redirect:/blokus/connexion?suppression";

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/blokus/connexion?error";        }
    }

}
