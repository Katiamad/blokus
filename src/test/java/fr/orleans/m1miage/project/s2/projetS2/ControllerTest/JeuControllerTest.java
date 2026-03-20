package fr.orleans.m1miage.project.s2.projetS2.ControllerTest;

import fr.orleans.m1miage.project.s2.projetS2.controller.JeuControleur;
import fr.orleans.m1miage.project.s2.projetS2.exeptions.NomPartieInvalideException;
import fr.orleans.m1miage.project.s2.projetS2.exeptions.PartieNonRejoignableException;
import fr.orleans.m1miage.project.s2.projetS2.model.*;
import fr.orleans.m1miage.project.s2.projetS2.service.PartieService;
import fr.orleans.m1miage.project.s2.projetS2.service.StatistiquesService;
import fr.orleans.m1miage.project.s2.projetS2.service.UtilisateurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpServletRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JeuControleurTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private UtilisateurService utilisateurService;

    @Mock
    private StatistiquesService statistiquesService;

    @Mock
    private PartieService partieService;

    @Mock
    private Model model;

    @Mock
    private UserDetails userDetails;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private JeuControleur jeuControleur;


    private UUID partieId;
    private UUID userId;
    private Partie partie;
    private Utilisateur utilisateur;
    private PartieJoueur partieJoueur;

    @BeforeEach
    void setUp() {
        partieId = UUID.randomUUID();
        userId = UUID.randomUUID();

        utilisateur = new Utilisateur();
        utilisateur.setId(userId);
        utilisateur.setNom("TestUser");

        partie = new Partie("TestPartie");
        partie.setId(partieId);
        partie.setStatus(Status.EN_ATTENTE);

        partieJoueur = new PartieJoueur(partie, utilisateur, "RED");
        partieJoueur.setId(UUID.randomUUID()); 

        partie.getJoueurs().add(partieJoueur);
        jeuControleur.messagingTemplate = messagingTemplate;
        lenient().when(userDetails.getUsername()).thenReturn("testuser");
        lenient().when(utilisateurService.findUtilisateurByUsername("testuser")).thenReturn(utilisateur);
    }


    @Test
    void testDemarrerLaPartie() {
        when(partieService.demarrerPartie(partieId)).thenReturn(partie);

        String result = jeuControleur.DemarrerLaPartie(model, partieId, userDetails);

        assertEquals("redirect:/blokus/partie-en-cours/" + partieId, result);
        verify(partieService).demarrerPartie(partieId);
        verify(messagingTemplate).convertAndSend(eq("/topic/status-updates/" + partieId), anyMap());
    }

    @Test
    void testNouvellePartie_PartieNonTerminee() {
        partie.setStatus(Status.EN_COURS);

        when(partieService.findById(partieId)).thenReturn(partie);

        String result = jeuControleur.nouvellePartie(model, partieId, userDetails);

        assertEquals("partie-de-jeu", result);
        verify(model).addAttribute(eq("partie"), eq(partie));
        verify(model).addAttribute(eq("utilisateur"), eq(utilisateur));
    }

    @Test
    void testNouvellePartie_PartieTerminee() {
        partie.setStatus(Status.TERMINEE);

        when(partieService.findById(partieId)).thenReturn(partie);

        String result = jeuControleur.nouvellePartie(model, partieId, userDetails);

        assertEquals("redirect:/blokus/fin/" + partieId, result);
    }

    @Test
    void nouvellePartie_joueurNonTrouve_exceptionRuntime() {
        partie.setStatus(Status.EN_COURS);
        partie.getJoueurs().clear(); 
        when(partieService.findById(partieId)).thenReturn(partie);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                jeuControleur.nouvellePartie(model, partieId, userDetails)
        );
        assertEquals("Joueur non trouvé dans la partie", exception.getMessage());
    }

    @Test
    void nouvellePartie_avecTimer_ajouteAttributsTimerEtStartTime() {
        partie.setStatus(Status.EN_COURS);
        partie.setModeJeu(ModeJeu.AVEC_TIMER);

        LocalDateTime startTime = LocalDateTime.of(2025, 5, 25, 19, 0);
        partie.setStartTime(startTime);
        partie.setTempsMin(60);

        when(partieService.findById(partieId)).thenReturn(partie);

        PartieJoueur pj = new PartieJoueur(partie, utilisateur, "RED");
        pj.setId(UUID.randomUUID());
        partie.getJoueurs().clear();
        partie.getJoueurs().add(pj);

        String result = jeuControleur.nouvellePartie(model, partieId, userDetails);

        assertEquals("partie-de-jeu", result);
        verify(model).addAttribute(eq("startTime"), any(java.util.Date.class));
        verify(model).addAttribute("timer", 60);
    }

    @Test
    void nouvellePartie_joueurTrouveParIdFonctionne() {
        partie.setStatus(Status.EN_COURS);

        Utilisateur memeUtilisateur = utilisateur;
        PartieJoueur pj = new PartieJoueur(partie, memeUtilisateur, "RED");
        pj.setId(UUID.randomUUID());
        partie.getJoueurs().clear();
        partie.getJoueurs().add(pj);

        when(partieService.findById(partieId)).thenReturn(partie);

        String result = jeuControleur.nouvellePartie(model, partieId, userDetails);

        assertEquals("partie-de-jeu", result);
        verify(model).addAttribute("partie", partie);
        verify(model).addAttribute("utilisateur", memeUtilisateur);
    }

    @Test
    void nouvellePartie_joueurNonTrouveSiIdEgalMaisObjetsDifferents() {
        partie.setStatus(Status.EN_COURS);

        UUID idUser = utilisateur.getId();
        Utilisateur autre = new Utilisateur();
        autre.setId(UUID.fromString(idUser.toString())); 

        PartieJoueur pj = new PartieJoueur(partie, autre, "RED");
        pj.setId(UUID.randomUUID());
        partie.getJoueurs().clear();
        partie.getJoueurs().add(pj);

        when(partieService.findById(partieId)).thenReturn(partie);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                jeuControleur.nouvellePartie(model, partieId, userDetails)
        );
        assertEquals("Joueur non trouvé dans la partie", exception.getMessage());
    }

    @Test
    void testLancerPartie_Success() throws NomPartieInvalideException {
        when(partieService.lancerPartie(eq(userId), eq("TestPartie"), anyString(), anyInt()))
                .thenReturn(partie);

        String result = jeuControleur.lancerPartie("TestPartie", userDetails, "CLASSIQUE", 0, model);

        assertEquals("redirect:/blokus/partie/" + partieId, result);
    }

    @Test
    void lancerPartie_lanceException_nonAttrapee_rethrowRuntimeException() {
        UtilisateurService utilisateurService = mock(UtilisateurService.class);
        PartieService partieService = mock(PartieService.class);
        StatistiquesService statistiquesService = mock(StatistiquesService.class);
        JeuControleur ctrl = new JeuControleur(utilisateurService, statistiquesService, partieService);

        String username = "lucie";
        UserDetails principal = User.withUsername(username).password("pass").roles("USER").build();
        Utilisateur user = new Utilisateur(username, "lucie@ex.com", "pass");
        user.setId(java.util.UUID.randomUUID());

        when(utilisateurService.findUtilisateurByUsername(username)).thenReturn(user);
        when(partieService.lancerPartie(any(), anyString(), anyString(), anyInt()))
                .thenThrow(new NullPointerException("Unexpected null!"));

        Model model = mock(Model.class);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            ctrl.lancerPartie("nom", principal, "CLASSIQUE", 0, model);
        });
        assertTrue(thrown.getCause() instanceof NullPointerException);
        assertEquals("Unexpected null!", thrown.getCause().getMessage());
    }

    @Test
    void testLancerPartie_NomInvalide() throws NomPartieInvalideException {
        when(partieService.lancerPartie(eq(userId), eq(""), anyString(), anyInt()))
                .thenThrow(new NomPartieInvalideException("Nom invalide"));

        String result = jeuControleur.lancerPartie("", userDetails, "CLASSIQUE", 0, model);

        verify(model).addAttribute(eq("error"), contains("Nom invalide"));
        assertEquals("home", result);
    }

    @Test
    void testRejoindreOuLancerPartie_DejaDansPartie() throws PartieNonRejoignableException {
        when(partieService.estDansLaPartie(partieId, userId)).thenReturn(true);
        when(partieService.findById(partieId)).thenReturn(partie);

        String result = jeuControleur.rejoindreOuLancerPartie(userDetails, model, partieId, null);

        assertEquals("attente", result);
        verify(model).addAttribute(eq("partie"), eq(partie));
    }

    @Test
    void testRejoindreOuLancerPartie_RejoindrePartie() throws PartieNonRejoignableException {
        when(partieService.estDansLaPartie(partieId, userId)).thenReturn(false);
        when(partieService.rejoindrePartie(partieId, userId)).thenReturn(partie);

        try (MockedStatic<URLEncoder> mocked = mockStatic(URLEncoder.class)) {
            mocked.when(() -> URLEncoder.encode(anyString(), eq(StandardCharsets.UTF_8)))
                    .thenReturn("encoded-error");

            String result = jeuControleur.rejoindreOuLancerPartie(userDetails, model, partieId, null);
            assertEquals("attente", result);
        }
    }

    @Test
    void testJouerPiece_Success() {
        when(partieService.validerJeu(eq(partieId), anyList(), anyInt())).thenReturn(partie);
        when(request.getParameterValues("positionOccupee")).thenReturn(new String[]{"1,2", "2,3"});

        String result = jeuControleur.jouerPiece(partieId, 1, model, userDetails, request);

        verify(partieService).validerJeu(eq(partieId), anyList(), eq(1));
        verify(messagingTemplate).convertAndSend(eq("/topic/grid-updates/" + partieId), anyMap());
    }

    @Test
    void testJouerPiece_InvalidPositions() {
        when(request.getParameterValues("positionOccupee")).thenReturn(new String[]{"invalid"});

        String result = jeuControleur.jouerPiece(partieId, 1, model, userDetails, request);

        assertEquals("partie-de-jeu", result);
        verify(model).addAttribute(eq("error"), anyString());
    }

    @Test
    void jouerPiece_aucunePositionRecue_afficheErreurEtResteSurPage() {
        String username = "lucie";
        UserDetails principal = User.withUsername(username).password("pass").roles("USER").build();
        Utilisateur user = new Utilisateur(username, "lucie@ex.com", "pass");
        user.setId(UUID.randomUUID());

        when(utilisateurService.findUtilisateurByUsername(username)).thenReturn(user);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getParameterValues("positionOccupee")).thenReturn(null);

        Model model = mock(Model.class);

        String view = new JeuControleur(utilisateurService, statistiquesService, partieService).jouerPiece(
                UUID.randomUUID(), 1, model, principal, req);

        assertEquals("partie-de-jeu", view);
        verify(model).addAttribute(eq("error"), contains("Aucune position reçue"));
    }

    @Test
    void jouerPiece_coordNonNumerique_afficheErreurEtResteSurPage() {
        String username = "lucie";
        UserDetails principal = User.withUsername(username).password("pass").roles("USER").build();
        Utilisateur user = new Utilisateur(username, "lucie@ex.com", "pass");
        user.setId(UUID.randomUUID());

        when(utilisateurService.findUtilisateurByUsername(username)).thenReturn(user);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getParameterValues("positionOccupee")).thenReturn(new String[]{"a,b"});

        Model model = mock(Model.class);

        String view = new JeuControleur(utilisateurService, statistiquesService,partieService).jouerPiece(
                UUID.randomUUID(), 1, model, principal, req);

        assertEquals("partie-de-jeu", view);
        verify(model).addAttribute(eq("error"), contains("Coordonnée non numérique"));
    }

    @Test
    void jouerPieceAjax_siNextEstRobot_unRobotJoueEtSendGridUpdateAppelee() {
        UtilisateurService utilisateurService = mock(UtilisateurService.class);
        PartieService partieService = mock(PartieService.class);
        StatistiquesService statistiquesService = mock(StatistiquesService.class);
        JeuControleur ctrl = spy(new JeuControleur(utilisateurService, statistiquesService, partieService));

        UserDetails userDetails = User.withUsername("lucie").password("pw").roles("USER").build();
        Utilisateur user = new Utilisateur("lucie", "lucie@ex.com", "pw");
        when(utilisateurService.findUtilisateurByUsername("lucie")).thenReturn(user);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterValues("positionOccupee")).thenReturn(new String[]{"1,2"});

        PartieRobot robot = mock(PartieRobot.class);
        Partie partie = mock(Partie.class);

        when(partieService.validerJeu(any(), anyList(), anyInt())).thenReturn(partie);
        when(partie.getStatus()).thenReturn(Status.EN_COURS); 
        when(partieService.nextIsHuman(partie)).thenReturn(false); 
        when(partie.getRobots()).thenReturn(Set.of(robot));
        when(partie.getGrille()).thenReturn(new int[2][2]);

        doNothing().when(ctrl).sendGridUpdate(any(), any(), any());
        doNothing().when(partieService).jouerRobot(any(), any());

        Map<String, Object> result = ctrl.jouerPieceAjax(UUID.randomUUID(), 1, userDetails, request);

        assertEquals(Boolean.TRUE, result.get("success"));
        verify(partieService).jouerRobot(robot, partie);
        verify(ctrl, atLeast(2)).sendGridUpdate(any(), any(), any());
    }
    @Test
    void jouerPieceAjax_aucunePositionRecue_returnError2() {
        UtilisateurService utilisateurService = mock(UtilisateurService.class);
        PartieService partieService = mock(PartieService.class);
        StatistiquesService statistiquesService = mock(StatistiquesService.class);
        JeuControleur ctrl = new JeuControleur(utilisateurService, statistiquesService, partieService);

        UserDetails userDetails = User.withUsername("lucie").password("pw").roles("USER").build();
        Utilisateur user = new Utilisateur("lucie", "lucie@ex.com", "pw");
        when(utilisateurService.findUtilisateurByUsername("lucie")).thenReturn(user);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterValues("positionOccupee")).thenReturn(null);

        Map<String, Object> result = ctrl.jouerPieceAjax(UUID.randomUUID(), 1, userDetails, request);

        assertEquals("Aucune position reçue", result.get("error"));

        when(request.getParameterValues("positionOccupee")).thenReturn(new String[0]);
        result = ctrl.jouerPieceAjax(UUID.randomUUID(), 1, userDetails, request);
        assertEquals("Aucune position reçue", result.get("error"));
    }
    @Test
    void jouerPieceAjax_nextIsHumanFalse_robotsJouentEtSendGridUpdateAppelee() {
        
        UtilisateurService utilisateurService = mock(UtilisateurService.class);
        PartieService partieService = mock(PartieService.class);
        StatistiquesService statistiquesService = mock(StatistiquesService.class);
        JeuControleur ctrl = spy(new JeuControleur(utilisateurService, statistiquesService, partieService));

        UserDetails userDetails = User.withUsername("lucie").password("pw").roles("USER").build();
        Utilisateur user = new Utilisateur("lucie", "lucie@ex.com", "pw");
        when(utilisateurService.findUtilisateurByUsername("lucie")).thenReturn(user);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterValues("positionOccupee")).thenReturn(new String[]{"1,2"});

        PartieRobot robot1 = mock(PartieRobot.class);
        PartieRobot robot2 = mock(PartieRobot.class);
        Partie partie = mock(Partie.class);

        when(partieService.validerJeu(any(), anyList(), anyInt())).thenReturn(partie);
        when(partie.getStatus()).thenReturn(Status.EN_COURS);
        when(partieService.nextIsHuman(partie)).thenReturn(false);
        when(partie.getRobots()).thenReturn(Set.of(robot1, robot2));
        when(partie.getGrille()).thenReturn(new int[4][4]);

        doNothing().when(ctrl).sendGridUpdate(any(), any(), any());
        doNothing().when(partieService).jouerRobot(any(), any());

        Map<String, Object> result = ctrl.jouerPieceAjax(UUID.randomUUID(), 1, userDetails, request);

        assertEquals(Boolean.TRUE, result.get("success"));
        verify(partieService).jouerRobot(robot1, partie);
        verify(partieService).jouerRobot(robot2, partie);
        verify(ctrl, atLeast(3)).sendGridUpdate(any(), any(), any());
    }


    @Test
    void jouerPiece_partieTerminee_redirigeVersFin() {
        String username = "lucie";
        UserDetails principal = User.withUsername(username).password("pass").roles("USER").build();
        Utilisateur user = new Utilisateur(username, "lucie@ex.com", "pass");
        user.setId(UUID.randomUUID());

        UUID partieId = UUID.randomUUID();
        Partie partie = new Partie("TestPartie");
        partie.setId(partieId);
        partie.setStatus(Status.TERMINEE);

        when(utilisateurService.findUtilisateurByUsername(username)).thenReturn(user);
        when(partieService.validerJeu(eq(partieId), anyList(), anyInt())).thenReturn(partie);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getParameterValues("positionOccupee")).thenReturn(new String[]{"1,2"});

        Model model = mock(Model.class);

        String view = new JeuControleur(utilisateurService, statistiquesService,partieService).jouerPiece(
                partieId, 1, model, principal, req);

        assertEquals("redirect:/blokus/fin/" + partieId, view);
    }

    @Test
    void jouerPiece_nextIsHumanFalse_boucleSurRobotsEtRedirige() {
        String username = "lucie";
        UserDetails principal = User.withUsername(username).password("pass").roles("USER").build();
        Utilisateur user = new Utilisateur(username, "lucie@ex.com", "pass");
        user.setId(UUID.randomUUID());

        UUID partieId = UUID.randomUUID();
        Partie partie = new Partie("TestPartie");
        partie.setId(partieId);
        partie.setStatus(Status.EN_COURS);

        PartieRobot robot1 = mock(PartieRobot.class);
        PartieRobot robot2 = mock(PartieRobot.class);
        partie.setRobots(new HashSet<>(List.of(robot1, robot2)));

        when(utilisateurService.findUtilisateurByUsername(username)).thenReturn(user);
        when(partieService.validerJeu(eq(partieId), anyList(), anyInt())).thenReturn(partie);
        when(partieService.nextIsHuman(partie)).thenReturn(false);

        doNothing().when(partieService).jouerRobot(any(), eq(partie));

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getParameterValues("positionOccupee")).thenReturn(new String[]{"1,2"});

        Model model = mock(Model.class);

        JeuControleur ctrl = new JeuControleur(utilisateurService, statistiquesService, partieService);
        ctrl.messagingTemplate = messagingTemplate; 

        String view = ctrl.jouerPiece(partieId, 1, model, principal, req);

        assertEquals("redirect:/blokus/partie-en-cours/" + partieId, view);

        verify(partieService, times(2)).jouerRobot(any(), eq(partie));
    }

    @Test
    void jouerPiece_nextIsHuman_redirigeVersPartieEnCours() {
        String username = "lucie";
        UserDetails principal = User.withUsername(username).password("pass").roles("USER").build();
        Utilisateur user = new Utilisateur(username, "lucie@ex.com", "pass");
        user.setId(UUID.randomUUID());

        UUID partieId = UUID.randomUUID();
        Partie partie = new Partie("TestPartie");
        partie.setId(partieId);
        partie.setStatus(Status.EN_COURS);

        when(utilisateurService.findUtilisateurByUsername(username)).thenReturn(user);
        when(partieService.validerJeu(eq(partieId), anyList(), anyInt())).thenReturn(partie);
        when(partieService.nextIsHuman(partie)).thenReturn(true);

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getParameterValues("positionOccupee")).thenReturn(new String[]{"1,2"});

        Model model = mock(Model.class);

        JeuControleur ctrl = new JeuControleur(utilisateurService, statistiquesService, partieService);
        ctrl.messagingTemplate = messagingTemplate;

        String view = ctrl.jouerPiece(partieId, 1, model, principal, req);

        assertEquals("redirect:/blokus/partie-en-cours/" + partieId, view);

        verify(partieService, never()).jouerRobot(any(), any());
    }



    @Test
    void testJouerPieceAjax_Success() {
        when(partieService.validerJeu(eq(partieId), anyList(), anyInt())).thenReturn(partie);
        when(request.getParameterValues("positionOccupee")).thenReturn(new String[]{"1,2", "2,3"});

        Map<String, Object> result = jeuControleur.jouerPieceAjax(partieId, 1, userDetails, request);

        assertTrue((Boolean) result.get("success"));
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/grid-updates/" + partieId), anyMap());
    }

    @Test
    void testJouerPieceAjax_PartieTerminee() {
        partie.setStatus(Status.TERMINEE);
        when(partieService.validerJeu(eq(partieId), anyList(), anyInt())).thenReturn(partie);
        when(request.getParameterValues("positionOccupee")).thenReturn(new String[]{"1,2", "2,3"});

        Map<String, Object> result = jeuControleur.jouerPieceAjax(partieId, 1, userDetails, request);

        assertTrue((Boolean) result.get("fin"));
        verify(messagingTemplate).convertAndSend(eq("/topic/fin-partie/" + partieId), anyMap());
    }

    @Test
    void jouerPieceAjax_aucunePositionRecue_returnError() {
        UtilisateurService utilisateurService = mock(UtilisateurService.class);
        PartieService partieService = mock(PartieService.class);
        StatistiquesService statistiquesService = mock(StatistiquesService.class);
        JeuControleur ctrl = new JeuControleur(utilisateurService, statistiquesService, partieService);

        UserDetails userDetails = User.withUsername("lucie").password("pw").roles("USER").build();
        Utilisateur user = new Utilisateur("lucie", "lucie@ex.com", "pw");
        when(utilisateurService.findUtilisateurByUsername("lucie")).thenReturn(user);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterValues("positionOccupee")).thenReturn(null); 

        Map<String, Object> result = ctrl.jouerPieceAjax(UUID.randomUUID(), 1, userDetails, request);

        assertEquals("Aucune position reçue", result.get("error"));
    }

    @Test
    void jouerPieceAjax_coordonneesMalFormees_returnError() {
        UtilisateurService utilisateurService = mock(UtilisateurService.class);
        PartieService partieService = mock(PartieService.class);
        StatistiquesService statistiquesService = mock(StatistiquesService.class);
        JeuControleur ctrl = new JeuControleur(utilisateurService, statistiquesService, partieService);

        UserDetails userDetails = User.withUsername("lucie").password("pw").roles("USER").build();
        Utilisateur user = new Utilisateur("lucie", "lucie@ex.com", "pw");
        when(utilisateurService.findUtilisateurByUsername("lucie")).thenReturn(user);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterValues("positionOccupee")).thenReturn(new String[]{"12"});

        Map<String, Object> result = ctrl.jouerPieceAjax(UUID.randomUUID(), 1, userDetails, request);

        assertTrue(result.get("error").toString().startsWith("Coordonnées mal formées"));
    }

    @Test
    void jouerPieceAjax_coordonneesNonNumeriques_returnError() {
        UtilisateurService utilisateurService = mock(UtilisateurService.class);
        PartieService partieService = mock(PartieService.class);
        StatistiquesService statistiquesService = mock(StatistiquesService.class);
        JeuControleur ctrl = new JeuControleur(utilisateurService, statistiquesService, partieService);

        UserDetails userDetails = User.withUsername("lucie").password("pw").roles("USER").build();
        Utilisateur user = new Utilisateur("lucie", "lucie@ex.com", "pw");
        when(utilisateurService.findUtilisateurByUsername("lucie")).thenReturn(user);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterValues("positionOccupee")).thenReturn(new String[]{"a,b"}); 

        Map<String, Object> result = ctrl.jouerPieceAjax(UUID.randomUUID(), 1, userDetails, request);

        assertTrue(result.get("error").toString().startsWith("Coordonnée non numérique"));
    }

    @Test
    void jouerPieceAjax_placementInvalide_returnError() {
        UtilisateurService utilisateurService = mock(UtilisateurService.class);
        PartieService partieService = mock(PartieService.class);
        StatistiquesService statistiquesService = mock(StatistiquesService.class);
        JeuControleur ctrl = new JeuControleur(utilisateurService, statistiquesService, partieService);

        UserDetails userDetails = User.withUsername("lucie").password("pw").roles("USER").build();
        Utilisateur user = new Utilisateur("lucie", "lucie@ex.com", "pw");
        when(utilisateurService.findUtilisateurByUsername("lucie")).thenReturn(user);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterValues("positionOccupee")).thenReturn(new String[]{"1,2"});

        when(partieService.validerJeu(any(), anyList(), anyInt())).thenThrow(new IllegalArgumentException());

        Map<String, Object> result = ctrl.jouerPieceAjax(UUID.randomUUID(), 1, userDetails, request);

        assertEquals("Placement invalide", result.get("error"));
    }

    @Test
    void jouerPieceAjax_siNextEstRobot_robotJoueEtSuccess() {
        UtilisateurService utilisateurService = mock(UtilisateurService.class);
        PartieService partieService = mock(PartieService.class);
        StatistiquesService statistiquesService = mock(StatistiquesService.class);
        JeuControleur ctrl = spy(new JeuControleur(utilisateurService, statistiquesService, partieService));

        UserDetails userDetails = User.withUsername("lucie").password("pw").roles("USER").build();
        Utilisateur user = new Utilisateur("lucie", "lucie@ex.com", "pw");
        when(utilisateurService.findUtilisateurByUsername("lucie")).thenReturn(user);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterValues("positionOccupee")).thenReturn(new String[]{"1,2"});

        PartieRobot robot = mock(PartieRobot.class);
        Partie partie = mock(Partie.class);

        when(partieService.validerJeu(any(), anyList(), anyInt())).thenReturn(partie);
        when(partie.getStatus()).thenReturn(Status.EN_COURS); 
        when(partieService.nextIsHuman(partie)).thenReturn(false);
        when(partie.getRobots()).thenReturn(Set.of(robot));
        when(partie.getGrille()).thenReturn(new int[4][4]); 

        doNothing().when(ctrl).sendGridUpdate(any(), any(), any());
        doNothing().when(partieService).jouerRobot(any(), any());

        Map<String, Object> result = ctrl.jouerPieceAjax(UUID.randomUUID(), 1, userDetails, request);

        assertEquals(Boolean.TRUE, result.get("success"));
        verify(partieService).jouerRobot(robot, partie);
        verify(ctrl, atLeastOnce()).sendGridUpdate(any(), any(), any());
    }

    @Test
    void testAfficherFinPartie() {
        partie.setStatus(Status.TERMINEE);
        partie.setGagnants(List.of(partieJoueur.getId()));

        when(partieService.findById(partieId)).thenReturn(partie);

        String result = jeuControleur.afficherFinPartie(partieId, model, userDetails);

        assertEquals("fin-de-partie", result);
        verify(model).addAttribute(eq("nomsGagnants"), anyList());
        verify(model).addAttribute(eq("gagnantPoints"), anyInt());
    }

    @Test
    void afficherFinPartie_gagnantEstRobot_nomRobotAjouteALaListeEtPointsCorrects() {
        UtilisateurService utilisateurService = mock(UtilisateurService.class);
        PartieService partieService = mock(PartieService.class);
        StatistiquesService statistiquesService = mock(StatistiquesService.class);

        JeuControleur ctrl = new JeuControleur(utilisateurService, statistiquesService, partieService);

        UUID partieId = UUID.randomUUID();
        UUID robotId = UUID.randomUUID();

        PartieRobot robotGagnant = mock(PartieRobot.class);
        when(robotGagnant.getId()).thenReturn(robotId);
        when(robotGagnant.getNom()).thenReturn("Robbie");
        when(robotGagnant.getPoints()).thenReturn(66);

        Partie partie = mock(Partie.class);
        when(partie.getJoueurs()).thenReturn(Collections.emptyList());
        when(partie.getRobots()).thenReturn(Set.of(robotGagnant));
        when(partie.getGagnants()).thenReturn(List.of(robotId));

        UserDetails userDetails = User.withUsername("lucie").password("pw").roles("USER").build();
        Utilisateur user = new Utilisateur("lucie", "lucie@ex.com", "pw");
        when(utilisateurService.findUtilisateurByUsername("lucie")).thenReturn(user);
        when(partieService.findById(partieId)).thenReturn(partie);

        Model model = mock(Model.class);

        String view = ctrl.afficherFinPartie(partieId, model, userDetails);

        assertEquals("fin-de-partie", view);
        verify(model).addAttribute(eq("nomsGagnants"), argThat(list -> ((List<String>)list).contains("Robbie")));
        verify(model).addAttribute(eq("gagnantPoints"), eq(66));
    }

    @Test
    void testMesPartiesParEtat_rangMapRempliSiJoueurCorrespond() {
        UUID userId = UUID.randomUUID();
        Utilisateur user = new Utilisateur();
        user.setId(userId);
        user.setNom("UserA");

        Partie partieTerminee = new Partie("Terminee");
        partieTerminee.setId(UUID.randomUUID());
        partieTerminee.setStatus(Status.TERMINEE);

        PartieJoueur pj = mock(PartieJoueur.class);
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(userId); 
        when(pj.getUtilisateur()).thenReturn(utilisateur);
        when(pj.getRang()).thenReturn("2ème");

        partieTerminee.setJoueurs(new ArrayList<>(List.of(pj)));

        UserDetails userDetails = User.withUsername("lucie").password("pw").roles("USER").build();
        when(utilisateurService.findUtilisateurByUsername("lucie")).thenReturn(user);

        Map<Status, List<Partie>> partiesMap = new HashMap<>();
        partiesMap.put(Status.EN_ATTENTE, new ArrayList<>());
        partiesMap.put(Status.EN_COURS, new ArrayList<>());
        partiesMap.put(Status.TERMINEE, List.of(partieTerminee));

        when(partieService.getPartiesGroupByEtat(userId)).thenReturn(partiesMap);

        Model model = mock(Model.class);

        String view = jeuControleur.mesPartiesParEtat(model, userDetails);

        assertEquals("parties", view);

        verify(model).addAttribute(eq("rangMap"), argThat((Map<UUID, String> map) ->
                map.containsKey(partieTerminee.getId()) && "2ème".equals(map.get(partieTerminee.getId()))
        ));
    }

    @Test
    void testMesPartiesParEtat() {
        Map<Status, List<Partie>> partiesMap = new HashMap<>();
        partiesMap.put(Status.EN_ATTENTE, List.of(new Partie("Attente")));
        partiesMap.put(Status.EN_COURS, List.of(new Partie("En cours")));
        partiesMap.put(Status.TERMINEE, List.of(partie));

        when(partieService.getPartiesGroupByEtat(userId)).thenReturn(partiesMap);

        String result = jeuControleur.mesPartiesParEtat(model, userDetails);

        assertEquals("parties", result);
        verify(model).addAttribute(eq("enAttente"), anyList());
        verify(model).addAttribute(eq("enCours"), anyList());
        verify(model).addAttribute(eq("terminees"), anyList());
        verify(model).addAttribute(eq("rangMap"), anyMap());
    }

    @Test
    void mesPartiesParEtat_aucunJoueurDansLaPartieTerminee_rangMapNull() {
        UUID userId = UUID.randomUUID();
        Utilisateur user = new Utilisateur();
        user.setId(userId);

        Partie partieTerminee = new Partie("Terminee");
        partieTerminee.setId(UUID.randomUUID());
        partieTerminee.setStatus(Status.TERMINEE);
        partieTerminee.setJoueurs(new ArrayList<>()); 

        UserDetails userDetails = User.withUsername("lucie").password("pw").roles("USER").build();
        when(utilisateurService.findUtilisateurByUsername("lucie")).thenReturn(user);

        Map<Status, List<Partie>> partiesMap = new HashMap<>();
        partiesMap.put(Status.EN_ATTENTE, new ArrayList<>());
        partiesMap.put(Status.EN_COURS, new ArrayList<>());
        partiesMap.put(Status.TERMINEE, List.of(partieTerminee));

        when(partieService.getPartiesGroupByEtat(userId)).thenReturn(partiesMap);

        Model model = mock(Model.class);

        String view = jeuControleur.mesPartiesParEtat(model, userDetails);

        assertEquals("parties", view);
        verify(model).addAttribute(eq("rangMap"), argThat((Map<UUID, String> map) ->
                map.containsKey(partieTerminee.getId()) && map.get(partieTerminee.getId()) == null
        ));
    }

    @Test
    void mesPartiesParEtat_aucunJoueurCorrespondant_rangMapNull() {
        UUID userId = UUID.randomUUID();
        Utilisateur user = new Utilisateur();
        user.setId(userId);

        Partie partieTerminee = new Partie("Terminee");
        partieTerminee.setId(UUID.randomUUID());
        partieTerminee.setStatus(Status.TERMINEE);

        PartieJoueur pj1 = mock(PartieJoueur.class);
        Utilisateur autre1 = new Utilisateur();
        autre1.setId(UUID.randomUUID());
        when(pj1.getUtilisateur()).thenReturn(autre1);

        PartieJoueur pj2 = mock(PartieJoueur.class);
        Utilisateur autre2 = new Utilisateur();
        autre2.setId(UUID.randomUUID());
        when(pj2.getUtilisateur()).thenReturn(autre2);

        partieTerminee.setJoueurs(new ArrayList<>(List.of(pj1, pj2)));

        UserDetails userDetails = User.withUsername("lucie").password("pw").roles("USER").build();
        when(utilisateurService.findUtilisateurByUsername("lucie")).thenReturn(user);

        Map<Status, List<Partie>> partiesMap = new HashMap<>();
        partiesMap.put(Status.EN_ATTENTE, new ArrayList<>());
        partiesMap.put(Status.EN_COURS, new ArrayList<>());
        partiesMap.put(Status.TERMINEE, List.of(partieTerminee));

        when(partieService.getPartiesGroupByEtat(userId)).thenReturn(partiesMap);

        Model model = mock(Model.class);

        String view = jeuControleur.mesPartiesParEtat(model, userDetails);

        assertEquals("parties", view);
        verify(model).addAttribute(eq("rangMap"), argThat((Map<UUID, String> map) ->
                map.containsKey(partieTerminee.getId()) && map.get(partieTerminee.getId()) == null
        ));
    }

    @Test
    void mesPartiesParEtat_joueurCorrespondantPasPremier_rangMapRempli() {
        UUID userId = UUID.randomUUID();
        Utilisateur user = new Utilisateur();
        user.setId(userId);

        Partie partieTerminee = new Partie("Terminee");
        partieTerminee.setId(UUID.randomUUID());
        partieTerminee.setStatus(Status.TERMINEE);

        PartieJoueur pj1 = mock(PartieJoueur.class);
        Utilisateur autre1 = new Utilisateur();
        autre1.setId(UUID.randomUUID());
        when(pj1.getUtilisateur()).thenReturn(autre1);

        PartieJoueur pj2 = mock(PartieJoueur.class);
        Utilisateur userOk = new Utilisateur();
        userOk.setId(userId);
        when(pj2.getUtilisateur()).thenReturn(userOk);
        when(pj2.getRang()).thenReturn("1er");

        partieTerminee.setJoueurs(new ArrayList<>(List.of(pj1, pj2)));

        UserDetails userDetails = User.withUsername("lucie").password("pw").roles("USER").build();
        when(utilisateurService.findUtilisateurByUsername("lucie")).thenReturn(user);

        Map<Status, List<Partie>> partiesMap = new HashMap<>();
        partiesMap.put(Status.EN_ATTENTE, new ArrayList<>());
        partiesMap.put(Status.EN_COURS, new ArrayList<>());
        partiesMap.put(Status.TERMINEE, List.of(partieTerminee));

        when(partieService.getPartiesGroupByEtat(userId)).thenReturn(partiesMap);

        Model model = mock(Model.class);

        String view = jeuControleur.mesPartiesParEtat(model, userDetails);

        assertEquals("parties", view);
        verify(model).addAttribute(eq("rangMap"), argThat((Map<UUID, String> map) ->
                "1er".equals(map.get(partieTerminee.getId()))
        ));
    }

    @Test
    void testAfficherHomePage() {
        JoueurStatistique stats = new JoueurStatistique();
        when(statistiquesService.getStatisticsForUser(userId)).thenReturn(stats);

        String result = jeuControleur.afficherHomePage(userDetails, model);

        assertEquals("statistiques", result);
        verify(model).addAttribute(eq("stats"), eq(stats));
    }

    @Test
    void testSendGridUpdate() {
        int[][] grid = new int[20][20];
        jeuControleur.sendGridUpdate(partieId, grid, partie);

        verify(messagingTemplate).convertAndSend(eq("/topic/grid-updates/" + partieId), anyMap());
    }

    @Test
    void testSendFinUpdate() {
        jeuControleur.sendFinUpdate(partie);

        verify(messagingTemplate).convertAndSend(eq("/topic/fin-partie/" + partieId), anyMap());
    }

    @Test
    void testSendStatusUpdate() {
        jeuControleur.sendStatusUpdate(partieId, partie);

        verify(messagingTemplate).convertAndSend(eq("/topic/status-updates/" + partieId), anyMap());
    }

    @Test
    void testSendJoueurUpdate() {
        jeuControleur.sendJoueurUpdate(partieId, partie);

        verify(messagingTemplate).convertAndSend(eq("/topic/attente-updates/" + partieId), anyMap());
    }

    @Test
    void testRejoindrePartieUpdate() {
        jeuControleur.rejoindrePartieUpdate(partieId, partie);

        verify(messagingTemplate).convertAndSend(eq("/topic/status-updates/" + partieId), anyMap());
    }
}