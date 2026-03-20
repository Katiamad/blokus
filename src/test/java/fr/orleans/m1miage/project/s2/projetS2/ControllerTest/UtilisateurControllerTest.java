package fr.orleans.m1miage.project.s2.projetS2.ControllerTest;


import fr.orleans.m1miage.project.s2.projetS2.config.WebSecurityConfig;
import fr.orleans.m1miage.project.s2.projetS2.controller.UtilisateurController;
import fr.orleans.m1miage.project.s2.projetS2.exeptions.InvalidEmailFormatException;
import fr.orleans.m1miage.project.s2.projetS2.exeptions.UtilisateurDejaExistantException;
import fr.orleans.m1miage.project.s2.projetS2.exeptions.UtilisateurNonTrouveException;
import fr.orleans.m1miage.project.s2.projetS2.model.Utilisateur;
import fr.orleans.m1miage.project.s2.projetS2.repository.UtilisateurRepository;
import fr.orleans.m1miage.project.s2.projetS2.service.UtilisateurService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UtilisateurController.class)
@AutoConfigureMockMvc
@Import(WebSecurityConfig.class)
public class UtilisateurControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UtilisateurService utilisateurService;

    @MockBean
    private UtilisateurRepository utilisateurRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    // --------------------------------------------
    // Tests pour afficherPageConnexion
    // --------------------------------------------

    @Test
    @DisplayName("Connexion - error affiche message d'erreur identifiants")
    void afficherPageConnexion_errorParamAfficheErreur() throws Exception {
        mockMvc.perform(get("/blokus/connexion").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("connexion"))
                .andExpect(model().attribute("error", "Identifiants invalides. Veuillez réessayer."));
    }

    @Test
    @DisplayName("Connexion - success affiche message de création de compte")
    void afficherPageConnexion_successParamAfficheSuccess() throws Exception {
        mockMvc.perform(get("/blokus/connexion").param("success", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("connexion"))
                .andExpect(model().attribute("error", "Votre compte a bien été créé ! Vous pouvez maintenant vous connecter !"));
    }

    @Test
    @DisplayName("Connexion - deconnexion affiche message de déconnexion")
    void afficherPageConnexion_deconnexionParamAfficheMessage() throws Exception {
        mockMvc.perform(get("/blokus/connexion").param("deconnexion", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("connexion"))
                .andExpect(model().attribute("error", "Vous avez été déconnecté."));
    }

    @Test
    @DisplayName("Connexion - suppression affiche message suppression")
    void afficherPageConnexion_suppressionParamAfficheMessage() throws Exception {
        mockMvc.perform(get("/blokus/connexion").param("suppression", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("connexion"))
                .andExpect(model().attribute("error", "Votre compte a bien été supprimé !"));
    }

    @Test
    @DisplayName("Connexion - modification affiche message modification")
    void afficherPageConnexion_modificationParamAfficheMessage() throws Exception {
        mockMvc.perform(get("/blokus/connexion").param("modification", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("connexion"))
                .andExpect(model().attribute("error", "Votre compte a bien été modifié !"));
    }

    @Test
    @DisplayName("Connexion - sans param affiche la vue sans message d'erreur")
    void afficherPageConnexion_sansParamPasDErreur() throws Exception {
        mockMvc.perform(get("/blokus/connexion"))
                .andExpect(status().isOk())
                .andExpect(view().name("connexion"))
                .andExpect(model().size(0)); // aucun attribut ajouté au modèle
    }


    // --------------------------------------------
    // Tests afficherPageIncription
    // --------------------------------------------

    @Test
    void afficherPageInscription_getStatusOkAndReturnView() throws Exception {
        mockMvc.perform(get("/blokus/inscription"))
                .andExpect(status().isOk())
                .andExpect(view().name("inscription"));
    }

    @Test
    void afficherPageInscription_modelContainsUtilisateur() throws Exception {
        mockMvc.perform(get("/blokus/inscription"))
                .andExpect(model().attributeExists("utilisateur"))
                .andExpect(model().attribute("utilisateur", org.hamcrest.Matchers.hasProperty("id")))
                .andExpect(model().attribute("utilisateur", org.hamcrest.Matchers.hasProperty("nom")))
                .andExpect(model().attribute("utilisateur", org.hamcrest.Matchers.hasProperty("email")));
    }

    // --------------------------------------------
    // Tests inscrireUtilisateur
    // --------------------------------------------

    @Test
    void inscrireUtilisateur_valide_redirigeConnexionSuccess() throws Exception, InvalidEmailFormatException {
        mockMvc.perform(post("/blokus/inscription")
                        .param("nom", "TestNom")
                        .param("email", "test@example.com")
                        .param("motDePasse", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blokus/connexion?success"));
        verify(utilisateurService).ajouterUtilisateur(any(Utilisateur.class));
    }

    // --------------------------------------------
    // Tests afficherHomePage
    // --------------------------------------------

    @Test
    void afficherHomePage_utilisateurAuthentifie_etTrouve() throws Exception {
        String username = "lucie";
        UserDetails principal = User.withUsername(username).password("xxx").roles("USER").build();

        Utilisateur user = new Utilisateur();
        user.setNom("Lucie");
        user.setEmail("lucie@email.com");
        user.setMotDePasse("xxx");

        when(utilisateurService.findUtilisateurByUsername(username)).thenReturn(user);

        mockMvc.perform(get("/blokus/home").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attribute("utilisateur", user));
    }

    @Test
    void afficherHomePage_utilisateurAuthentifie_nonTrouve() throws Exception {
        String username = "lucie";
        UserDetails principal = User.withUsername(username).password("xxx").roles("USER").build();

        when(utilisateurService.findUtilisateurByUsername(username)).thenReturn(null);

        mockMvc.perform(get("/blokus/home").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attribute("utilisateur", (Object) null));
    }

    @Test
    void afficherHomePage_nonAuthentifie_redirigeConnexion() throws Exception {
        mockMvc.perform(get("/blokus/home"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/connexion"));
    }

    // --------------------------------------------
    // Tests afficherCompte
    // --------------------------------------------

    @Test
    void afficherCompte_utilisateurExiste() throws Exception {
        String username = "lucie";
        String password = "secret";
        UserDetails principal = User.withUsername(username).password(password).roles("USER").build();
        Utilisateur user = new Utilisateur(username, "lucie@email.com", password);

        when(utilisateurService.findUtilisateurByUsername(username)).thenReturn(user);

        mockMvc.perform(get("/blokus/compte")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("compte"))
                .andExpect(model().attribute("utilisateur", user));

        verify(utilisateurService).findUtilisateurByUsername(username);
    }

    @Test
    void afficherCompte_utilisateurNonTrouve() throws Exception {
        String username = "lucie";
        UserDetails principal = User.withUsername(username).password("xxx").roles("USER").build();

        when(utilisateurService.findUtilisateurByUsername(username)).thenReturn(null);

        mockMvc.perform(get("/blokus/compte")
                        .with(user(principal)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blokus/connexion?error"));

        verify(utilisateurService).findUtilisateurByUsername(username);
    }

    // --------------------------------------------
    // Tests modifierNomUtilisateur
    // --------------------------------------------

    @Test
    void modifierNomUtilisateur_nomVide_neModifiePasNom() throws Exception, UtilisateurDejaExistantException, InvalidEmailFormatException {
        String username = "lucie";
        String nomInitial = "Lucie";
        UserDetails principal = User.withUsername(username).password("pass").roles("USER").build();
        Utilisateur user = new Utilisateur(username, "lucie@ex.com", "pass");
        user.setNom(nomInitial);

        when(utilisateurService.findUtilisateurByUsername(username)).thenReturn(user);

        mockMvc.perform(post("/blokus/modificationNom")
                        .with(user(principal))
                        .param("nom", "  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blokus/connexion?modification"));

        assertEquals(nomInitial, user.getNom());
        verify(utilisateurService).findUtilisateurByUsername(username);
        verify(utilisateurService).modifierNomEmailUtilisateur(user);
    }

    @Test
    void modifierNomUtilisateur_ok_redirigeEtModifieNom() throws Exception, UtilisateurDejaExistantException, InvalidEmailFormatException {
        String username = "lucie";
        String nouveauNom = "LucieTest";
        UserDetails principal = User.withUsername(username).password("pass").roles("USER").build();
        Utilisateur user = new Utilisateur(username, "lucie@ex.com", "pass");

        when(utilisateurService.findUtilisateurByUsername(username)).thenReturn(user);

        mockMvc.perform(post("/blokus/modificationNom")
                        .with(user(principal))
                        .param("nom", nouveauNom))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blokus/connexion?modification"));

        assertEquals(nouveauNom, user.getNom());
        verify(utilisateurService).findUtilisateurByUsername(username);
        verify(utilisateurService).modifierNomEmailUtilisateur(user);
    }

    @Test
    void modifierNomUtilisateur_ok() throws Exception, UtilisateurDejaExistantException, InvalidEmailFormatException {
        String username = "lucie";
        String nouveauNom = "LucieTest";
        UserDetails principal = User.withUsername(username).password("pass").roles("USER").build();
        Utilisateur user = new Utilisateur(username, "lucie@ex.com", "pass");

        when(utilisateurService.findUtilisateurByUsername(username)).thenReturn(user);

        doNothing().when(utilisateurService).modifierNomEmailUtilisateur(user);

        mockMvc.perform(post("/blokus/modificationNom")
                        .with(user(principal))
                        .param("nom", nouveauNom))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blokus/connexion?modification"));

        verify(utilisateurService).findUtilisateurByUsername(username);
        verify(utilisateurService).modifierNomEmailUtilisateur(user);
    }

    // --------------------------------------------
    // Tests modifierNomUtilisateur
    // --------------------------------------------

    @Test
    void modifierEmailUtilisateur_success() throws Exception, UtilisateurDejaExistantException, InvalidEmailFormatException {
        String username = "lucie";
        String nouvelEmail = "lucie2@example.com";
        UserDetails principal = User.withUsername(username).password("pass").roles("USER").build();
        Utilisateur user = new Utilisateur(username, "lucie@ex.com", "pass");

        when(utilisateurService.findUtilisateurByUsername(username)).thenReturn(user);
        doNothing().when(utilisateurService).modifierNomEmailUtilisateur(user);

        mockMvc.perform(post("/blokus/modificationEmail")
                        .with(user(principal))
                        .param("email", nouvelEmail))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blokus/compte?success"));

        verify(utilisateurService).findUtilisateurByUsername(username);
        verify(utilisateurService).modifierNomEmailUtilisateur(user);
    }

    @Test
    void modifierEmailUtilisateur_emailDejaExistant() throws Exception, UtilisateurDejaExistantException, InvalidEmailFormatException {
        String username = "lucie";
        String nouvelEmail = "existant@example.com";
        UserDetails principal = User.withUsername(username).password("pass").roles("USER").build();
        Utilisateur user = new Utilisateur(username, "lucie@ex.com", "pass");

        when(utilisateurService.findUtilisateurByUsername(username)).thenReturn(user);
        doThrow(new UtilisateurDejaExistantException("Email déjà utilisé"))
                .when(utilisateurService).modifierNomEmailUtilisateur(user);

        mockMvc.perform(post("/blokus/modificationEmail")
                        .with(user(principal))
                        .param("email", nouvelEmail))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blokus/compte?error"));

        verify(utilisateurService).findUtilisateurByUsername(username);
        verify(utilisateurService).modifierNomEmailUtilisateur(user);
    }

    @Test
    void modifierEmailUtilisateur_emailInvalide() throws Exception, UtilisateurDejaExistantException, InvalidEmailFormatException {
        String username = "lucie";
        String nouvelEmail = "mauvaisformat"; // mauvais format
        UserDetails principal = User.withUsername(username).password("pass").roles("USER").build();
        Utilisateur user = new Utilisateur(username, "lucie@ex.com", "pass");

        when(utilisateurService.findUtilisateurByUsername(username)).thenReturn(user);
        doThrow(new InvalidEmailFormatException("Format email invalide"))
                .when(utilisateurService).modifierNomEmailUtilisateur(user);

        mockMvc.perform(post("/blokus/modificationEmail")
                        .with(user(principal))
                        .param("email", nouvelEmail))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blokus/compte?error"));

        verify(utilisateurService).findUtilisateurByUsername(username);
        verify(utilisateurService).modifierNomEmailUtilisateur(user);
    }

    @Test
    void modifierEmailUtilisateur_exceptionGenerique() throws Exception, UtilisateurDejaExistantException, InvalidEmailFormatException {
        String username = "lucie";
        String nouvelEmail = "test@example.com";
        UserDetails principal = User.withUsername(username).password("pass").roles("USER").build();
        Utilisateur user = new Utilisateur(username, "lucie@ex.com", "pass");

        when(utilisateurService.findUtilisateurByUsername(username)).thenReturn(user);
        doThrow(new RuntimeException("Erreur inattendue"))
                .when(utilisateurService).modifierNomEmailUtilisateur(user);

        mockMvc.perform(post("/blokus/modificationEmail")
                        .with(user(principal))
                        .param("email", nouvelEmail))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blokus/compte?error"));

        verify(utilisateurService).findUtilisateurByUsername(username);
        verify(utilisateurService).modifierNomEmailUtilisateur(user);
    }

    @Test
    void modifierEmailUtilisateur_utilisateurNonTrouve() throws Exception, UtilisateurDejaExistantException, InvalidEmailFormatException {
        String username = "lucie";
        String nouvelEmail = "lucie2@example.com";
        UserDetails principal = User.withUsername(username).password("pass").roles("USER").build();

        when(utilisateurService.findUtilisateurByUsername(username)).thenReturn(null);

        mockMvc.perform(post("/blokus/modificationEmail")
                        .with(user(principal))
                        .param("email", nouvelEmail))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blokus/compte?error"));

        verify(utilisateurService).findUtilisateurByUsername(username);
        verify(utilisateurService, never()).modifierNomEmailUtilisateur(any());
    }

    // --------------------------------------------
    // Tests modifierNomUtilisateur
    // --------------------------------------------

    @Test
    void modifierMotDePasseUtilisateur_ancienMotDePasseIncorrect() throws Exception {
        String username = "lucie";
        String oldPassword = "wrongold";
        String encodedOldPassword = "$2a$10$abcdef";
        String newPassword = "newpass";

        UserDetails principal = User.withUsername(username).password(encodedOldPassword).roles("USER").build();
        Utilisateur user = new Utilisateur(username, "lucie@ex.com", encodedOldPassword);

        when(utilisateurService.findUtilisateurByUsername(username)).thenReturn(user);
        when(passwordEncoder.matches(oldPassword, encodedOldPassword)).thenReturn(false);

        mockMvc.perform(post("/blokus/modificationMotDePasse")
                        .with(user(principal))
                        .param("currentPassword", oldPassword)
                        .param("motDePasse", newPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blokus/compte?error"));

        verify(utilisateurService).findUtilisateurByUsername(username);
        verify(utilisateurService, never()).modifierMotDePasseUtilisateur(any());
    }

    @Test
    void modifierMotDePasseUtilisateur_success() throws Exception {
        String username = "lucie";
        String oldPassword = "oldpass";
        String newPassword = "newpass";
        String encodedOldPassword = "$2a$10$abcdef";

        UserDetails principal = User.withUsername(username).password(encodedOldPassword).roles("USER").build();
        Utilisateur user = new Utilisateur(username, "lucie@ex.com", encodedOldPassword);

        when(utilisateurService.findUtilisateurByUsername(username)).thenReturn(user);
        when(passwordEncoder.matches(oldPassword, encodedOldPassword)).thenReturn(true);
        when(passwordEncoder.matches(newPassword, encodedOldPassword)).thenReturn(false);

        doNothing().when(utilisateurService).modifierMotDePasseUtilisateur(user);

        mockMvc.perform(post("/blokus/modificationMotDePasse")
                        .with(user(principal))
                        .param("currentPassword", oldPassword)
                        .param("motDePasse", newPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blokus/connexion?modification"));

        verify(utilisateurService).findUtilisateurByUsername(username);
        verify(utilisateurService).modifierMotDePasseUtilisateur(user);
    }

    @Test
    void modifierMotDePasseUtilisateur_motDePasseIdentique() throws Exception {
        String username = "lucie";
        String oldPassword = "oldpass";
        String encodedOldPassword = "$2a$10$abcdef";
        String newPassword = "oldpass";

        UserDetails principal = User.withUsername(username).password(encodedOldPassword).roles("USER").build();
        Utilisateur user = new Utilisateur(username, "lucie@ex.com", encodedOldPassword);

        when(utilisateurService.findUtilisateurByUsername(username)).thenReturn(user);
        when(passwordEncoder.matches(oldPassword, encodedOldPassword)).thenReturn(true);
        when(passwordEncoder.matches(newPassword, encodedOldPassword)).thenReturn(true);

        mockMvc.perform(post("/blokus/modificationMotDePasse")
                        .with(user(principal))
                        .param("currentPassword", oldPassword)
                        .param("motDePasse", newPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blokus/compte?error"));

        verify(utilisateurService).findUtilisateurByUsername(username);
        verify(utilisateurService, never()).modifierMotDePasseUtilisateur(any());
    }

    @Test
    void modifierMotDePasseUtilisateur_utilisateurNonTrouve() throws Exception {
        String username = "lucie";
        String oldPassword = "oldpass";
        String newPassword = "newpass";

        UserDetails principal = User.withUsername(username).password("irrelevant").roles("USER").build();

        when(utilisateurService.findUtilisateurByUsername(username)).thenReturn(null);

        mockMvc.perform(post("/blokus/modificationMotDePasse")
                        .with(user(principal))
                        .param("currentPassword", oldPassword)
                        .param("motDePasse", newPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blokus/compte?error"));

        verify(utilisateurService).findUtilisateurByUsername(username);
        verify(utilisateurService, never()).modifierMotDePasseUtilisateur(any());
    }

    // --------------------------------------------
    // Tests supprimerUtilisateur
    // --------------------------------------------

    @Test
    void supprimerUtilisateur_succes() throws Exception {
        String username = "lucie";
        UserDetails principal = User.withUsername(username).password("irrelevant").roles("USER").build();
        Utilisateur user = new Utilisateur(username, "lucie@ex.com", "pwd");

        when(utilisateurService.findUtilisateurByUsername(username)).thenReturn(user);

        mockMvc.perform(post("/blokus/suppression")
                        .with(user(principal)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blokus/connexion?suppression"));

        verify(utilisateurService).findUtilisateurByUsername(username);
        verify(utilisateurService).deleteUtilisateur(user);
    }

    @Test
    void supprimerUtilisateur_utilisateurNonTrouve() throws Exception {
        String username = "lucie";
        UserDetails principal = User.withUsername(username).password("irrelevant").roles("USER").build();

        when(utilisateurService.findUtilisateurByUsername(username)).thenReturn(null);

        mockMvc.perform(post("/blokus/suppression")
                        .with(user(principal)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blokus/connexion?error"));

        verify(utilisateurService).findUtilisateurByUsername(username);
        verify(utilisateurService, never()).deleteUtilisateur(any());
    }

    @Test
    void supprimerUtilisateur_exceptionUtilisateurNonTrouve() throws Exception {
        String username = "lucie";
        UserDetails principal = User.withUsername(username).password("irrelevant").roles("USER").build();

        when(utilisateurService.findUtilisateurByUsername(username)).thenThrow(new UtilisateurNonTrouveException(username));

        mockMvc.perform(post("/blokus/suppression")
                        .with(user(principal)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blokus/connexion?error"));

        verify(utilisateurService).findUtilisateurByUsername(username);
        verify(utilisateurService, never()).deleteUtilisateur(any());
    }

}