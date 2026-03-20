package fr.orleans.m1miage.project.s2.projetS2.sevicesTest;

import fr.orleans.m1miage.project.s2.projetS2.model.*;
import fr.orleans.m1miage.project.s2.projetS2.repository.PartieRepository;
import fr.orleans.m1miage.project.s2.projetS2.service.StatistiquesServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StatistiquesServiceImplTest {

    @Mock
    private PartieRepository partieRepository;

    @InjectMocks
    private StatistiquesServiceImpl statistiquesService;

    private UUID userId;
    private UUID partieId;
    private UUID autreId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userId = UUID.randomUUID();
        partieId = UUID.randomUUID();
        autreId = UUID.randomUUID();

    }

    @Test
    void testGetStatisticsForUser_NoGames() {
        when(partieRepository.findByJoueursUtilisateurIdAndStatus(userId, Status.TERMINEE)).thenReturn(Collections.emptyList());

        JoueurStatistique stats = statistiquesService.getStatisticsForUser(userId);

        assertEquals(0, stats.getNbPartiesJouees());
        assertEquals(0, stats.getNbPartiesGagnees());
        assertEquals(0.0, stats.getRatioPartiesGagnees());
    }

    @Test
    void testStatistiques_UtilisateurGagneEtPerd() {
        Partie partieGagnee = new Partie();
        partieGagnee.setStatus(Status.TERMINEE);
        Utilisateur user = new Utilisateur();
        user.setId(userId);
        PartieJoueur pjWin = new PartieJoueur(partieGagnee, user, "red" );
        pjWin.setPoints(50);
        partieGagnee.getJoueurs().add(pjWin);

        Partie partiePerdue = new Partie();
        partiePerdue.setStatus(Status.TERMINEE);
        PartieJoueur pjLose = new PartieJoueur(partiePerdue, user, "red" );
        pjLose.setPoints(20);

        Utilisateur autre = new Utilisateur();
        autre.setId(autreId);
        PartieJoueur pjAutre = new PartieJoueur(partiePerdue, autre, "blue");
        pjAutre.setPoints(50);

        partiePerdue.getJoueurs().add(pjLose);
        partiePerdue.getJoueurs().add(pjAutre);

        List<Partie> parties = Arrays.asList(partieGagnee, partiePerdue);

        when(partieRepository.findByJoueursUtilisateurIdAndStatus(userId, Status.TERMINEE)).thenReturn(parties);

        JoueurStatistique stats = statistiquesService.getStatisticsForUser(userId);

        assertEquals(2, stats.getNbPartiesJouees(), "Nombre de parties jouées");
        assertEquals(1, stats.getNbPartiesGagnees(), "Nombre de parties gagnées");
        assertEquals(0.5, stats.getRatioPartiesGagnees(), "Ratio de victoire");
    }

    @Test
    void testGetStatisticsForUser_JoueurSansUtilisateur() {
        Partie partie = new Partie();
        partie.setStatus(Status.TERMINEE);
        PartieJoueur pjWin = new PartieJoueur();
        pjWin.setPoints(42);
        partie.getJoueurs().add(pjWin);

        List<Partie> parties = List.of(partie);

        when(partieRepository.findByJoueursUtilisateurIdAndStatus(userId, Status.TERMINEE)).thenReturn(parties);

        JoueurStatistique stats = statistiquesService.getStatisticsForUser(userId);

        assertEquals(1, stats.getNbPartiesJouees());
        assertEquals(0, stats.getNbPartiesGagnees());
        assertEquals(0.0, stats.getRatioPartiesGagnees());
    }
    @Test
    void testGetStatisticsForUser_JoueurAvecAutreId() {
        Partie partie = new Partie();
        partie.setStatus(Status.TERMINEE);
        Utilisateur autre = new Utilisateur();
        autre.setId(autreId);
        PartieJoueur pjWin = new PartieJoueur();
        pjWin.setUtilisateur(autre);
        pjWin.setPoints(50);
        partie.getJoueurs().add(pjWin);

        List<Partie> parties = List.of(partie);

        when(partieRepository.findByJoueursUtilisateurIdAndStatus(userId, Status.TERMINEE)).thenReturn(parties);

        JoueurStatistique stats = statistiquesService.getStatisticsForUser(userId);

        assertEquals(1, stats.getNbPartiesJouees());
        assertEquals(0, stats.getNbPartiesGagnees());
        assertEquals(0.0, stats.getRatioPartiesGagnees());
    }

    @Test
    void testGetStatisticsForUser_PartieSansJoueur() {
        Partie partie = new Partie();
        partie.setStatus(Status.TERMINEE);

        List<Partie> parties = List.of(partie);

        when(partieRepository.findByJoueursUtilisateurIdAndStatus(userId, Status.TERMINEE)).thenReturn(parties);

        JoueurStatistique stats = statistiquesService.getStatisticsForUser(userId);

        assertEquals(1, stats.getNbPartiesJouees());
        assertEquals(0, stats.getNbPartiesGagnees());
        assertEquals(0.0, stats.getRatioPartiesGagnees());
    }

    @Test
    void filtre_match_siUtilisateurAvecBonId() {
        UUID utilisateurId = UUID.randomUUID();

        Utilisateur u = new Utilisateur();
        u.setId(utilisateurId);

        PartieJoueur pj = new PartieJoueur();
        pj.setUtilisateur(u);

        List<PartieJoueur> joueurs = List.of(pj);

        Optional<PartieJoueur> res = joueurs.stream()
                .filter(j -> j.getUtilisateur() != null && utilisateurId.equals(j.getUtilisateur().getId()))
                .findFirst();

        assertTrue(res.isPresent(), "Doit matcher si utilisateur avec le bon id");
        assertEquals(u, res.get().getUtilisateur());
    }

    @Test
    void filtre_neMatchPas_siUtilisateurAbsent() {
        UUID utilisateurId = UUID.randomUUID();

        Utilisateur autre = new Utilisateur();
        autre.setId(UUID.randomUUID());

        PartieJoueur pj = new PartieJoueur();
        pj.setUtilisateur(autre);

        List<PartieJoueur> joueurs = List.of(pj);

        Optional<PartieJoueur> res = joueurs.stream()
                .filter(j -> j.getUtilisateur() != null && utilisateurId.equals(j.getUtilisateur().getId()))
                .findFirst();

        assertTrue(res.isEmpty(), "Ne doit pas matcher si aucun utilisateur ne correspond");
    }

    @Test
    void filtre_neMatchPas_siUtilisateurIdNull() {
        UUID utilisateurId = UUID.randomUUID();

        Utilisateur u = new Utilisateur();
        u.setId(null);

        PartieJoueur pj = new PartieJoueur();
        pj.setUtilisateur(u);

        List<PartieJoueur> joueurs = List.of(pj);

        Optional<PartieJoueur> res = joueurs.stream()
                .filter(j -> j.getUtilisateur() != null && utilisateurId.equals(j.getUtilisateur().getId()))
                .findFirst();

        assertTrue(res.isEmpty(), "Ne doit pas matcher si getId() est null");
    }

    @Test
    void filtre_neMatchPas_siUtilisateurNull() {
        UUID utilisateurId = UUID.randomUUID();

        PartieJoueur pj = new PartieJoueur();
        pj.setUtilisateur(null);

        List<PartieJoueur> joueurs = List.of(pj);

        Optional<PartieJoueur> res = joueurs.stream()
                .filter(j -> j.getUtilisateur() != null && utilisateurId.equals(j.getUtilisateur().getId()))
                .findFirst();

        assertTrue(res.isEmpty(), "Ne doit pas matcher si getUtilisateur() est null");
    }

    @Test
    void filtre_matchUniquementLeBonDansPlusieurs() {
        UUID utilisateurId = UUID.randomUUID();
        UUID autreId = UUID.randomUUID();

        Utilisateur u1 = new Utilisateur(); u1.setId(utilisateurId);
        Utilisateur u2 = new Utilisateur(); u2.setId(autreId);

        PartieJoueur pj1 = new PartieJoueur(); pj1.setUtilisateur(u1);
        PartieJoueur pj2 = new PartieJoueur(); pj2.setUtilisateur(u2);

        List<PartieJoueur> joueurs = Arrays.asList(pj1, pj2);

        List<PartieJoueur> matches = joueurs.stream()
                .filter(j -> j.getUtilisateur() != null && utilisateurId.equals(j.getUtilisateur().getId()))
                .toList();

        assertEquals(1, matches.size(), "Doit matcher un seul joueur avec le bon id");
        assertEquals(u1, matches.get(0).getUtilisateur());
    }
    @Test
    void filtre_NPE_siUtilisateurIdNull() {
        UUID utilisateurId = null;

        Utilisateur u = new Utilisateur();
        u.setId(UUID.randomUUID());
        PartieJoueur pj = new PartieJoueur();
        pj.setUtilisateur(u);
        List<PartieJoueur> joueurs = List.of(pj);

        assertThrows(NullPointerException.class, () -> {
            joueurs.stream()
                    .filter(j -> j.getUtilisateur() != null && utilisateurId.equals(j.getUtilisateur().getId()))
                    .findFirst();
        });
    }
    @Test
    void filtre_NPE_siUtilisateurIdEtJoueurIdNull() {
        UUID utilisateurId = null;

        Utilisateur u = new Utilisateur();
        u.setId(null);
        PartieJoueur pj = new PartieJoueur();
        pj.setUtilisateur(u);
        List<PartieJoueur> joueurs = List.of(pj);

        assertThrows(NullPointerException.class, () -> {
            joueurs.stream()
                    .filter(j -> j.getUtilisateur() != null && utilisateurId.equals(j.getUtilisateur().getId()))
                    .findFirst();
        });
    }

    @Test
    void getStatisticsForUser_NPE_siUtilisateurIdNull() {
        Partie partie = new Partie();
        Utilisateur user = new Utilisateur();
        user.setId(UUID.randomUUID());
        PartieJoueur pj = new PartieJoueur();
        pj.setUtilisateur(user);
        partie.getJoueurs().add(pj);
        partie.setStatus(Status.TERMINEE);

        when(partieRepository.findByJoueursUtilisateurIdAndStatus(null, Status.TERMINEE))
                .thenReturn(List.of(partie));

        assertThrows(NullPointerException.class, () -> {
            statistiquesService.getStatisticsForUser(null);
        });
    }

}