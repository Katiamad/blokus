package fr.orleans.m1miage.project.s2.projetS2.sevicesTest;

import fr.orleans.m1miage.project.s2.projetS2.exeptions.*;
import fr.orleans.m1miage.project.s2.projetS2.model.Partie;
import fr.orleans.m1miage.project.s2.projetS2.model.PartieJoueur;
import fr.orleans.m1miage.project.s2.projetS2.model.Status;
import fr.orleans.m1miage.project.s2.projetS2.model.Utilisateur;
import fr.orleans.m1miage.project.s2.projetS2.repository.PartieJoueurRepository;
import fr.orleans.m1miage.project.s2.projetS2.repository.PartieRepository;
import fr.orleans.m1miage.project.s2.projetS2.repository.UtilisateurRepository;
import fr.orleans.m1miage.project.s2.projetS2.service.UtilisateurServiceImpl;
import fr.orleans.m1miage.project.s2.projetS2.service.PartieService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtilisateurServiceImplTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private PartieService partieService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PartieJoueurRepository partieJoueurRepository;

    @Mock
    private PartieRepository partieRepository;

    @InjectMocks
    private UtilisateurServiceImpl utilisateurService;


    /**** postConstruct ****/
    @Test
    void postConstruct_CreeTroisUtilisateursEtSeedLesParties() {
        UtilisateurServiceImpl service = new UtilisateurServiceImpl(utilisateurRepository,partieRepository, passwordEncoder, partieService , partieJoueurRepository);

        when(passwordEncoder.encode("securepass456")).thenReturn("ENCODED1");
        when(passwordEncoder.encode("mypassword789")).thenReturn("ENCODED2");
        when(passwordEncoder.encode("Lucie2903")).thenReturn("ENCODED3");

        service.postConstruct();

        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository, times(3)).save(captor.capture());

        List<Utilisateur> saved = captor.getAllValues();
        assertTrue(saved.stream().anyMatch(u -> u.getNom().equals("BossLyliiia") && u.getMotDePasse().equals("ENCODED1")));
        assertTrue(saved.stream().anyMatch(u -> u.getNom().equals("KatiaQueen") && u.getMotDePasse().equals("ENCODED2")));
        assertTrue(saved.stream().anyMatch(u -> u.getNom().equals("lulu") && u.getMotDePasse().equals("ENCODED3")));

        verify(partieService).seedFinishedGamesForUser(any(Utilisateur.class), any(Utilisateur.class));
    }

    @Test
    void findUtilisateurByEmail_ExistingEmail_ReturnsUser() {
        Utilisateur expected = new Utilisateur("test", "test@example.com", "ignore");
        when(utilisateurRepository.findByEmail("test@example.com")).thenReturn(expected);

        Utilisateur actual = utilisateurService.findUtilisateurByEmail("test@example.com");

        assertSame(expected, actual);
    }

    @Test
    void findUtilisateurByEmail_NonExistingEmail_ThrowsException() {
        when(utilisateurRepository.findByEmail("foo@bar.com")).thenReturn(null);

        UtilisateurNonTrouveException ex = assertThrows(
                UtilisateurNonTrouveException.class,
                () -> utilisateurService.findUtilisateurByEmail("foo@bar.com")
        );
        assertEquals("Aucun utilisateur trouvé avec l'email : foo@bar.com", ex.getMessage());
    }

    @Test
    void findUtilisateurByUsername_ExistingUsername_ReturnsUser() {
        Utilisateur expected = new Utilisateur("alice", "a@b.com", "ignore");
        when(utilisateurRepository.findByNomUtilisateur("alice")).thenReturn(Optional.of(expected));

        Utilisateur actual = utilisateurService.findUtilisateurByUsername("alice");

        assertSame(expected, actual);
    }

    @Test
    void findUtilisateurByUsername_NonExistingUsername_ThrowsException() {
        when(utilisateurRepository.findByNomUtilisateur("unknown"))
                .thenReturn(Optional.empty());

        UtilisateurNonTrouveException ex = assertThrows(
                UtilisateurNonTrouveException.class,
                () -> utilisateurService.findUtilisateurByUsername("unknown")
        );
        assertEquals("Aucun utilisateur trouvé avec le nom : unknown", ex.getMessage());
    }

    @Test
    void findUtilisateurByUsernameOuEmail_ExistingUsername_ReturnsUser() {
        Utilisateur expected = new Utilisateur("bob", "b@c.com", "ignore");
        when(utilisateurRepository.findByNomUtilisateur("bob"))
                .thenReturn(Optional.of(expected));

        Utilisateur actual = utilisateurService.findUtilisateurByUsernameOuEmail("bob");

        assertSame(expected, actual);
        verify(utilisateurRepository, never()).findByEmail(anyString());
    }

    @Test
    void findUtilisateurByUsernameOuEmail_NonExisting_ThrowsUsernameNotFound() {
        when(utilisateurRepository.findByNomUtilisateur("nope"))
                .thenReturn(Optional.empty());

        UsernameNotFoundException ex = assertThrows(
                UsernameNotFoundException.class,
                () -> utilisateurService.findUtilisateurByUsernameOuEmail("nope")
        );
        assertEquals("Utilisateur introuvable pour : nope", ex.getMessage());
    }

    /**** ajouterUtilisateur ****/

    @Test
    void ajouterUtilisateur_EmailNull_ThrowsInvalidEmailFormatException() {
        Utilisateur u = new Utilisateur("bob", null, "Password1");
        assertThrows(InvalidEmailFormatException.class,
                () -> utilisateurService.ajouterUtilisateur(u));
    }

    @Test
    void ajouterUtilisateur_EmailEmpty_ThrowsInvalidEmailFormatException() {
        Utilisateur u = new Utilisateur("bob", "   ", "Password1");
        assertThrows(InvalidEmailFormatException.class,
                () -> utilisateurService.ajouterUtilisateur(u));
    }

    @Test
    void ajouterUtilisateur_EmailBadFormat_ThrowsInvalidEmailFormatException() {
        Utilisateur u = new Utilisateur("bob", "bad_email", "Password1");
        assertThrows(InvalidEmailFormatException.class,
                () -> utilisateurService.ajouterUtilisateur(u));
    }

    @Test
    void ajouterUtilisateur_PasswordNull_ThrowsInvalidPasswordException() {
        Utilisateur u = new Utilisateur("bob", "bob@mail.com", null);
        assertThrows(InvalidPasswordException.class,
                () -> utilisateurService.ajouterUtilisateur(u));
    }

    @Test
    void ajouterUtilisateur_PasswordEmpty_ThrowsInvalidPasswordException() {
        Utilisateur u = new Utilisateur("bob", "bob@mail.com", "    ");
        assertThrows(InvalidPasswordException.class,
                () -> utilisateurService.ajouterUtilisateur(u));
    }

    @Test
    void ajouterUtilisateur_PasswordBadFormat_ThrowsInvalidPasswordException() {
        Utilisateur u = new Utilisateur("bob", "bob@mail.com", "nomajuscule1");
        assertThrows(InvalidPasswordException.class,
                () -> utilisateurService.ajouterUtilisateur(u));
    }

    @Test
    void ajouterUtilisateur_EmailAlreadyExists_ThrowsEmailDejaExistantException() {
        Utilisateur u = new Utilisateur("bob", "bob@mail.com", "Password1");
        when(utilisateurRepository.findByEmail("bob@mail.com")).thenReturn(new Utilisateur());
        assertThrows(EmailDejaExistantException.class,
                () -> utilisateurService.ajouterUtilisateur(u));
    }

    @Test
    void ajouterUtilisateur_NomAlreadyExists_ThrowsEmailDejaExistantException() {
        Utilisateur u = new Utilisateur("bob", "bob@mail.com", "Password1");
        when(utilisateurRepository.findByEmail("bob@mail.com")).thenReturn(null);
        when(utilisateurRepository.findByNomUtilisateur("bob")).thenReturn(Optional.of(new Utilisateur()));
        assertThrows(EmailDejaExistantException.class,
                () -> utilisateurService.ajouterUtilisateur(u));
    }

    @Test
    void ajouterUtilisateur_ValidUser_SavesUserWithEncodedPassword() throws Exception, InvalidEmailFormatException {
        Utilisateur u = new Utilisateur("bob", "bob@mail.com", "Password1");
        when(utilisateurRepository.findByEmail("bob@mail.com")).thenReturn(null);
        when(utilisateurRepository.findByNomUtilisateur("bob")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password1")).thenReturn("ENCODED");

        utilisateurService.ajouterUtilisateur(u);

        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(captor.capture());
        Utilisateur saved = captor.getValue();
        assertEquals("bob", saved.getNom());
        assertEquals("bob@mail.com", saved.getEmail());
        assertEquals("ENCODED", saved.getMotDePasse());
    }

    /**** Modifier mdp ****/

    @Test
    void modifierMotDePasseUtilisateur_ValidNewPassword_UpdatesAndSaves() {
        Utilisateur existing = new Utilisateur("bob", "bob@example.com", "ENCODED_OLD");
        existing.setId(UUID.randomUUID());
        Utilisateur input = new Utilisateur();
        input.setId(existing.getId());
        input.setMotDePasse("NewPassword1");

        when(utilisateurRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("NewPassword1", "ENCODED_OLD")).thenReturn(false);
        when(passwordEncoder.encode("NewPassword1")).thenReturn("ENCODED_NEW");

        utilisateurService.modifierMotDePasseUtilisateur(input);

        assertEquals("ENCODED_NEW", existing.getMotDePasse());
        verify(utilisateurRepository).save(existing);
    }

    @Test
    void modifierMotDePasseUtilisateur_NullOrBlankPassword_DoesNothing() {
        Utilisateur existing = new Utilisateur("bob", "bob@example.com", "ENCODED_OLD");
        existing.setId(UUID.randomUUID());
        Utilisateur input = new Utilisateur();
        input.setId(existing.getId());
        input.setMotDePasse(""); // blank

        when(utilisateurRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        utilisateurService.modifierMotDePasseUtilisateur(input);

        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void modifierMotDePasseUtilisateur_InvalidFormat_ThrowsException() {
        Utilisateur existing = new Utilisateur("bob", "bob@example.com", "ENCODED_OLD");
        existing.setId(UUID.randomUUID());
        Utilisateur input = new Utilisateur();
        input.setId(existing.getId());
        input.setMotDePasse("nopass");

        when(utilisateurRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        assertThrows(InvalidPasswordException.class, () ->
                utilisateurService.modifierMotDePasseUtilisateur(input)
        );
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void modifierMotDePasseUtilisateur_SameAsOldPassword_ThrowsException() {
        Utilisateur existing = new Utilisateur("bob", "bob@example.com", "ENCODED_OLD");
        existing.setId(UUID.randomUUID());
        Utilisateur input = new Utilisateur();
        input.setId(existing.getId());
        input.setMotDePasse("SamePassword1");

        when(utilisateurRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("SamePassword1", "ENCODED_OLD")).thenReturn(true);

        assertThrows(InvalidPasswordException.class, () ->
                utilisateurService.modifierMotDePasseUtilisateur(input)
        );
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void modifierMotDePasseUtilisateur_UserNotFound_ThrowsException() {
        Utilisateur input = new Utilisateur();
        input.setId(UUID.randomUUID());
        input.setMotDePasse("ValidPass1");

        when(utilisateurRepository.findById(input.getId())).thenReturn(Optional.empty());

        assertThrows(UtilisateurNonTrouveException.class, () ->
                utilisateurService.modifierMotDePasseUtilisateur(input)
        );
        verify(utilisateurRepository, never()).save(any());
    }

    /**** modifierNomEmailUtilisateur ****/

    @Test
    void modifierNomEmailUtilisateur_ModifieNom_Succes() throws Exception, UtilisateurDejaExistantException, InvalidEmailFormatException {
        Utilisateur existing = new Utilisateur("bob", "bob@old.com", "mdp");
        existing.setId(UUID.randomUUID());
        Utilisateur input = new Utilisateur();
        input.setId(existing.getId());
        input.setNom("alice"); // nouveau nom

        when(utilisateurRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(utilisateurRepository.findByNomUtilisateur("alice")).thenReturn(Optional.empty());

        utilisateurService.modifierNomEmailUtilisateur(input);

        assertEquals("alice", existing.getNom());
        verify(utilisateurRepository).save(existing);
    }

    // Cas : Nom déjà existant -> exception
    @Test
    void modifierNomEmailUtilisateur_NomDejaPris_Exception() {
        Utilisateur existing = new Utilisateur("bob", "bob@old.com", "mdp");
        existing.setId(UUID.randomUUID());
        Utilisateur input = new Utilisateur();
        input.setId(existing.getId());
        input.setNom("alice");

        when(utilisateurRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(utilisateurRepository.findByNomUtilisateur("alice")).thenReturn(Optional.of(new Utilisateur()));

        assertThrows(UtilisateurDejaExistantException.class, () ->
                utilisateurService.modifierNomEmailUtilisateur(input)
        );
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void modifierNomEmailUtilisateur_ModifieEmail_Succes() throws Exception, UtilisateurDejaExistantException, InvalidEmailFormatException {
        Utilisateur existing = new Utilisateur("bob", "bob@old.com", "mdp");
        existing.setId(UUID.randomUUID());
        Utilisateur input = new Utilisateur();
        input.setId(existing.getId());
        input.setEmail("bob@new.com");

        when(utilisateurRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(utilisateurRepository.findByEmail("bob@new.com")).thenReturn(null);

        utilisateurService.modifierNomEmailUtilisateur(input);

        assertEquals("bob@new.com", existing.getEmail());
        verify(utilisateurRepository).save(existing);
    }

    @Test
    void modifierNomEmailUtilisateur_EmailInvalide_Exception() {
        Utilisateur existing = new Utilisateur("bob", "bob@old.com", "mdp");
        existing.setId(UUID.randomUUID());
        Utilisateur input = new Utilisateur();
        input.setId(existing.getId());
        input.setEmail("bad_email_format");

        when(utilisateurRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        assertThrows(InvalidEmailFormatException.class, () ->
                utilisateurService.modifierNomEmailUtilisateur(input)
        );
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void modifierNomEmailUtilisateur_EmailDejaPris_Exception() {
        Utilisateur existing = new Utilisateur("bob", "bob@old.com", "mdp");
        existing.setId(UUID.randomUUID());
        Utilisateur input = new Utilisateur();
        input.setId(existing.getId());
        input.setEmail("bob@new.com");

        when(utilisateurRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(utilisateurRepository.findByEmail("bob@new.com")).thenReturn(new Utilisateur());

        assertThrows(EmailDejaExistantException.class, () ->
                utilisateurService.modifierNomEmailUtilisateur(input)
        );
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void modifierNomEmailUtilisateur_NomEtEmailIdentiquesOuVides_RienNeSePasse() throws Exception, UtilisateurDejaExistantException, InvalidEmailFormatException {
        Utilisateur existing = new Utilisateur("bob", "bob@old.com", "mdp");
        existing.setId(UUID.randomUUID());
        Utilisateur input = new Utilisateur();
        input.setId(existing.getId());
        input.setNom("bob");
        input.setEmail("bob@old.com");

        when(utilisateurRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        utilisateurService.modifierNomEmailUtilisateur(input);

        assertEquals("bob", existing.getNom());
        assertEquals("bob@old.com", existing.getEmail());
        verify(utilisateurRepository).save(existing);
    }

    @Test
    void modifierNomEmailUtilisateur_NomNull_RienNeSePasse() throws Exception, UtilisateurDejaExistantException, InvalidEmailFormatException {
        Utilisateur existing = new Utilisateur("bob", "bob@old.com", "mdp");
        existing.setId(UUID.randomUUID());
        Utilisateur input = new Utilisateur();
        input.setId(existing.getId());
        input.setNom(null);

        when(utilisateurRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        utilisateurService.modifierNomEmailUtilisateur(input);

        assertEquals("bob", existing.getNom());
        verify(utilisateurRepository).save(existing);
    }

    @Test
    void modifierNomEmailUtilisateur_NomVide_RienNeSePasse() throws Exception, UtilisateurDejaExistantException, InvalidEmailFormatException {
        Utilisateur existing = new Utilisateur("bob", "bob@old.com", "mdp");
        existing.setId(UUID.randomUUID());
        Utilisateur input = new Utilisateur();
        input.setId(existing.getId());
        input.setNom("   ");

        when(utilisateurRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        utilisateurService.modifierNomEmailUtilisateur(input);

        assertEquals("bob", existing.getNom());
        verify(utilisateurRepository).save(existing);
    }

    @Test
    void modifierNomEmailUtilisateur_EmailNull_RienNeSePasse() throws Exception, UtilisateurDejaExistantException, InvalidEmailFormatException {
        Utilisateur existing = new Utilisateur("bob", "bob@old.com", "mdp");
        existing.setId(UUID.randomUUID());
        Utilisateur input = new Utilisateur();
        input.setId(existing.getId());
        input.setEmail(null);

        when(utilisateurRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        utilisateurService.modifierNomEmailUtilisateur(input);

        assertEquals("bob@old.com", existing.getEmail());
        verify(utilisateurRepository).save(existing);
    }

    @Test
    void modifierNomEmailUtilisateur_EmailVide_RienNeSePasse() throws Exception, UtilisateurDejaExistantException, InvalidEmailFormatException {
        Utilisateur existing = new Utilisateur("bob", "bob@old.com", "mdp");
        existing.setId(UUID.randomUUID());
        Utilisateur input = new Utilisateur();
        input.setId(existing.getId());
        input.setEmail("   ");

        when(utilisateurRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        utilisateurService.modifierNomEmailUtilisateur(input);

        assertEquals("bob@old.com", existing.getEmail());
        verify(utilisateurRepository).save(existing);
    }

    @Test
    void modifierNomEmailUtilisateur_EmailIdentique_RienNeSePasse() throws Exception, UtilisateurDejaExistantException, InvalidEmailFormatException {
        Utilisateur existing = new Utilisateur("bob", "bob@old.com", "mdp");
        existing.setId(UUID.randomUUID());
        Utilisateur input = new Utilisateur();
        input.setId(existing.getId());
        input.setEmail("bob@old.com");

        when(utilisateurRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        utilisateurService.modifierNomEmailUtilisateur(input);

        assertEquals("bob@old.com", existing.getEmail());
        verify(utilisateurRepository).save(existing);
    }

    @Test
    void modifierNomEmailUtilisateur_NomIdentique_RienNeSePasse() throws Exception, UtilisateurDejaExistantException, InvalidEmailFormatException {
        Utilisateur existing = new Utilisateur("bob", "bob@old.com", "mdp");
        existing.setId(UUID.randomUUID());
        Utilisateur input = new Utilisateur();
        input.setId(existing.getId());
        input.setNom("bob");

        when(utilisateurRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        utilisateurService.modifierNomEmailUtilisateur(input);

        assertEquals("bob", existing.getNom());
        verify(utilisateurRepository).save(existing);
    }

    @Test
    void modifierMotDePasseUtilisateur_MotDePasseNull_DoesNothing() {
        Utilisateur existing = new Utilisateur("bob", "bob@example.com", "ENCODED_OLD");
        existing.setId(UUID.randomUUID());
        Utilisateur input = new Utilisateur();
        input.setId(existing.getId());
        input.setMotDePasse(null);

        when(utilisateurRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        utilisateurService.modifierMotDePasseUtilisateur(input);

        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void modifierMotDePasseUtilisateur_MotDePasseVide_DoesNothing() {
        Utilisateur existing = new Utilisateur("bob", "bob@example.com", "ENCODED_OLD");
        existing.setId(UUID.randomUUID());
        Utilisateur input = new Utilisateur();
        input.setId(existing.getId());
        input.setMotDePasse("   ");

        when(utilisateurRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        utilisateurService.modifierMotDePasseUtilisateur(input);

        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void deleteUtilisateur_UserNotFound_ThrowsException() {
        Utilisateur u = new Utilisateur();
        u.setId(UUID.randomUUID());

        when(utilisateurRepository.existsById(u.getId())).thenReturn(false);

        UtilisateurNonTrouveException ex = assertThrows(UtilisateurNonTrouveException.class,
                () -> utilisateurService.deleteUtilisateur(u));
        assertEquals("Utilisateur introuvable pour suppression, ID : " + u.getId(), ex.getMessage());
        verify(utilisateurRepository, never()).delete(any());
    }

    @Test
    void deleteUtilisateur_SupprimeToutesLesPartiesEnAttenteEtEnCours() {
        Utilisateur u = new Utilisateur();
        u.setId(UUID.randomUUID());

        Partie p1 = new Partie();
        p1.setStatus(Status.EN_ATTENTE);
        Partie p2 = new Partie();
        p2.setStatus(Status.EN_COURS);

        List<Partie> parties = new ArrayList<>();
        parties.add(p1);
        parties.add(p2);

        when(utilisateurRepository.existsById(u.getId())).thenReturn(true);
        when(partieRepository.findByJoueursUtilisateurId(u.getId())).thenReturn(parties);
        when(partieJoueurRepository.findByUtilisateurId(u.getId())).thenReturn(new ArrayList<>());

        utilisateurService.deleteUtilisateur(u);

        verify(partieRepository, times(2)).delete(any(Partie.class));
        verify(utilisateurRepository).delete(u);
    }

    @Test
    void deleteUtilisateur_SupprimePartieJoueurSiPartieTerminee() {
        Utilisateur u = new Utilisateur();
        u.setId(UUID.randomUUID());

        Partie terminee = new Partie();
        terminee.setStatus(Status.TERMINEE);

        PartieJoueur pj = new PartieJoueur(terminee,u,"red");

        List<Partie> parties = new ArrayList<>();
        parties.add(terminee);

        List<PartieJoueur> joueurs = new ArrayList<>();
        joueurs.add(pj);

        when(utilisateurRepository.existsById(u.getId())).thenReturn(true);
        when(partieRepository.findByJoueursUtilisateurId(u.getId())).thenReturn(parties);
        when(partieJoueurRepository.findByUtilisateurId(u.getId())).thenReturn(joueurs);

        utilisateurService.deleteUtilisateur(u);

        verify(partieJoueurRepository).delete(pj);
        verify(partieRepository, never()).delete(terminee);
        verify(utilisateurRepository).delete(u);
    }

    @Test
    void deleteUtilisateur_ToutVide_RienDeSpecial() {
        Utilisateur u = new Utilisateur();
        u.setId(UUID.randomUUID());

        when(utilisateurRepository.existsById(u.getId())).thenReturn(true);
        when(partieRepository.findByJoueursUtilisateurId(u.getId())).thenReturn(new ArrayList<>());
        when(partieJoueurRepository.findByUtilisateurId(u.getId())).thenReturn(new ArrayList<>());

        utilisateurService.deleteUtilisateur(u);

        verify(partieRepository, never()).delete(any());
        verify(partieJoueurRepository, never()).delete(any());
        verify(utilisateurRepository).delete(u);
    }

    @Test
    void deleteUtilisateur_PartiesMixtes_SupprimeCeQuIlFaut() {
        Utilisateur u = new Utilisateur();
        u.setId(UUID.randomUUID());

        Partie enAttente = new Partie();
        enAttente.setStatus(Status.EN_ATTENTE);
        Partie enCours = new Partie();
        enCours.setStatus(Status.EN_COURS);
        Partie finie = new Partie();
        finie.setStatus(Status.TERMINEE);

        List<Partie> parties = List.of(enAttente, enCours, finie);

        PartieJoueur pj1 = new PartieJoueur(finie,u, "red");

        List<PartieJoueur> joueurs = List.of(pj1);

        when(utilisateurRepository.existsById(u.getId())).thenReturn(true);
        when(partieRepository.findByJoueursUtilisateurId(u.getId())).thenReturn(parties);
        when(partieJoueurRepository.findByUtilisateurId(u.getId())).thenReturn(joueurs);

        utilisateurService.deleteUtilisateur(u);

        verify(partieRepository).delete(enAttente);
        verify(partieRepository).delete(enCours);
        verify(partieRepository, never()).delete(finie);

        verify(partieJoueurRepository).delete(pj1);

        verify(utilisateurRepository).delete(u);
    }

    @Test
    void deleteUtilisateur_SupprimeUniquementPartieJoueurSurPartieTerminee() {
        Utilisateur u = new Utilisateur();
        u.setId(UUID.randomUUID());

        Partie terminee = new Partie();
        terminee.setStatus(Status.TERMINEE);

        Partie enCours = new Partie();
        enCours.setStatus(Status.EN_COURS);

        PartieJoueur pjTerminee = new PartieJoueur(terminee,u,"red");

        PartieJoueur pjEnCours = new PartieJoueur(enCours,u,"red");

        List<Partie> parties = List.of(terminee, enCours);
        List<PartieJoueur> joueurs = List.of(pjTerminee, pjEnCours);

        when(utilisateurRepository.existsById(u.getId())).thenReturn(true);
        when(partieRepository.findByJoueursUtilisateurId(u.getId())).thenReturn(parties);
        when(partieJoueurRepository.findByUtilisateurId(u.getId())).thenReturn(joueurs);

        utilisateurService.deleteUtilisateur(u);

        verify(partieJoueurRepository).delete(pjTerminee);
        verify(partieJoueurRepository, never()).delete(pjEnCours);

        verify(partieRepository).delete(enCours);
        verify(partieRepository, never()).delete(terminee);

        verify(utilisateurRepository).delete(u);
    }

}
