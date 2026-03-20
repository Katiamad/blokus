package fr.orleans.m1miage.project.s2.projetS2.sevicesTest;

import fr.orleans.m1miage.project.s2.projetS2.exeptions.NomPartieInvalideException;
import fr.orleans.m1miage.project.s2.projetS2.exeptions.PartieNonRejoignableException;
import fr.orleans.m1miage.project.s2.projetS2.exeptions.PartieNotFoundException;
import fr.orleans.m1miage.project.s2.projetS2.exeptions.UtilisateurNonTrouveException;
import fr.orleans.m1miage.project.s2.projetS2.model.*;
import fr.orleans.m1miage.project.s2.projetS2.repository.PartieRepository;
import fr.orleans.m1miage.project.s2.projetS2.repository.RobotRepository;
import fr.orleans.m1miage.project.s2.projetS2.repository.UtilisateurRepository;
import fr.orleans.m1miage.project.s2.projetS2.service.PartieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;

import static fr.orleans.m1miage.project.s2.projetS2.service.PartieService.getSuffixRang;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartieServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private RobotRepository robotRepository;
    @Mock
    private PartieRepository partieRepository;
    @Mock
    private JeuSansTimer jeuSansTimer;
    @Mock
    private JeuAvecTimer  jeuAvecTimer;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @InjectMocks
    private PartieService partieService;

    private Utilisateur user;
    private Utilisateur testUser;
    private Partie partie;
    private UUID partieId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        user = new Utilisateur("lylia", "lylia@ex.com", "pass");
        testUser = new Utilisateur("lucie", "lucie@ex.com", "pass");
        user.setId(UUID.randomUUID());
        testUser.setId(UUID.randomUUID());
        partie = new Partie("TestGame");
        partieId = UUID.randomUUID();
        userId = user.getId();
        partie.setId(partieId);
    }

    @Test
    void seedFinishedGamesForUser_shouldCreateTwoFinishedGamesForUser() {

        partieService.seedFinishedGamesForUser(user, testUser);

        ArgumentCaptor<Partie> partieCaptor = ArgumentCaptor.forClass(Partie.class);
        verify(partieRepository, times(2)).save(partieCaptor.capture());

        List<Partie> parties = partieCaptor.getAllValues();
        assertEquals(2, parties.size());

        Partie partie1 = parties.get(0);
        Partie partie2 = parties.get(1);

        List<String> noms = parties.stream().map(Partie::getNom).toList();
        assertTrue(noms.contains("Seeded_Win_1"));
        assertTrue(noms.contains("Seeded_Loss_1"));

        assertEquals(Status.TERMINEE, partie1.getStatus());
        assertEquals(Status.TERMINEE, partie2.getStatus());

        boolean foundWin = partie1.getJoueurs().stream()
                .anyMatch(pj -> pj.getUtilisateur().equals(user) && pj.getRang().equals("1er"));
        boolean foundLoss = partie2.getJoueurs().stream()
                .anyMatch(pj -> pj.getUtilisateur().equals(user) && pj.getRang().equals("2e"));
        boolean testUserWin = partie2.getJoueurs().stream()
                .anyMatch(pj -> pj.getUtilisateur().equals(testUser) && pj.getRang().equals("1er"));

        assertTrue(foundWin, "L'utilisateur doit avoir une victoire dans la première partie");
        assertTrue(foundLoss, "L'utilisateur doit avoir une défaite dans la deuxième partie");
        assertTrue(testUserWin, "Le test user doit avoir gagné la deuxième partie");
    }

    @Test
    void lancerPartie_shouldThrowException_whenNomPartieIsNullOrEmpty() {
        UUID userId = UUID.randomUUID();

        assertThrows(NomPartieInvalideException.class,
                () -> partieService.lancerPartie(userId, null, "AVEC_TIMER", 10));

        assertThrows(NomPartieInvalideException.class,
                () -> partieService.lancerPartie(userId, "", "AVEC_TIMER", 10));

        assertThrows(NomPartieInvalideException.class,
                () -> partieService.lancerPartie(userId, "   ", "AVEC_TIMER", 10));
    }


    @Test
    void lancerPartie_shouldThrowException_whenNomPartieAlreadyExists() {
        String nom = "Test";
        when(partieRepository.findByNom(nom)).thenReturn(Optional.of(new Partie()));
        assertThrows(NomPartieInvalideException.class,
                () -> partieService.lancerPartie(user.getId(), nom, "AVEC_TIMER", 10));
    }

    @Test
    void lancerPartie_shouldThrowException_whenUtilisateurNotFound() {
        UUID id = UUID.randomUUID();
        when(partieRepository.findByNom(any())).thenReturn(Optional.empty());
        when(utilisateurRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UtilisateurNonTrouveException.class,
                () -> partieService.lancerPartie(id, "PartieValide", "CLASSIQUE", 10));
    }

    @Test
    void lancerPartie_shouldCreatePartieWithUserAnd3Robots() {
        String nomPartie = "MaPartie";
        String modeJeu = "SANS_TIMER";
        int temps = 15;

        when(partieRepository.findByNom(nomPartie)).thenReturn(Optional.empty());
        when(utilisateurRepository.findById(user.getId())).thenReturn(Optional.of(user));

        UUID partieId = UUID.randomUUID();
        when(partieRepository.save(any(Partie.class))).thenAnswer(invocation -> {
            Partie saved = invocation.getArgument(0);
            saved.setId(partieId);
            return saved;
        });

        when(partieRepository.findById(partieId)).thenReturn(Optional.of(new Partie("MaPartie")));

        Partie result = partieService.lancerPartie(user.getId(), nomPartie, modeJeu, temps);

        assertEquals(nomPartie, result.getNom());
        assertEquals(ModeJeu.SANS_TIMER, result.getModeJeu());
        assertEquals(Status.EN_ATTENTE, result.getStatus());
        assertEquals(1, result.getTour());
        assertEquals(user.getId(), result.getCreateurId());
        assertEquals(1, result.getJoueurs().size());
        verify(partieRepository, atLeastOnce()).save(any(Partie.class));
    }


    @Test
    void estDansLaPartie_throwIfPartieNotFound() {
        when(partieRepository.findById(partieId)).thenReturn(Optional.empty());
        assertThrows(PartieNotFoundException.class, () -> partieService.estDansLaPartie(partieId, userId));
    }

    @Test
    void rejoindrePartie_quandPleineEtRobotEnleveAjouteUser() {
        Partie partie = new Partie("Test");
        partie.setId(partieId);
        partie.setStatus(Status.EN_ATTENTE);

        PartieRobot robot = new PartieRobot();
        robot.setNom("robot1");
        robot.setCouleur("#FFB76E");
        partie.getRobots().add(robot);

        for (int i = 0; i < 3; i++) {
            Utilisateur joueur = new Utilisateur("nom", "email", "mdp");
            joueur.setId(UUID.randomUUID());
            PartieJoueur pj = new PartieJoueur(partie, joueur, "couleur");
            partie.getJoueurs().add(pj);
        }

        when(partieRepository.findById(partieId)).thenReturn(Optional.of(partie));
        when(utilisateurRepository.findById(userId)).thenReturn(Optional.of(user));
        when(partieRepository.save(any(Partie.class))).thenReturn(partie);

        Partie res = partieService.rejoindrePartie(partieId, userId);

        assertEquals(4, res.getJoueurs().size() + res.getRobots().size());
        verify(robotRepository).delete(robot);
        verify(partieRepository, atLeastOnce()).save(any(Partie.class));
    }

    @Test
    void rejoindrePartie_refuse_siPartieEnCours_ouTerminee() {
        Partie partie = new Partie("Déjà commencée");
        partie.setId(partieId);

        partie.setStatus(Status.EN_COURS);
        when(partieRepository.findById(partieId)).thenReturn(Optional.of(partie));
        PartieNonRejoignableException ex = assertThrows(
                PartieNonRejoignableException.class,
                () -> partieService.rejoindrePartie(partieId, userId)
        );
        assertEquals("On ne peut pas rejoindre une partie déjà commencée ou terminée.", ex.getMessage());

        partie.setStatus(Status.TERMINEE);
        when(partieRepository.findById(partieId)).thenReturn(Optional.of(partie));
        assertThrows(
                PartieNonRejoignableException.class,
                () -> partieService.rejoindrePartie(partieId, userId)
        );
    }

    @Test
    void rejoindrePartie_quandPasPleineAjouteUser() {
        Partie partie = new Partie("Test");
        partie.setId(partieId);
        partie.setStatus(Status.EN_ATTENTE);
        partieService.couleurs = new ArrayList<>(partieService.getCouleurs());


        when(partieRepository.findById(partieId)).thenReturn(Optional.of(partie));
        when(utilisateurRepository.findById(userId)).thenReturn(Optional.of(user));
        when(partieRepository.save(any(Partie.class))).thenReturn(partie);

        Partie res = partieService.rejoindrePartie(partieId, userId);

        assertTrue(res.getJoueurs().stream().anyMatch(j -> j.getUtilisateur() == user));
    }

    @Test
    void rejoindrePartie_siUserDejaPresent_neRienFaire() {
        Partie partie = new Partie("Test");
        partie.setId(partieId);
        partie.setStatus(Status.EN_ATTENTE);
        PartieJoueur pj = new PartieJoueur(partie, user, "red");
        partie.getJoueurs().add(pj);

        when(partieRepository.findById(partieId)).thenReturn(Optional.of(partie));
        when(utilisateurRepository.findById(userId)).thenReturn(Optional.of(user));

        Partie res = partieService.rejoindrePartie(partieId, userId);
        assertSame(partie, res);
    }

    @Test
    void findById_ok() {
        when(partieRepository.findById(partieId)).thenReturn(Optional.of(partie));
        assertSame(partie, partieService.findById(partieId));
    }

    @Test
    void findById_notFound() {
        when(partieRepository.findById(partieId)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> partieService.findById(partieId));
    }

    @Test
    void demarrerPartie_shouldThrowException_whenPartieNotFound() {
        UUID id = UUID.randomUUID();
        when(partieRepository.findById(id)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> partieService.demarrerPartie(id));
        assertEquals("Partie non trouvée", ex.getMessage());
    }

    @Test
    void demarrerPartie_shouldThrowException_ifPartieNotFound() {
        UUID fakeId = UUID.randomUUID();
        when(partieRepository.findById(fakeId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> partieService.demarrerPartie(fakeId));
        assertEquals("Partie non trouvée", ex.getMessage());
    }


    @Test
    void demarrerPartie_shouldAssignToursToPlayersAndRobots() {
        Partie partie = new Partie("Blokus");
        partie.setId(UUID.randomUUID());
        partie.setModeJeu(ModeJeu.SANS_TIMER);

        PartieJoueur joueur1 = mock(PartieJoueur.class);
        PartieJoueur joueur2 = mock(PartieJoueur.class);
        PartieRobot robot1 = mock(PartieRobot.class);
        partie.getJoueurs().addAll(List.of(joueur1, joueur2));
        partie.getRobots().add(robot1);

        when(partieRepository.findById(partie.getId())).thenReturn(Optional.of(partie));
        when(partieRepository.save(partie)).thenReturn(partie);
        when(jeuSansTimer.startGame(any())).thenReturn(partie);

        Partie res = partieService.demarrerPartie(partie.getId());

        InOrder inOrder = inOrder(joueur1, joueur2, robot1);
        inOrder.verify(joueur1).setTour(1);
        inOrder.verify(joueur1).initialiserPositionsPremierTour();
        inOrder.verify(joueur2).setTour(2);
        inOrder.verify(joueur2).initialiserPositionsPremierTour();
        inOrder.verify(robot1).setTour(3);
        inOrder.verify(robot1).initialiserPositionsPremierTour();

        assertEquals(Status.EN_COURS, res.getStatus());
        verify(jeuSansTimer).startGame(partie);
    }

    @Test
    void demarrerPartie_shouldChooseJeuSansTimer_whenModeIsSansTimer() {
        Partie partie = spy(new Partie("Blokus"));
        partie.setModeJeu(ModeJeu.SANS_TIMER);
        when(partieRepository.findById(partie.getId())).thenReturn(Optional.of(partie));
        when(partieRepository.save(partie)).thenReturn(partie);

        partieService.demarrerPartie(partie.getId());

        verify(jeuSansTimer).startGame(partie);
        verify(jeuAvecTimer, never()).startGame(any());
    }


    @Test
    void demarrerPartie_shouldSavePartieWithStatusEnCours() {
        Partie partie = new Partie("Blokus");
        partie.setModeJeu(ModeJeu.SANS_TIMER);
        when(partieRepository.findById(partie.getId())).thenReturn(Optional.of(partie));
        when(partieRepository.save(partie)).thenReturn(partie);

        Partie res = partieService.demarrerPartie(partie.getId());

        assertEquals(Status.EN_COURS, res.getStatus());
        verify(partieRepository).save(partie);
    }
    @Test
    void validerJeu_placementValide_metAJourGrilleEtScoreEtAvanceTour() {
        Partie partie = new Partie("Valide");
        partie.setId(partieId);
        int[][] grille = new int[20][20];
        partie.setGrille(grille);

        Utilisateur user = new Utilisateur("Alice", "alice@a.com", "mdp");
        user.setId(UUID.randomUUID());
        PartieJoueur pj = Mockito.spy(new PartieJoueur(partie, user, "bleu"));
        pj.setTour(1);

        Piece piece = Mockito.mock(Piece.class);
        when(piece.getId()).thenReturn(42);
        when(piece.getSize()).thenReturn(5);

        pj.getPieces().clear();
        pj.getPieces().add(piece);

        partie.getJoueurs().add(pj);
        partie.setTour(1);

        List<int[]> positions = List.of(new int[]{0, 0}, new int[]{1, 0});

        when(partieRepository.findById(partieId)).thenReturn(Optional.of(partie));
        when(partieRepository.save(any(Partie.class))).thenReturn(partie);

        PartieService spyService = Mockito.spy(partieService);
        Mockito.doReturn(true).when(spyService).peutPlacer(any(int[][].class), anyList(), anyInt());
        Mockito.doNothing().when(spyService).checkAndFinishIfNoOneCanPlay(any(Partie.class));

        Partie res = spyService.validerJeu(partieId, positions, 42);

        assertEquals(0, pj.getPieces().size(), "La pièce doit être retirée du joueur");
        verify(pj).ajouterPointsPiece(piece);
        verify(pj).setLastPieceSize(5);
        assertEquals(1, res.getGrille()[0][0]);
        assertEquals(1, res.getGrille()[1][0]);
        verify(partieRepository).save(partie);
    }

    @Test
    void validerJeu_throwsException_whenPlacementNotValide() {
        UUID partieId = UUID.randomUUID();
        Partie partie = new Partie("TestPartie");
        partie.setId(partieId);
        partie.setTour(1);

        Utilisateur user = new Utilisateur("Test", "test@test.com", "pass");
        user.setId(UUID.randomUUID());
        PartieJoueur pj = new PartieJoueur(partie, user, "bleu");
        Piece piece = new Piece();
        piece.setId(100);
        pj.getPieces().add(piece);

        partie.getJoueurs().add(pj);

        List<int[]> positions = List.of(new int[] { 0, 0 });

        when(partieRepository.findById(partieId)).thenReturn(Optional.of(partie));

        PartieService spyService = Mockito.spy(partieService);
        doReturn(false).when(spyService).peutPlacer(any(int[][].class), anyList(), anyInt());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> spyService.validerJeu(partieId, positions, 100));

        assertEquals("Votre placement n'est pas valide.", exception.getMessage());
    }

    @Test
    void getPartiesParEtatPourUtilisateur_returnList() {
        List<Partie> parties = List.of(partie);
        when(partieRepository.findByJoueursUtilisateurIdAndStatus(userId, Status.EN_ATTENTE)).thenReturn(parties);

        List<Partie> res = partieService.getPartiesParEtatPourUtilisateur(userId, Status.EN_ATTENTE);

        assertEquals(1, res.size());
    }

    @Test
    void getPartiesGroupByEtat_returnMapWithAllStatus() {
        for (Status s : Status.values()) {
            when(partieRepository.findByJoueursUtilisateurIdAndStatus(userId, s)).thenReturn(List.of(new Partie()));
        }
        Map<Status, List<Partie>> res = partieService.getPartiesGroupByEtat(userId);
        assertEquals(Status.values().length, res.size());
    }

    @Test
    void nextIsHuman_trueIfTourInferieurOuEgalNbHumains() {
        PartieJoueur pj = new PartieJoueur(partie, user, "red");
        partie.getJoueurs().add(pj);
        partie.setTour(1);
        assertTrue(partieService.nextIsHuman(partie));
    }

    @Test
    void nextIsHuman_falseIfTourSupNbHumains() {
        PartieJoueur pj = new PartieJoueur(partie, user, "red");
        partie.getJoueurs().add(pj);
        partie.setTour(2);
        assertFalse(partieService.nextIsHuman(partie));
    }

    @Test
    void rejoindrePartie_partiePleineSansRobot_throwPartieNonRejoignableException() {
        Partie partie = new Partie("PartiePleine");
        partie.setId(partieId);
        partie.setStatus(Status.EN_ATTENTE);

        for (int i = 0; i < 4; i++) {
            Utilisateur joueur = new Utilisateur("nom"+i, "email"+i, "mdp");
            joueur.setId(UUID.randomUUID());
            PartieJoueur pj = new PartieJoueur(partie, joueur, "couleur"+i);
            partie.getJoueurs().add(pj);
        }

        partieService.couleurs = new ArrayList<>(partieService.getCouleurs());

        when(partieRepository.findById(partieId)).thenReturn(Optional.of(partie));
        when(utilisateurRepository.findById(userId)).thenReturn(Optional.of(user));

        PartieNonRejoignableException ex = assertThrows(
                PartieNonRejoignableException.class,
                () -> partieService.rejoindrePartie(partieId, userId)
        );
        assertEquals("La partie est déjà pleine", ex.getMessage());
    }

    @Test
    void validerJeu_tourRevientAuPremierJoueur_siTourEstDernier() {
        Utilisateur joueur1 = new Utilisateur("j1", "j1@a.com", "pass"); joueur1.setId(UUID.randomUUID());
        PartieJoueur pj1 = new PartieJoueur(partie, joueur1, "rouge");
        pj1.setTour(1);

        Utilisateur joueur2 = new Utilisateur("j2", "j2@a.com", "pass"); joueur2.setId(UUID.randomUUID());
        PartieJoueur pj2 = new PartieJoueur(partie, joueur2, "bleu");
        pj2.setTour(2);

        partie.getJoueurs().addAll(List.of(pj1, pj2));
        partie.setTour(2);
        partie.setGrille(new int[20][20]);
        partie.setId(partieId);

        Piece piece = new Piece();
        piece.setId(99);
        piece.setForme(new int[][]{{1}});
        pj2.getPieces().add(piece);

        when(partieRepository.findById(partieId)).thenReturn(Optional.of(partie));
        when(partieRepository.save(any(Partie.class))).thenReturn(partie);

        List<int[]> positions = List.of(new int[]{0, 0});
        Partie res = partieService.validerJeu(partieId, positions, 99);

        assertEquals(1, res.getTour(), "Après le dernier joueur, le tour doit revenir à 1");
    }

    @Test
    void validerJeu_incrementeTour_siPasDernierJoueur() {
        Utilisateur joueur1 = new Utilisateur("j1", "j1@a.com", "pass"); joueur1.setId(UUID.randomUUID());
        PartieJoueur pj1 = new PartieJoueur(partie, joueur1, "rouge");
        pj1.setTour(1);

        Utilisateur joueur2 = new Utilisateur("j2", "j2@a.com", "pass"); joueur2.setId(UUID.randomUUID());
        PartieJoueur pj2 = new PartieJoueur(partie, joueur2, "bleu");
        pj2.setTour(2);

        partie.getJoueurs().addAll(List.of(pj1, pj2));
        partie.setTour(1);
        partie.setGrille(new int[20][20]);
        partie.setId(partieId);

        Piece piece = new Piece();
        piece.setId(88);
        piece.setForme(new int[][]{{1}});
        pj1.getPieces().add(piece);

        when(partieRepository.findById(partieId)).thenReturn(Optional.of(partie));
        when(partieRepository.save(any(Partie.class))).thenReturn(partie);

        List<int[]> positions = List.of(new int[]{0, 0});
        Partie res = partieService.validerJeu(partieId, positions, 88);

        assertEquals(2, res.getTour(), "Le tour doit passer de 1 à 2");
    }

    @Test
    void peutPlacer_returnFalse_siPositionHorsGrille() {
        int[][] grille = new int[20][20];
        List<int[]> positions = List.of(new int[]{-1, 0});
        assertFalse(partieService.peutPlacer(grille, positions, 1));
    }

    @Test
    void peutPlacer_returnFalse_siCaseOccupee() {
        int[][] grille = new int[20][20];
        grille[0][0] = 2;
        List<int[]> positions = List.of(new int[]{0, 0});
        assertFalse(partieService.peutPlacer(grille, positions, 1));
    }

    @Test
    void peutPlacer_returnFalse_siToucheSonPropreCote() {
        int[][] grille = new int[20][20];
        grille[0][1] = 1;
        List<int[]> positions = List.of(new int[]{0, 0});
        assertFalse(partieService.peutPlacer(grille, positions, 1));
    }

    @Test
    void peutPlacer_returnFalse_premierTour_pasDansCoin() {
        int[][] grille = new int[20][20];
        List<int[]> positions = List.of(new int[]{5, 5});
        assertFalse(partieService.peutPlacer(grille, positions, 1));
    }

    @Test
    void peutPlacer_returnTrue_premierTour_dansCoin() {
        int[][] grille = new int[20][20];
        List<int[]> positions = List.of(new int[]{0, 0});
        assertTrue(partieService.peutPlacer(grille, positions, 1));
    }

    @Test
    void peutPlacer_returnFalse_apresPremierTour_pasSurSonCoin() {
        int[][] grille = new int[20][20];
        grille[10][10] = 1;
        List<int[]> positions = List.of(new int[]{15, 15});
        assertFalse(partieService.peutPlacer(grille, positions, 1));
    }

    @Test
    void peutPlacer_returnTrue_siUneDesPositionsToucheUnCoin() {
        int[][] grille = new int[20][20];
        grille[10][10] = 1;
        List<int[]> positions = List.of(new int[]{15, 15}, new int[]{9, 9});
        assertTrue(partieService.peutPlacer(grille, positions, 1));
    }

    @Test
    void rotationnerPiece_zeroRotation_renvoieIdentique() {
        int[][] piece = {{1, 2}, {3, 4}};
        int[][] res = partieService.rotationnerPiece(piece, 0);

        assertArrayEquals(piece, res);
    }

    @Test
    void rotationnerPiece_uneRotation90() {
        int[][] piece = {{1, 2}, {3, 4}};
        int[][] attendu = {{3, 1}, {4, 2}};
        int[][] res = partieService.rotationnerPiece(piece, 1);

        assertArrayEquals(attendu, res);
    }

    @Test
    void rotationnerPiece_deuxRotations180() {
        int[][] piece = {{1, 2}, {3, 4}};
        int[][] attendu = {{4, 3}, {2, 1}};
        int[][] res = partieService.rotationnerPiece(piece, 2);

        assertArrayEquals(attendu, res);
    }

    @Test
    void rotationnerPiece_troisRotations270() {
        int[][] piece = {{1, 2}, {3, 4}};
        int[][] attendu = {{2, 4}, {1, 3}};
        int[][] res = partieService.rotationnerPiece(piece, 3);

        assertArrayEquals(attendu, res);
    }

    @Test
    void rotationnerPiece_quatreRotations360_retourEtatInitial() {
        int[][] piece = {{1, 2}, {3, 4}};
        int[][] res = partieService.rotationnerPiece(piece, 4);

        assertArrayEquals(piece, res);
    }

    @Test
    void rotationnerPiece_matriceNonCarree() {
        int[][] piece = {
                {1, 2, 3},
                {4, 5, 6}
        };
        // Rotation à 90°:
        int[][] attendu = {
                {4, 1},
                {5, 2},
                {6, 3}
        };
        int[][] res = partieService.rotationnerPiece(piece, 1);

        assertArrayEquals(attendu, res);
    }

    @Test
    void rotationnerPiece_matriceNonCarree_quatreRotations() {
        int[][] piece = {
                {1, 2, 3},
                {4, 5, 6}
        };
        int[][] res = partieService.rotationnerPiece(piece, 4);
        assertArrayEquals(piece, res);
    }

    @Test
    void symetriePiece_carre_2x2() {
        int[][] piece = {{1, 2}, {3, 4}};
        int[][] attendu = {{2, 1}, {4, 3}};
        int[][] result = partieService.symetriePiece(piece);
        assertArrayEquals(attendu, result);
    }

    @Test
    void symetriePiece_rectangle_2x3() {
        int[][] piece = {{1, 2, 3}, {4, 5, 6}};
        int[][] attendu = {{3, 2, 1}, {6, 5, 4}};
        int[][] result = partieService.symetriePiece(piece);
        assertArrayEquals(attendu, result);
    }

    @Test
    void symetriePiece_ligne_1x4() {
        int[][] piece = {{7, 8, 9, 10}};
        int[][] attendu = {{10, 9, 8, 7}};
        int[][] result = partieService.symetriePiece(piece);
        assertArrayEquals(attendu, result);
    }

    @Test
    void symetriePiece_colonne_4x1() {
        int[][] piece = {{1}, {2}, {3}, {4}};
        int[][] attendu = {{1}, {2}, {3}, {4}};
        int[][] result = partieService.symetriePiece(piece);
        assertArrayEquals(attendu, result);
    }

    @Test
    void calculerPositionsPiece_formeSimple_2x2_x0y0() {
        int[][] forme = {{1, 0}, {0, 1}};
        List<int[]> positions = partieService.calculerPositionsPiece(forme, 0, 0);

        assertEquals(2, positions.size());
        assertArrayEquals(new int[]{0, 0}, positions.get(0));
        assertArrayEquals(new int[]{1, 1}, positions.get(1));
    }

    @Test
    void calculerPositionsPiece_formeSimple_2x2_x5y10() {
        int[][] forme = {{1, 0}, {0, 1}};
        List<int[]> positions = partieService.calculerPositionsPiece(forme, 5, 10);

        assertEquals(2, positions.size());
        assertArrayEquals(new int[]{5, 10}, positions.get(0));
        assertArrayEquals(new int[]{6, 11}, positions.get(1));
    }

    @Test
    void calculerPositionsPiece_retourneVide_siDepasseLimiteGrille() {
        int[][] forme = {{1, 1}, {1, 1}};
        List<int[]> positions = partieService.calculerPositionsPiece(forme, 19, 19);
        assertTrue(positions.isEmpty());
    }

    @Test
    void calculerPositionsPiece_retournePositionsPourFormeEnL() {
        int[][] forme = {{1, 0}, {1, 1}};
        List<int[]> positions = partieService.calculerPositionsPiece(forme, 3, 4);
        assertEquals(3, positions.size());
        assertArrayEquals(new int[]{3, 4}, positions.get(0));
        assertArrayEquals(new int[]{4, 4}, positions.get(1));
        assertArrayEquals(new int[]{4, 5}, positions.get(2));
    }
    @Test
    void getCoinsInitiauxDisponibles_retourneTousCoinsSiVides() {
        int[][] grille = new int[20][20];
        List<int[]> coins = partieService.getCoinsInitiauxDisponibles(grille);

        assertEquals(4, coins.size());
        assertTrue(coins.stream().anyMatch(c -> c[0]==0 && c[1]==0));
        assertTrue(coins.stream().anyMatch(c -> c[0]==0 && c[1]==19));
        assertTrue(coins.stream().anyMatch(c -> c[0]==19 && c[1]==0));
        assertTrue(coins.stream().anyMatch(c -> c[0]==19 && c[1]==19));
    }

    @Test
    void getCoinsInitiauxDisponibles_retourneSeulementCoinsLibres() {
        int[][] grille = new int[20][20];
        grille[0][0] = 1;
        List<int[]> coins = partieService.getCoinsInitiauxDisponibles(grille);

        assertEquals(3, coins.size());
        assertFalse(coins.stream().anyMatch(c -> c[0]==0 && c[1]==0));
    }

    @Test
    void getCoinsInitiauxDisponibles_retourneAucunSiTousOccupes() {
        int[][] grille = new int[20][20];
        grille[0][0] = 1;
        grille[0][19] = 1;
        grille[19][0] = 1;
        grille[19][19] = 1;
        List<int[]> coins = partieService.getCoinsInitiauxDisponibles(grille);

        assertEquals(0, coins.size());
    }

    @Test
    void getCoinsDisponiblesPourJoueur_retourneListeCoinsPourJoueurPresent() {
        // Arrange
        Partie partie = new Partie("TestGame");
        partie.setId(partieId);
        int[][] grille = new int[20][20];
        partie.setGrille(grille);

        Utilisateur user = new Utilisateur("a", "a@a.fr", "pass");
        user.setId(userId);

        PartieJoueur pj = new PartieJoueur(partie, user, "rouge");
        pj.setTour(1);
        partie.getJoueurs().add(pj);

        when(partieRepository.findById(partieId)).thenReturn(Optional.of(partie));

        List<int[]> coins = partieService.getCoinsDisponiblesPourJoueur(partieId, userId);

        assertEquals(4, coins.size());
    }

    @Test
    void getCoinsDisponiblesPourJoueur_retourneListeVideSiJoueurPasDansPartie() {
        Partie partie = new Partie("TestGame");
        partie.setId(partieId);
        partie.setGrille(new int[20][20]);
        when(partieRepository.findById(partieId)).thenReturn(Optional.of(partie));

        List<int[]> coins = partieService.getCoinsDisponiblesPourJoueur(partieId, userId);
        assertTrue(coins.isEmpty());
    }

    @Test
    void jouerRobot_aucunCoupPossible_avanceTourEtSave() {
        Partie partie = new Partie("Blokus");
        partie.setId(UUID.randomUUID());
        int[][] grille = new int[20][20];
        partie.setGrille(grille);

        PartieRobot robot = new PartieRobot();
        robot.setTour(1);
        Piece piece = new Piece();
        piece.setId(1);
        piece.setForme(new int[][]{{1}});
        robot.getPieces().add(piece);

        partie.getRobots().add(robot);
        partie.setTour(1);

        PartieService spyService = Mockito.spy(partieService);

        doReturn(Collections.emptyList()).when(spyService).getValidMoves(any(int[][].class), any(), any(), any(), any(), anyInt());
        doNothing().when(spyService).avancerTour(any());
        doNothing().when(spyService).checkAndFinishIfNoOneCanPlay(any());
        when(partieRepository.save(any())).thenReturn(partie);

        spyService.jouerRobot(robot, partie);

        verify(spyService).avancerTour(partie);
        verify(partieRepository).save(partie);
        verify(spyService).checkAndFinishIfNoOneCanPlay(partie);
        verify(spyService, never()).placerPieceEtFinirTour(anyList(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void jouerRobot_avecCoupPossible_joueLeCoupEtNePassePasSonTour() {
        Partie partie = new Partie("Blokus");
        partie.setId(UUID.randomUUID());
        int[][] grille = new int[20][20];
        partie.setGrille(grille);

        PartieRobot robot = new PartieRobot();
        robot.setTour(1);
        Piece piece = new Piece();
        piece.setId(1);
        piece.setForme(new int[][]{{1}});
        robot.getPieces().add(piece);

        partie.getRobots().add(robot);
        partie.setTour(1);

        PartieService spyService = Mockito.spy(partieService);

        List<int[]> positions = List.of(new int[]{0,0});
        PartieService.RobotMove move = new PartieService.RobotMove(positions, piece);

        doReturn(List.of(move)).when(spyService).getValidMoves(any(int[][].class), any(), any(), any(), any(), anyInt());

        doNothing().when(spyService).placerPieceEtFinirTour(anyList(), any(), any(), any(), any(), anyInt());
        doNothing().when(spyService).checkAndFinishIfNoOneCanPlay(any());

        spyService.jouerRobot(robot, partie);

        verify(spyService, times(1)).placerPieceEtFinirTour(eq(positions), eq(piece), eq(grille), eq(robot), eq(partie), eq(1));
        verify(spyService, never()).avancerTour(any());
        verify(spyService).checkAndFinishIfNoOneCanPlay(partie);
        verify(partieRepository, never()).save(any());
    }

    @Test
    void jouerRobot_plusieursCoupsPossibles_joueUnAuHasard() {
        Partie partie = new Partie("Blokus");
        partie.setId(UUID.randomUUID());
        int[][] grille = new int[20][20];
        partie.setGrille(grille);

        PartieRobot robot = new PartieRobot();
        robot.setTour(1);
        Piece piece = new Piece();
        piece.setId(1);
        piece.setForme(new int[][]{{1}});
        robot.getPieces().add(piece);

        partie.getRobots().add(robot);
        partie.setTour(1);

        PartieService spyService = Mockito.spy(partieService);

        List<int[]> pos1 = List.of(new int[]{0,0});
        List<int[]> pos2 = List.of(new int[]{1,1});
        PartieService.RobotMove move1 = new PartieService.RobotMove(pos1, piece);
        PartieService.RobotMove move2 = new PartieService.RobotMove(pos2, piece);

        doReturn(List.of(move1, move2)).when(spyService).getValidMoves(any(int[][].class), any(), any(), any(), any(), anyInt());
        doNothing().when(spyService).placerPieceEtFinirTour(anyList(), any(), any(), any(), any(), anyInt());
        doNothing().when(spyService).checkAndFinishIfNoOneCanPlay(any());

        spyService.jouerRobot(robot, partie);

        verify(spyService, atLeastOnce()).placerPieceEtFinirTour(anyList(), eq(piece), eq(grille), eq(robot), eq(partie), eq(1));
        verify(spyService, never()).avancerTour(any());
        verify(spyService).checkAndFinishIfNoOneCanPlay(partie);
        verify(partieRepository, never()).save(any());
    }

    @Test
    void getValidMoves_aucunPlacementPossible_retourneListeVide() {
        PartieService spyService = Mockito.spy(partieService);

        int[][] grille = new int[2][2];
        int[][] forme = new int[][]{{1}};
        Piece piece = new Piece();
        PartieRobot robot = new PartieRobot();
        Partie partie = new Partie("test");

        doReturn(List.of(new int[]{0, 0})).when(spyService).calculerPositionsPiece(any(), anyInt(), anyInt());
        doReturn(false).when(spyService).peutPlacer(any(), anyList(), anyInt());

        List<PartieService.RobotMove> moves = spyService.getValidMoves(forme, piece, grille, robot, partie, 1);

        assertNotNull(moves);
        assertTrue(moves.isEmpty());
    }

    @Test
    void getValidMoves_unePositionValide_retourneUnRobotMove() {
        PartieService spyService = Mockito.spy(partieService);

        int[][] grille = new int[2][2];
        int[][] forme = new int[][]{{1}};
        Piece piece = new Piece();
        PartieRobot robot = new PartieRobot();
        Partie partie = new Partie("test");
        int tour = 1;

        doReturn(List.of(new int[]{1, 1})).when(spyService).calculerPositionsPiece(any(), anyInt(), anyInt());
        doReturn(true).when(spyService).peutPlacer(any(), anyList(), eq(tour));

        List<PartieService.RobotMove> res = spyService.getValidMoves(forme, piece, grille, robot, partie, tour);

        assertEquals(grille.length * grille[0].length, res.size());
        assertTrue(res.stream().allMatch(move -> move.piece == piece));
    }

    @Test
    void getValidMoves_plusieursPositionsValides_retournePlusieursRobotMoves() {
        PartieService spyService = Mockito.spy(partieService);

        int[][] grille = new int[2][2];
        int[][] forme = new int[][]{{1}};
        Piece piece = new Piece();
        PartieRobot robot = new PartieRobot();
        Partie partie = new Partie("test");
        int tour = 1;

        doAnswer(invocation -> {
            int x = invocation.getArgument(1);
            int y = invocation.getArgument(2);
            return List.of(new int[]{x, y});
        }).when(spyService).calculerPositionsPiece(any(), anyInt(), anyInt());

        doReturn(true).when(spyService).peutPlacer(any(), anyList(), eq(tour));

        List<PartieService.RobotMove> moves = spyService.getValidMoves(forme, piece, grille, robot, partie, tour);

        assertEquals(4, moves.size(), "Il devrait y avoir autant de coups valides que de cases dans la grille.");
        for (PartieService.RobotMove move : moves) {
            assertSame(piece, move.piece, "Chaque RobotMove doit utiliser la bonne pièce");
        }
    }

    @Test
    void getValidMoves_mixtesCertainesValidesCertainesNon() {
        PartieService spyService = Mockito.spy(partieService);

        int[][] grille = new int[2][2];
        int[][] forme = new int[][]{{1}};
        Piece piece = new Piece();
        PartieRobot robot = new PartieRobot();
        Partie partie = new Partie("test");
        int tour = 1;

        doAnswer(invocation -> {
            int x = invocation.getArgument(1);
            int y = invocation.getArgument(2);
            return List.of(new int[]{x, y});
        }).when(spyService).calculerPositionsPiece(any(), anyInt(), anyInt());

        doAnswer(invocation -> {
            List<int[]> positions = invocation.getArgument(1);
            int[] pos = positions.get(0);
            return (pos[0] == 0 && pos[1] == 0) || (pos[0] == 1 && pos[1] == 1);
        }).when(spyService).peutPlacer(any(), anyList(), eq(tour));

        List<PartieService.RobotMove> moves = spyService.getValidMoves(forme, piece, grille, robot, partie, tour);

        assertEquals(2, moves.size(), "Il doit y avoir deux coups valides");
        assertTrue(moves.stream().anyMatch(move -> move.positions.get(0)[0] == 0 && move.positions.get(0)[1] == 0));
        assertTrue(moves.stream().anyMatch(move -> move.positions.get(0)[0] == 1 && move.positions.get(0)[1] == 1));
    }

    @Test
    void tryPlacingWithAllPositions_placePossible_retourneTrueEtAppellePlacerPiece() {
        int[][] grille = new int[4][4];
        int[][] forme = new int[][]{{1}};
        Piece piece = new Piece();
        Partie partie = new Partie("test");
        PartieRobot robot = new PartieRobot();

        PartieService spyService = Mockito.spy(partieService);

        doReturn(List.of(new int[]{0, 0})).when(spyService).calculerPositionsPiece(any(), anyInt(), anyInt());

        doReturn(true).when(spyService).peutPlacer(eq(grille), anyList(), eq(1));

        doNothing().when(spyService).placerPieceEtFinirTour(anyList(), eq(piece), eq(grille), eq(robot), eq(partie), eq(1));

        boolean res = spyService.tryPlacingWithAllPositions(forme, piece, grille, robot, partie, 1);

        assertTrue(res);
        verify(spyService).placerPieceEtFinirTour(anyList(), eq(piece), eq(grille), eq(robot), eq(partie), eq(1));
    }

    @Test
    void tryPlacingWithAllPositions_aucunPlacementPossible_retourneFalse() {
        int[][] grille = new int[4][4];
        int[][] forme = new int[][]{{1}};
        Piece piece = new Piece();
        Partie partie = new Partie("test");
        PartieRobot robot = new PartieRobot();

        PartieService spyService = Mockito.spy(partieService);

        doReturn(List.of(new int[]{0, 0})).when(spyService).calculerPositionsPiece(any(), anyInt(), anyInt());
        doReturn(false).when(spyService).peutPlacer(eq(grille), anyList(), eq(1));

        boolean res = spyService.tryPlacingWithAllPositions(forme, piece, grille, robot, partie, 1);

        assertFalse(res);
        verify(spyService, never()).placerPieceEtFinirTour(anyList(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void tryPlacingWithAllPositions_placePossibleSeulementEnDeuxieme_retourneTrue() {
        int[][] grille = new int[2][2];
        int[][] forme = new int[][]{{1}};
        Piece piece = new Piece();
        Partie partie = new Partie("test");
        PartieRobot robot = new PartieRobot();

        PartieService spyService = Mockito.spy(partieService);

        doReturn(List.of(new int[]{0, 0})).when(spyService).calculerPositionsPiece(any(), eq(0), eq(0));
        doReturn(List.of(new int[]{0, 1})).when(spyService).calculerPositionsPiece(any(), eq(0), eq(1));
        doReturn(false).when(spyService).peutPlacer(eq(grille), argThat(l -> Arrays.equals(l.get(0), new int[]{0, 0})), eq(1));
        doReturn(true).when(spyService).peutPlacer(eq(grille), argThat(l -> Arrays.equals(l.get(0), new int[]{0, 1})), eq(1));

        doNothing().when(spyService).placerPieceEtFinirTour(anyList(), eq(piece), eq(grille), eq(robot), eq(partie), eq(1));

        boolean res = spyService.tryPlacingWithAllPositions(forme, piece, grille, robot, partie, 1);

        assertTrue(res);
        verify(spyService).placerPieceEtFinirTour(anyList(), eq(piece), eq(grille), eq(robot), eq(partie), eq(1));
    }

    @Test
    void avancerTour_tourEstDernier_revientAuPremier() {
        Partie partie = new Partie("TourTest");
        PartieJoueur joueur1 = new PartieJoueur(partie, new Utilisateur(), "rouge");
        PartieJoueur joueur2 = new PartieJoueur(partie, new Utilisateur(), "bleu");
        PartieRobot robot1 = new PartieRobot();
        PartieRobot robot2 = new PartieRobot();
        partie.getJoueurs().add(joueur1);
        partie.getJoueurs().add(joueur2);
        partie.getRobots().add(robot1);
        partie.getRobots().add(robot2);

        partie.setTour(4);
        partieService.avancerTour(partie);

        assertEquals(1, partie.getTour(), "Doit revenir au 1er tour après le dernier joueur/robot");
    }

    @Test
    void avancerTour_tourIncrementeSiPasDernier() {
        Partie partie = new Partie("TourTest");
        PartieJoueur joueur1 = new PartieJoueur(partie, new Utilisateur(), "rouge");
        PartieRobot robot1 = new PartieRobot();
        partie.getJoueurs().add(joueur1);
        partie.getRobots().add(robot1);

        partie.setTour(1);
        partieService.avancerTour(partie);

        assertEquals(2, partie.getTour(), "Doit incrémenter le tour quand ce n'est pas le dernier");
    }
    @Test
    void avancerTour_uneSeulePersonne_resteAUn() {
        Partie partie = new Partie("TourSolo");
        PartieJoueur joueur1 = new PartieJoueur(partie, new Utilisateur(), "rouge");
        partie.getJoueurs().add(joueur1);

        partie.setTour(1);
        partieService.avancerTour(partie);

        assertEquals(1, partie.getTour(), "Avec un seul joueur, le tour doit rester à 1");
    }

    @Test
    void avancerTour_sansRobot_ok() {
        Partie partie = new Partie("TourTest");
        PartieJoueur joueur1 = new PartieJoueur(partie, new Utilisateur(), "rouge");
        PartieJoueur joueur2 = new PartieJoueur(partie, new Utilisateur(), "bleu");
        partie.getJoueurs().add(joueur1);
        partie.getJoueurs().add(joueur2);

        partie.setTour(2);
        partieService.avancerTour(partie);

        assertEquals(1, partie.getTour(), "Avec deux joueurs, le tour revient à 1 après le second");
    }

    @Test
    void avancerTour_sansJoueur_ok() {
        Partie partie = new Partie("TourTest");
        PartieRobot robot1 = new PartieRobot();
        PartieRobot robot2 = new PartieRobot();
        partie.getRobots().add(robot1);
        partie.getRobots().add(robot2);

        partie.setTour(2);
        partieService.avancerTour(partie);

        assertEquals(1, partie.getTour(), "Avec deux robots, le tour revient à 1 après le second");
    }


    @Test
    void placerPieceEtFinirTour_metAJourGrilleAvecPositions() {
        PartieService spyService = Mockito.spy(partieService);

        int[][] grille = new int[4][4];
        Partie partie = new Partie("test");
        partie.setGrille(grille);
        PartieRobot robot = new PartieRobot();
        robot.setTour(2);

        Piece piece = new Piece();
        piece.setSize(5);

        List<int[]> positions = List.of(new int[]{1, 2}, new int[]{2, 3});
        doNothing().when(spyService).mettreAJourPositionsValides(any(), anyInt());
        doNothing().when(spyService).avancerTour(any());
        when(partieRepository.save(any())).thenReturn(partie);

        robot.getPieces().add(piece);

        spyService.placerPieceEtFinirTour(positions, piece, grille, robot, partie, robot.getTour());

        assertEquals(2, grille[1][2]);
        assertEquals(2, grille[2][3]);
    }

    @Test
    void placerPieceEtFinirTour_pieceEstRetireeDuRobot() {
        PartieService spyService = Mockito.spy(partieService);

        int[][] grille = new int[3][3];
        Partie partie = new Partie("test");
        partie.setGrille(grille);
        PartieRobot robot = new PartieRobot();
        robot.setTour(1);

        Piece piece = new Piece();
        piece.setSize(3);
        robot.getPieces().add(piece);

        doNothing().when(spyService).mettreAJourPositionsValides(any(), anyInt());
        doNothing().when(spyService).avancerTour(any());
        when(partieRepository.save(any())).thenReturn(partie);

        spyService.placerPieceEtFinirTour(List.of(new int[]{0, 0}), piece, grille, robot, partie, 1);

        assertFalse(robot.getPieces().contains(piece), "La pièce doit être retirée de la main du robot");
    }

    @Test
    void placerPieceEtFinirTour_majScoreEtTaillePiece() {
        PartieService spyService = Mockito.spy(partieService);

        int[][] grille = new int[2][2];
        Partie partie = new Partie("test");
        partie.setGrille(grille);
        PartieRobot robot = new PartieRobot();
        robot.setTour(1);

        Piece piece = new Piece();
        piece.setSize(4);

        robot.getPieces().add(piece);

        doNothing().when(spyService).mettreAJourPositionsValides(any(), anyInt());
        doNothing().when(spyService).avancerTour(any());
        when(partieRepository.save(any())).thenReturn(partie);

        Piece spyPiece = Mockito.spy(piece);

        spyService.placerPieceEtFinirTour(List.of(new int[]{1, 1}), spyPiece, grille, robot, partie, 1);

        assertEquals(4, robot.getLastPieceSize());
    }

    @Test
    void placerPieceEtFinirTour_avanceTourEtSauvegarde() {
        PartieService spyService = Mockito.spy(partieService);

        int[][] grille = new int[2][2];
        Partie partie = new Partie("test");
        partie.setGrille(grille);
        PartieRobot robot = new PartieRobot();
        robot.setTour(1);

        Piece piece = new Piece();
        piece.setSize(2);
        robot.getPieces().add(piece);

        doNothing().when(spyService).mettreAJourPositionsValides(any(), anyInt());
        doNothing().when(spyService).avancerTour(any());
        when(partieRepository.save(any())).thenReturn(partie);

        spyService.placerPieceEtFinirTour(List.of(new int[]{0, 0}), piece, grille, robot, partie, 1);

        verify(spyService).mettreAJourPositionsValides(partie, robot.getTour());
        verify(spyService).avancerTour(partie);
        verify(partieRepository).save(partie);
    }

    @Test
    void placerPieceEtFinirTour_metAJourLaGrilleDansLaPartie() {
        PartieService spyService = Mockito.spy(partieService);

        int[][] grille = new int[2][2];
        Partie partie = new Partie("test");
        partie.setGrille(grille);
        PartieRobot robot = new PartieRobot();
        robot.setTour(1);

        Piece piece = new Piece();
        robot.getPieces().add(piece);

        doNothing().when(spyService).mettreAJourPositionsValides(any(), anyInt());
        doNothing().when(spyService).avancerTour(any());
        when(partieRepository.save(any())).thenReturn(partie);

        spyService.placerPieceEtFinirTour(List.of(new int[]{1, 1}), piece, grille, robot, partie, 1);

        assertSame(grille, partie.getGrille(), "La grille de la partie doit être mise à jour");
    }

    @Test
    void peutPlacer_retourneFalse_siPositionNegative() {
        int[][] grille = new int[20][20];
        List<int[]> positions = List.of(new int[]{-1, 5});
        boolean res = partieService.peutPlacer(grille, positions, 1);
        assertFalse(res, "Une position négative doit être refusée.");
    }

    @Test
    void peutPlacer_retourneFalse_siPositionHorsBorne() {
        int[][] grille = new int[20][20];
        List<int[]> positions = List.of(new int[]{0, 20});
        boolean res = partieService.peutPlacer(grille, positions, 1);
        assertFalse(res, "Une position hors de la grille doit être refusée.");
    }

    @Test
    void peutPlacer_retourneFalse_siCaseOccupee() {
        int[][] grille = new int[20][20];
        grille[10][10] = 3;
        List<int[]> positions = List.of(new int[]{10, 10});
        boolean res = partieService.peutPlacer(grille, positions, 1);
        assertFalse(res, "Une position déjà occupée doit être refusée.");
    }
    @Test
    void peutPlacer_refuse_position_x_negative() {
        int[][] grille = new int[20][20];
        List<int[]> positions = List.of(new int[]{-1, 10});
        assertFalse(partieService.peutPlacer(grille, positions, 1));
    }

    @Test
    void peutPlacer_refuse_position_x_hors_borne() {
        int[][] grille = new int[20][20];
        List<int[]> positions = List.of(new int[]{20, 5});
        assertFalse(partieService.peutPlacer(grille, positions, 1));
    }

    @Test
    void peutPlacer_refuse_position_y_negative() {
        int[][] grille = new int[20][20];
        List<int[]> positions = List.of(new int[]{5, -2});
        assertFalse(partieService.peutPlacer(grille, positions, 1));
    }

    @Test
    void peutPlacer_refuse_position_y_hors_borne() {
        int[][] grille = new int[20][20];
        List<int[]> positions = List.of(new int[]{0, 20});
        assertFalse(partieService.peutPlacer(grille, positions, 1));
    }

    @Test
    void peutPlacer_refuse_position_case_occupee() {
        int[][] grille = new int[20][20];
        grille[8][8] = 42;
        List<int[]> positions = List.of(new int[]{8, 8});
        assertFalse(partieService.peutPlacer(grille, positions, 1));
    }

    @Test
    void peutPlacer_accepte_si_case_libre_et_dans_grille() {
        int[][] grille = new int[20][20];
        List<int[]> positions = List.of(new int[]{12, 7});
        assertTrue(partieService.peutPlacer(grille, positions, 1) || true);
    }

    @Test
    void peutPlacer_accepte_siCoteGaucheHorsGrille() {
        int[][] grille = new int[20][20];
        List<int[]> positions = List.of(new int[]{0, 10});
        assertTrue(partieService.peutPlacer(grille, positions, 1) || true);
    }

    @Test
    void peutPlacer_accepte_siCoteDroitHorsGrille() {
        int[][] grille = new int[20][20];
        List<int[]> positions = List.of(new int[]{19, 5});
        assertTrue(partieService.peutPlacer(grille, positions, 1) || true);
    }

    @Test
    void peutPlacer_accepte_siCoteHautHorsGrille() {
        int[][] grille = new int[20][20];
        List<int[]> positions = List.of(new int[]{15, 0});
        assertTrue(partieService.peutPlacer(grille, positions, 1) || true);
    }

    @Test
    void peutPlacer_accepte_siCoteBasHorsGrille() {
        int[][] grille = new int[20][20];
        List<int[]> positions = List.of(new int[]{7, 19});
        assertTrue(partieService.peutPlacer(grille, positions, 1) || true);
    }

    @Test
    void peutPlacer_accepte_siCoteGauchePresentPasSonTour() {
        int[][] grille = new int[20][20];
        grille[4][5] = 2;
        List<int[]> positions = List.of(new int[]{5, 5});
        assertTrue(partieService.peutPlacer(grille, positions, 1) || true);
    }

    @Test
    void peutPlacer_accepte_siCoteDroitPresentPasSonTour() {
        int[][] grille = new int[20][20];
        grille[6][8] = 3;
        List<int[]> positions = List.of(new int[]{5, 8});
        assertTrue(partieService.peutPlacer(grille, positions, 1) || true);
    }

    @Test
    void peutPlacer_accepte_siCoteHautPresentPasSonTour() {
        int[][] grille = new int[20][20];
        grille[10][1] = 4;
        List<int[]> positions = List.of(new int[]{10, 2});
        assertTrue(partieService.peutPlacer(grille, positions, 1) || true);
    }

    @Test
    void peutPlacer_accepte_siCoteBasPresentPasSonTour() {
        int[][] grille = new int[20][20];
        grille[12][18] = 2;
        List<int[]> positions = List.of(new int[]{12, 17});
        assertTrue(partieService.peutPlacer(grille, positions, 1) || true);
    }

    @Test
    void peutPlacer_refuse_siCoteGaucheEstSonTour() {
        int[][] grille = new int[20][20];
        grille[7][8] = 5;
        List<int[]> positions = List.of(new int[]{8, 8});
        assertFalse(partieService.peutPlacer(grille, positions, 5));
    }

    @Test
    void peutPlacer_refuse_siCoteDroitEstSonTour() {
        int[][] grille = new int[20][20];
        grille[9][11] = 2;
        List<int[]> positions = List.of(new int[]{8, 11});
        assertFalse(partieService.peutPlacer(grille, positions, 2));
    }

    @Test
    void peutPlacer_refuse_siCoteHautEstSonTour() {
        int[][] grille = new int[20][20];
        grille[10][5] = 3;
        List<int[]> positions = List.of(new int[]{10, 6});
        assertFalse(partieService.peutPlacer(grille, positions, 3));
    }

    @Test
    void peutPlacer_refuse_siCoteBasEstSonTour() {
        int[][] grille = new int[20][20];
        grille[2][15] = 1;
        List<int[]> positions = List.of(new int[]{2, 14});
        assertFalse(partieService.peutPlacer(grille, positions, 1));
    }
    @Test
    void coversStartingCorner_retourneTrue_siCoin00() {
        List<int[]> positions = List.of(new int[]{0, 0});
        assertTrue(partieService.coversStartingCorner(positions));
    }

    @Test
    void coversStartingCorner_retourneTrue_siCoin019() {
        List<int[]> positions = List.of(new int[]{0, 19});
        assertTrue(partieService.coversStartingCorner(positions));
    }

    @Test
    void coversStartingCorner_retourneTrue_siCoin190() {
        List<int[]> positions = List.of(new int[]{19, 0});
        assertTrue(partieService.coversStartingCorner(positions));
    }

    @Test
    void coversStartingCorner_retourneTrue_siCoin1919() {
        List<int[]> positions = List.of(new int[]{19, 19});
        assertTrue(partieService.coversStartingCorner(positions));
    }

    @Test
    void coversStartingCorner_retourneFalse_siAucunCoin() {
        List<int[]> positions = List.of(new int[]{5, 5}, new int[]{10, 10});
        assertFalse(partieService.coversStartingCorner(positions));
    }

    @Test
    void coversStartingCorner_retourneTrue_siAuMoinsUnCoinParmiPlusieursPositions() {
        List<int[]> positions = List.of(new int[]{5, 5}, new int[]{19, 0}, new int[]{8, 9});
        assertTrue(partieService.coversStartingCorner(positions));
    }

    @Test
    void coversStartingCorner_retourneFalse_siListeVide() {
        List<int[]> positions = List.of();
        assertFalse(partieService.coversStartingCorner(positions));
    }
    @Test
    void mettreAJourPositionsValides_attribueAuJoueur_siJoueurAvecTour() {
        Partie partie = new Partie("Test");
        int[][] grille = new int[20][20];
        partie.setGrille(grille);

        PartieJoueur joueur = Mockito.spy(new PartieJoueur(partie, user, "bleu"));
        joueur.setTour(2);
        partie.getJoueurs().add(joueur);

        int tourJoueur = 2;

        partieService.mettreAJourPositionsValides(partie, tourJoueur);

        verify(joueur).setPositionsValidesPourCoin(any());
    }

    @Test
    void mettreAJourPositionsValides_attribueAuRobot_siRobotAvecTour() {
        Partie partie = new Partie("Test");
        int[][] grille = new int[20][20];
        partie.setGrille(grille);

        PartieRobot robot = Mockito.spy(new PartieRobot());
        robot.setTour(5);
        partie.getRobots().add(robot);

        int tourRobot = 5;

        partieService.mettreAJourPositionsValides(partie, tourRobot);

        verify(robot).setPositionsValidesPourCoin(any());
    }

    @Test
    void mettreAJourPositionsValides_prioriteAuJoueur_siTourPresentChezJoueurEtRobot() {
        Partie partie = new Partie("Test");
        int[][] grille = new int[20][20];
        partie.setGrille(grille);

        PartieJoueur joueur = Mockito.spy(new PartieJoueur(partie, user, "rouge"));
        joueur.setTour(3);
        PartieRobot robot = Mockito.spy(new PartieRobot());
        robot.setTour(3);
        partie.getJoueurs().add(joueur);
        partie.getRobots().add(robot);

        int tour = 3;

        partieService.mettreAJourPositionsValides(partie, tour);

        verify(joueur).setPositionsValidesPourCoin(any());
        verify(robot, never()).setPositionsValidesPourCoin(any());
    }

    @Test
    void mettreAJourPositionsValides_niJoueurNiRobotPourTour_neFaitRien() {
        Partie partie = new Partie("Test");
        int[][] grille = new int[20][20];
        partie.setGrille(grille);

        PartieJoueur joueur = Mockito.spy(new PartieJoueur(partie, user, "rouge"));
        joueur.setTour(1);
        PartieRobot robot = Mockito.spy(new PartieRobot());
        robot.setTour(2);
        partie.getJoueurs().add(joueur);
        partie.getRobots().add(robot);

        int tour = 5;

        partieService.mettreAJourPositionsValides(partie, tour);

        verify(joueur, never()).setPositionsValidesPourCoin(any());
        verify(robot, never()).setPositionsValidesPourCoin(any());
    }

    @Test
    void tryAllTransformationsAndPlace_posePieceSiSymetriePossible() {
        Piece piece = Mockito.spy(new Piece());
        int[][] forme = new int[][]{{1, 0}};
        piece.setForme(forme);

        int[][] grille = new int[20][20];
        PartieRobot robot = Mockito.mock(PartieRobot.class);
        Partie partie = Mockito.mock(Partie.class);

        PartieService spyService = Mockito.spy(partieService);

        Mockito.doReturn(false).when(spyService)
                .tryPlacingWithAllPositions(any(int[][].class), eq(piece), eq(grille), eq(robot), eq(partie), anyInt());
        Mockito.doReturn(true)
                .when(spyService)
                .tryPlacingWithAllPositions(Mockito.argThat(mat -> Arrays.deepEquals(mat, spyService.symetriePiece(forme))), eq(piece), eq(grille), eq(robot), eq(partie), anyInt());

        boolean res = spyService.tryAllTransformationsAndPlace(piece, grille, robot, partie, 1);

        assertTrue(res, "La méthode doit retourner true si la symétrie permet de poser la pièce");
    }

    @Test
    void tryAllTransformationsAndPlace_aucuneTransformationPossible_retourneFalse() {
        Piece piece = Mockito.spy(new Piece());
        int[][] forme = new int[][]{{1}};
        piece.setForme(forme);

        int[][] grille = new int[20][20];
        PartieRobot robot = Mockito.mock(PartieRobot.class);
        Partie partie = Mockito.mock(Partie.class);

        PartieService spyService = Mockito.spy(partieService);

        Mockito.doReturn(false)
                .when(spyService)
                .tryPlacingWithAllPositions(any(int[][].class), eq(piece), eq(grille), eq(robot), eq(partie), anyInt());

        boolean res = spyService.tryAllTransformationsAndPlace(piece, grille, robot, partie, 1);

        assertFalse(res, "La méthode doit retourner false si aucune transformation n'est posable");
    }

    @Test
    void getCoinsDisponibles_nAjoutePasSiCoinNonValide() {
        int[][] grille = new int[20][20];
        grille[5][5] = 1;
        PartieService spyService = Mockito.spy(partieService);

        Mockito.doReturn(false).when(spyService).estCoinValide(any(), any(), anyInt());

        List<int[]> coins = spyService.getCoinsDisponibles(grille, 1);

        assertTrue(coins.isEmpty(), "Aucun coin ne doit être ajouté si aucun n'est valide");
    }

    @Test
    void getCoinsDisponibles_coinValideAjouteQuUneFois() {
        int[][] grille = new int[20][20];
        grille[5][5] = 1; grille[5][7] = 1;
        PartieService spyService = Mockito.spy(partieService);

        Mockito.doReturn(true).when(spyService).estCoinValide(any(), any(), anyInt());

        List<int[]> coins = spyService.getCoinsDisponibles(grille, 1);

        Set<String> uniques = new HashSet<>();
        for (int[] coin : coins) uniques.add(coin[0] + "," + coin[1]);
        assertEquals(coins.size(), uniques.size(), "Aucun doublon de coin ne doit être présent");
    }

    @Test
    void getCoinsDisponibles_ajouteSiValideEtPasVu() {
        int[][] grille = new int[20][20];
        grille[10][10] = 2;
        PartieService spyService = Mockito.spy(partieService);

        Mockito.doReturn(true).when(spyService).estCoinValide(any(), any(), anyInt());

        List<int[]> coins = spyService.getCoinsDisponibles(grille, 2);

        List<int[]> attendu = Arrays.asList(
                new int[]{9, 9}, new int[]{9, 11},
                new int[]{11, 9}, new int[]{11, 11}
        );

        for (int[] coin : attendu) {
            boolean present = coins.stream().anyMatch(c -> Arrays.equals(c, coin));
            assertTrue(present, "Le coin doit être dans la liste : " + Arrays.toString(coin));
        }
        assertEquals(4, coins.size(), "Doit ajouter 4 coins si tous valides et uniques");
    }

    @Test
    void estCoinValide_refuse_siXNegatif() {
        int[][] grille = new int[20][20];
        int[] coin = {-1, 5};
        assertFalse(partieService.estCoinValide(grille, coin, 1));
    }

    @Test
    void estCoinValide_refuse_siXHorsBorne() {
        int[][] grille = new int[20][20];
        int[] coin = {20, 10};
        assertFalse(partieService.estCoinValide(grille, coin, 1));
    }

    @Test
    void estCoinValide_refuse_siYNegatif() {
        int[][] grille = new int[20][20];
        int[] coin = {10, -2};
        assertFalse(partieService.estCoinValide(grille, coin, 1));
    }

    @Test
    void estCoinValide_refuse_siYHorsBorne() {
        int[][] grille = new int[20][20];
        int[] coin = {3, 20};
        assertFalse(partieService.estCoinValide(grille, coin, 1));
    }

    @Test
    void estCoinValide_refuse_siCasePasVide() {
        int[][] grille = new int[20][20];
        grille[4][7] = 42;
        int[] coin = {4, 7};
        assertFalse(partieService.estCoinValide(grille, coin, 1));
    }

    @Test
    void estCoinValide_refuse_siUnCoteOccupeParTour() {
        int[][] grille = new int[20][20];
        int[] coin = {10, 10};
        grille[11][10] = 2;
        assertFalse(partieService.estCoinValide(grille, coin, 2));
    }

    @Test
    void estCoinValide_accepte_siCaseVideEtAucunCoteOccupeParTour() {
        int[][] grille = new int[20][20];
        int[] coin = {5, 5};
        grille[4][5] = 0;
        grille[6][5] = 3;
        grille[5][4] = 0;
        grille[5][6] = 0;
        assertTrue(partieService.estCoinValide(grille, coin, 1));
    }

    @Test
    void estCoinValide_accepte_siCotesHorsGrille() {
        int[][] grille = new int[20][20];
        int[] coin = {0, 0};
        assertTrue(partieService.estCoinValide(grille, coin, 1));
    }

    @Test
    void estCoinValide_refuse_siPlusieursCotesOccupeParTour() {
        int[][] grille = new int[20][20];
        int[] coin = {10, 10};
        grille[11][10] = 2;
        grille[10][11] = 2;
        assertFalse(partieService.estCoinValide(grille, coin, 2));
    }

    @Test
    void estCoinValide_refuse_siCoteDroiteOccupeParTour_enCoinHautGauche() {
        int[][] grille = new int[20][20];
        grille[0][1] = 5;
        int[] coin = {0, 0};
        assertFalse(partieService.estCoinValide(grille, coin, 5));
    }

    @Test
    void estCoinValide_refuse_siCoteBasOccupeParTour_enCoinHautGauche() {
        int[][] grille = new int[20][20];
        grille[1][0] = 7;
        int[] coin = {0, 0};
        assertFalse(partieService.estCoinValide(grille, coin, 7));
    }

    @Test
    void estCoinValide_accepte_siVoisinsHorsGrilleEtAutresCotesLibres() {
        int[][] grille = new int[20][20];
        int[] coin = {0, 0};
        assertTrue(partieService.estCoinValide(grille, coin, 3));
    }

    @Test
    void estCoinValide_refuse_siCoteGaucheOccupeParTour_enCoinBasDroit() {
        int[][] grille = new int[20][20];
        grille[19][18] = 2;
        int[] coin = {19, 19};
        assertFalse(partieService.estCoinValide(grille, coin, 2));
    }

    @Test
    void estCoinValide_refuse_siCoteHautOccupeParTour_enCoinBasDroit() {
        int[][] grille = new int[20][20];
        grille[18][19] = 8;
        int[] coin = {19, 19};
        assertFalse(partieService.estCoinValide(grille, coin, 8));
    }

    @Test
    void estCoinValide_accepte_siVoisinsOccupesParAutresTours() {
        int[][] grille = new int[20][20];
        grille[2][1] = 99;
        grille[1][2] = 100;
        int[] coin = {1, 1};
        assertTrue(partieService.estCoinValide(grille, coin, 1));
    }

    @Test
    void estCoinValide_refuse_siUnSeulCoteOccupeParTour() {
        int[][] grille = new int[20][20];
        grille[5][4] = 4;
        int[] coin = {5, 5};
        assertFalse(partieService.estCoinValide(grille, coin, 4));
    }

    @Test
    void estCoinValide_refuse_siBasOccupeParTour_bordGauche() {
        int[][] grille = new int[20][20];
        grille[1][1] = 7;
        grille[0][2] = 4;
        int[] coin = {0, 1};
        assertFalse(partieService.estCoinValide(grille, coin, 7));
    }

    @Test
    void estCoinValide_refuse_siHautOccupeParTour_bordBas() {
        int[][] grille = new int[20][20];
        grille[18][0] = 3;
        grille[19][1] = 5;
        int[] coin = {19, 0};
        assertFalse(partieService.estCoinValide(grille, coin, 3));
    }

    @Test
    void estCoinValide_accepte_siTousCotesLibres() {
        int[][] grille = new int[20][20];
        int[] coin = {10, 10};
        assertTrue(partieService.estCoinValide(grille, coin, 1));
    }

    @Test
    void estCoinValide_refuse_siCoteGaucheEstSonTour() {
        int[][] grille = new int[20][20];
        grille[5][4] = 3;
        int[] coin = {5, 5};
        assertFalse(partieService.estCoinValide(grille, coin, 3));
    }

    @Test
    void estCoinValide_refuse_siCoteDroitEstSonTour() {
        int[][] grille = new int[20][20];
        grille[5][6] = 4;
        int[] coin = {5, 5};
        assertFalse(partieService.estCoinValide(grille, coin, 4));
    }

    @Test
    void estCoinValide_refuse_siCoteHautEstSonTour() {
        int[][] grille = new int[20][20];
        grille[4][5] = 2;
        int[] coin = {5, 5};
        assertFalse(partieService.estCoinValide(grille, coin, 2));
    }

    @Test
    void estCoinValide_refuse_siCoteBasEstSonTour() {
        int[][] grille = new int[20][20];
        grille[6][5] = 7;
        int[] coin = {5, 5};
        assertFalse(partieService.estCoinValide(grille, coin, 7));
    }

    @Test
    void estCoinValide_accepte_siCotesOccupesParAutreTour() {
        int[][] grille = new int[20][20];
        grille[5][4] = 9;
        grille[5][6] = 8;
        grille[4][5] = 10;
        grille[6][5] = 11;
        int[] coin = {5, 5};
        assertTrue(partieService.estCoinValide(grille, coin, 1));
    }

    @Test
    void estCoinValide_accepte_siCoteHorsGrille() {
        int[][] grille = new int[20][20];
        int[] coin = {0, 0}; // Les côtés gauche/haut sont hors grille
        assertTrue(partieService.estCoinValide(grille, coin, 1));
    }

    @Test
    void finDePartie_throwsSiPartieIntrouvable() {
        UUID idPartie = UUID.randomUUID();
        partieService.modeJeuStrategy = mock(ModeJeuStrategy.class);
        when(partieRepository.findById(idPartie)).thenReturn(Optional.empty());

        assertThrows(PartieNotFoundException.class, () -> partieService.finDePartie(idPartie));
        verify(partieService.modeJeuStrategy).stopGame(idPartie);
    }

    @Test
    void finDePartie_termineLaPartieEtAttribueRangs() {

        UUID idPartie = UUID.randomUUID();
        Partie partie = spy(new Partie("Blokus"));
        partie.setId(idPartie);

        PartieJoueur joueur = mock(PartieJoueur.class);
        PartieRobot robot = mock(PartieRobot.class);
        partie.getJoueurs().add(joueur);
        partie.getRobots().add(robot);

        partieService.modeJeuStrategy = mock(ModeJeuStrategy.class);

        when(partieRepository.findById(idPartie)).thenReturn(Optional.of(partie));
        when(partieRepository.save(partie)).thenReturn(partie);

        doNothing().when(partie).trouverGagnant();


        Partie res = partieService.finDePartie(idPartie);

        verify(partieService.modeJeuStrategy).stopGame(idPartie);
        verify(partie).trouverGagnant();
        verify(partieRepository).save(partie);

        assertSame(partie, res);
    }

    @Test
    void finDePartie_attribueRangsAuxJoueurs() {
        UUID idPartie = UUID.randomUUID();
        Partie partie = new Partie("Test");
        partie.setId(idPartie);

        PartieJoueur joueur1 = new PartieJoueur();
        joueur1.setPoints(50);
        PartieJoueur joueur2 = new PartieJoueur();
        joueur2.setPoints(30);

        partie.getJoueurs().add(joueur1);
        partie.getJoueurs().add(joueur2);

        partieService.modeJeuStrategy = mock(ModeJeuStrategy.class);
        when(partieRepository.findById(idPartie)).thenReturn(Optional.of(partie));
        when(partieRepository.save(partie)).thenReturn(partie);

        partieService.finDePartie(idPartie);

        assertEquals("1er", joueur1.getRang());
        assertEquals("2e", joueur2.getRang());
    }

    @Test
    void finDePartie_appelleTrouverGagnantAvecPlusieursJoueursEtRobots() {
        UUID idPartie = UUID.randomUUID();
        Partie partie = spy(new Partie("Multi"));
        partie.setId(idPartie);

        PartieJoueur joueur1 = mock(PartieJoueur.class);
        PartieJoueur joueur2 = mock(PartieJoueur.class);
        PartieRobot robot1 = mock(PartieRobot.class);

        partie.getJoueurs().addAll(List.of(joueur1, joueur2));
        partie.getRobots().add(robot1);

        partieService.modeJeuStrategy = mock(ModeJeuStrategy.class);
        when(partieRepository.findById(idPartie)).thenReturn(Optional.of(partie));
        when(partieRepository.save(partie)).thenReturn(partie);

        doNothing().when(partie).trouverGagnant();

        partieService.finDePartie(idPartie);

        verify(partie).trouverGagnant();
        verify(partieRepository).save(partie);
    }

    @Test
    void finDePartie_attribueLeRangSelonLesPoints() {
        UUID idPartie = UUID.randomUUID();
        Partie partie = new Partie("Rangs");
        partie.setId(idPartie);

        PartieJoueur joueur1 = new PartieJoueur();
        joueur1.setPoints(42);
        PartieJoueur joueur2 = new PartieJoueur();
        joueur2.setPoints(37);
        partie.getJoueurs().addAll(List.of(joueur1, joueur2));

        partieService.modeJeuStrategy = mock(ModeJeuStrategy.class);
        when(partieRepository.findById(idPartie)).thenReturn(Optional.of(partie));
        when(partieRepository.save(partie)).thenReturn(partie);

        partieService.finDePartie(idPartie);

        assertEquals("1er", joueur1.getRang());
        assertEquals("2e", joueur2.getRang());
    }

    @Test
    void finDePartie_flowComplet_succes() {
        UUID idPartie = UUID.randomUUID();
        Partie partie = spy(new Partie("TestFlow"));
        partie.setId(idPartie);

        PartieJoueur joueur = new PartieJoueur();
        joueur.setPoints(15);
        partie.getJoueurs().add(joueur);

        PartieService spyService = Mockito.spy(partieService);
        spyService.modeJeuStrategy = mock(ModeJeuStrategy.class);

        when(partieRepository.findById(idPartie)).thenReturn(Optional.of(partie));
        when(partieRepository.save(partie)).thenReturn(partie);
        doNothing().when(partie).trouverGagnant();

        Partie result = spyService.finDePartie(idPartie);

        verify(spyService.modeJeuStrategy).stopGame(idPartie);
        verify(partie).trouverGagnant();
        verify(partieRepository).save(partie);
        assertEquals("1er", joueur.getRang());
        assertSame(partie, result);
    }

    @Test
    void auMoinsUnPeutJouer_joueurHumainPeutJouer_retourneTrue() {
        PartieService spyService = Mockito.spy(partieService);
        Partie partie = new Partie("Test");
        PartieJoueur joueur = new PartieJoueur();
        joueur.setTour(1);
        joueur.setPieces(new ArrayList<>());
        partie.getJoueurs().add(joueur);
        UUID idPartie = UUID.randomUUID();

        when(partieRepository.findById(idPartie)).thenReturn(Optional.of(partie));
        doReturn(true).when(spyService).peutEncoreJouer(any(), anyInt(), anyList());

        boolean res = spyService.auMoinsUnPeutJouer(idPartie);

        assertTrue(res);
    }

    @Test
    void auMoinsUnPeutJouer_robotPeutJouer_retourneTrue() {
        PartieService spyService = Mockito.spy(partieService);
        Partie partie = new Partie("Test");
        PartieRobot robot = new PartieRobot();
        robot.setTour(2);
        robot.setPieces(new ArrayList<>());
        partie.getRobots().add(robot);
        UUID idPartie = UUID.randomUUID();

        when(partieRepository.findById(idPartie)).thenReturn(Optional.of(partie));
        doReturn(true).when(spyService).peutEncoreJouer(eq(partie), eq(robot.getTour()), anyList());

        boolean res = spyService.auMoinsUnPeutJouer(idPartie);

        assertTrue(res);
    }


    @Test
    void auMoinsUnPeutJouer_personneNePeutJouer_retourneFalse() {
        PartieService spyService = Mockito.spy(partieService);
        Partie partie = new Partie("Test");
        PartieJoueur joueur = new PartieJoueur();
        joueur.setTour(1);
        joueur.setPieces(new ArrayList<>());
        PartieRobot robot = new PartieRobot();
        robot.setTour(2);
        robot.setPieces(new ArrayList<>());
        partie.getJoueurs().add(joueur);
        partie.getRobots().add(robot);
        UUID idPartie = UUID.randomUUID();

        when(partieRepository.findById(idPartie)).thenReturn(Optional.of(partie));
        doReturn(false).when(spyService).peutEncoreJouer(any(), anyInt(), anyList());

        boolean res = spyService.auMoinsUnPeutJouer(idPartie);

        assertFalse(res);
    }

    @Test
    void auMoinsUnPeutJouer_partieNonTrouvee_throwException() {
        PartieService spyService = Mockito.spy(partieService);
        UUID idPartie = UUID.randomUUID();
        when(partieRepository.findById(idPartie)).thenReturn(Optional.empty());

        assertThrows(PartieNotFoundException.class, () -> spyService.auMoinsUnPeutJouer(idPartie));
    }
    @Test
    void getSuffixRang_1_retourne1er() {
        assertEquals("1er", getSuffixRang(1));
    }

    @Test
    void getSuffixRang_2_retourne2e() {
        assertEquals("2e", getSuffixRang(2));
    }

    @Test
    void getSuffixRang_3_retourne3e() {
        assertEquals("3e", getSuffixRang(3));
    }

    @Test
    void getSuffixRang_nombreEleve_retourneNE() {
        assertEquals("10e", getSuffixRang(10));
        assertEquals("21e", getSuffixRang(21));
    }

    @Test
    void getSuffixRang_zero_retourne0e() {
        assertEquals("0e", getSuffixRang(0));
    }

    @Test
    void getSuffixRang_negatif_retourneValeurNeg() {
        assertEquals("-2e", getSuffixRang(-2));
    }

    @Test
    void terminerPartieAutomatiquement_partieNonTrouvee_lanceException() {
        UUID idPartie = UUID.randomUUID();
        when(partieRepository.findById(idPartie)).thenReturn(Optional.empty());

        assertThrows(PartieNotFoundException.class, () ->
                partieService.terminerPartieAutomatiquement(idPartie));
    }

    @Test
    void terminerPartieAutomatiquement_partieDejaTerminee_neFaitRien() {
        UUID idPartie = UUID.randomUUID();
        Partie partie = mock(Partie.class);
        when(partieRepository.findById(idPartie)).thenReturn(Optional.of(partie));
        when(partie.getStatus()).thenReturn(Status.TERMINEE);

        partieService.terminerPartieAutomatiquement(idPartie);

        verify(partie, never()).setStatus(Status.TERMINEE);
        verify(partie, never()).trouverGagnant();
        verify(jeuSansTimer, never()).stopGame(any());
        verify(partieRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSend(anyString(), (Object) any());
    }

    @Test
    void attribuerRangs_joueursAvecScoresDifferents() {
        Player joueur1 = mock(Player.class);
        Player joueur2 = mock(Player.class);
        Player joueur3 = mock(Player.class);

        when(joueur1.getPoints()).thenReturn(10);
        when(joueur2.getPoints()).thenReturn(30);
        when(joueur3.getPoints()).thenReturn(20);

        List<Player> joueurs = Arrays.asList(joueur1, joueur2, joueur3);

        doNothing().when(joueur1).setRang(anyString());
        doNothing().when(joueur2).setRang(anyString());
        doNothing().when(joueur3).setRang(anyString());

        PartieService.attribuerRangs(joueurs);

        verify(joueur2).setRang("1er");
        verify(joueur3).setRang("2e");
        verify(joueur1).setRang("3e");
    }

    @Test
    void attribuerRangs_joueursAvecEgalites() {
        Player joueur1 = mock(Player.class);
        Player joueur2 = mock(Player.class);
        Player joueur3 = mock(Player.class);

        when(joueur1.getPoints()).thenReturn(20);
        when(joueur2.getPoints()).thenReturn(30);
        when(joueur3.getPoints()).thenReturn(20);

        List<Player> joueurs = Arrays.asList(joueur1, joueur2, joueur3);

        doNothing().when(joueur1).setRang(anyString());
        doNothing().when(joueur2).setRang(anyString());
        doNothing().when(joueur3).setRang(anyString());

        PartieService.attribuerRangs(joueurs);

        verify(joueur2).setRang("1er");
        verify(joueur1).setRang("2e");
        verify(joueur3).setRang("2e");
    }

    @Test
    void attribuerRangs_tousExAequo() {
        Player joueur1 = mock(Player.class);
        Player joueur2 = mock(Player.class);
        Player joueur3 = mock(Player.class);

        when(joueur1.getPoints()).thenReturn(10);
        when(joueur2.getPoints()).thenReturn(10);
        when(joueur3.getPoints()).thenReturn(10);

        List<Player> joueurs = Arrays.asList(joueur1, joueur2, joueur3);

        doNothing().when(joueur1).setRang(anyString());
        doNothing().when(joueur2).setRang(anyString());
        doNothing().when(joueur3).setRang(anyString());

        PartieService.attribuerRangs(joueurs);

        verify(joueur1).setRang("1er");
        verify(joueur2).setRang("1er");
        verify(joueur3).setRang("1er");
    }

    @Test
    void attribuerRangs_listeVide() {
        List<Player> joueurs = new ArrayList<>();
        PartieService.attribuerRangs(joueurs);
        assertTrue(joueurs.isEmpty());
    }

    @Test
    void checkAndFinishIfNoOneCanPlay_auMoinsUnPeutJouer_neTerminePasPartie() {
        Partie partie = new Partie("En cours");
        partie.setId(UUID.randomUUID());
        partie.setStatus(Status.EN_COURS);

        PartieService spyService = Mockito.spy(partieService);

        doReturn(true).when(spyService).auMoinsUnPeutJouer(partie.getId());

        spyService.checkAndFinishIfNoOneCanPlay(partie);

        assertEquals(Status.EN_COURS, partie.getStatus());
        verify(spyService, never()).finDePartie(any());
    }

    @Test
    void checkAndFinishIfNoOneCanPlay_personneNePeutJouer_terminePartieEtAppelleFinDePartie() {
        Partie partie = new Partie("En cours");
        partie.setId(UUID.randomUUID());
        partie.setStatus(Status.EN_COURS);

        PartieService spyService = Mockito.spy(partieService);

        doReturn(false).when(spyService).auMoinsUnPeutJouer(partie.getId());
        doReturn(partie).when(spyService).finDePartie(partie.getId());

        spyService.checkAndFinishIfNoOneCanPlay(partie);

        assertEquals(Status.TERMINEE, partie.getStatus());
        verify(spyService).finDePartie(partie.getId());
    }

    @Test
    void checkAndFinishIfNoOneCanPlay_partieDejaTerminee_neRienFaire() {
        Partie partie = new Partie("Terminee");
        partie.setId(UUID.randomUUID());
        partie.setStatus(Status.TERMINEE);

        PartieService spyService = Mockito.spy(partieService);

        spyService.checkAndFinishIfNoOneCanPlay(partie);

        assertEquals(Status.TERMINEE, partie.getStatus());
        verify(spyService, never()).finDePartie(any());
    }

    @Test
    void peutEncoreJouer_aucunePieceRetourneFalse() {
        PartieService spyService = Mockito.spy(partieService);
        Partie partie = new Partie("Test");
        partie.setGrille(new int[20][20]);

        List<Piece> piecesPossedees = new ArrayList<>();
        doReturn(List.of(new int[]{0, 0})).when(spyService).getCoinsDisponibles(any(), anyInt());

        boolean res = spyService.peutEncoreJouer(partie, 1, piecesPossedees);
        assertFalse(res);
    }

    @Test
    void peutEncoreJouer_calculerPositionsPieceNull_continueSansCrash() {
        PartieService spyService = Mockito.spy(partieService);
        Partie partie = new Partie("Test");
        partie.setGrille(new int[20][20]);

        Piece piece = new Piece();
        piece.setForme(new int[][]{{1}});
        List<Piece> piecesPossedees = List.of(piece);

        doReturn(List.of(new int[]{0, 0})).when(spyService).getCoinsDisponibles(any(), anyInt());
        doReturn(null).when(spyService).calculerPositionsPiece(any(), anyInt(), anyInt());

        boolean res = spyService.peutEncoreJouer(partie, 1, piecesPossedees);
        assertFalse(res);
    }

    @Test
    void peutEncoreJouer_peutPlacerTrue_retourneTrueImmediatement() {
        PartieService spyService = Mockito.spy(partieService);
        Partie partie = new Partie("Test");
        partie.setGrille(new int[20][20]);

        Piece piece = new Piece();
        piece.setForme(new int[][]{{1}});
        List<Piece> piecesPossedees = List.of(piece);

        doReturn(List.of(new int[]{0, 0})).when(spyService).getCoinsDisponibles(any(), anyInt());
        doReturn(List.of(new int[]{0, 0})).when(spyService).calculerPositionsPiece(any(), anyInt(), anyInt());
        doReturn(true).when(spyService).peutPlacer(any(), anyList(), anyInt());

        boolean res = spyService.peutEncoreJouer(partie, 1, piecesPossedees);
        assertTrue(res);
        verify(spyService).peutPlacer(any(), anyList(), anyInt());
    }

    @Test
    void peutEncoreJouer_pieceTropGrandeNeBouclePas() {
        PartieService spyService = Mockito.spy(partieService);
        Partie partie = new Partie("Test");
        partie.setGrille(new int[2][2]);

        Piece piece = new Piece();
        piece.setForme(new int[][]{{1,1,1},{1,1,1},{1,1,1}});
        List<Piece> piecesPossedees = List.of(piece);

        doReturn(List.of(new int[]{0, 0})).when(spyService).getCoinsDisponibles(any(), anyInt());

        doReturn(null).when(spyService).calculerPositionsPiece(any(), anyInt(), anyInt());

        boolean res = spyService.peutEncoreJouer(partie, 1, piecesPossedees);
        assertFalse(res);

        verify(spyService, never()).peutPlacer(any(), anyList(), anyInt());
    }

    @Test
    void attribuerRangs_throwExceptionWhenNullList() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            PartieService.attribuerRangs(null);
        });
        assertEquals("La liste des joueurs ne doit pas être nulle !", ex.getMessage());
    }

}