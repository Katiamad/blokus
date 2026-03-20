package fr.orleans.m1miage.project.s2.projetS2.service;

import fr.orleans.m1miage.project.s2.projetS2.exeptions.NomPartieInvalideException;
import fr.orleans.m1miage.project.s2.projetS2.exeptions.PartieNonRejoignableException;
import fr.orleans.m1miage.project.s2.projetS2.exeptions.PartieNotFoundException;
import fr.orleans.m1miage.project.s2.projetS2.exeptions.UtilisateurNonTrouveException;
import fr.orleans.m1miage.project.s2.projetS2.model.*;
import fr.orleans.m1miage.project.s2.projetS2.repository.PartieRepository;
import fr.orleans.m1miage.project.s2.projetS2.repository.RobotRepository;
import fr.orleans.m1miage.project.s2.projetS2.repository.UtilisateurRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PartieService {

    private final UtilisateurRepository utilisateurRepository;
    private final RobotRepository robotRepository;
    private final PartieRepository partieRepository;
    private final ModeJeuStrategy jeuSansTimer;
    private final ModeJeuStrategy jeuAvecTimer;


    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public List<String> couleurs;
    public ModeJeuStrategy modeJeuStrategy;
    private static final List<String> COULEURS_DISPONIBLES = Arrays.asList("#FFB76E", "#8ED8B1", "#5A54E1", "#83B5F4");
    private static final String PARTIE_NON_TROUVE = "Partie non trouvée";

    public PartieService(UtilisateurRepository utilisateurRepository, RobotRepository robotRepository, PartieRepository partieRepository, JeuAvecTimer jeuAvecTimer, JeuSansTimer jeuSansTimer) {
        this.utilisateurRepository = utilisateurRepository;
        this.robotRepository = robotRepository;
        this.partieRepository = partieRepository;
        this.jeuAvecTimer = jeuAvecTimer;
        this.jeuSansTimer = jeuSansTimer;
    }

    public void seedFinishedGamesForUser(Utilisateur user, Utilisateur test) {
        Partie partieGagnee = new Partie("Seeded_Win_1");
        partieGagnee.setModeJeu(ModeJeu.SANS_TIMER);
        partieGagnee.setStatus(Status.TERMINEE);
        PartieJoueur pjWin = new PartieJoueur(partieGagnee, user, "Red");
        pjWin.setPoints(50);
        pjWin.setRang("1er");
        partieGagnee.getJoueurs().add(pjWin);
        partieRepository.save(partieGagnee);
        Partie partiePerdue = new Partie("Seeded_Loss_1");
        partiePerdue.setModeJeu(ModeJeu.AVEC_TIMER);
        partiePerdue.setStatus(Status.TERMINEE);
        PartieJoueur pjLose = new PartieJoueur(partiePerdue, user, "Green");
        pjLose.setPoints(20);
        pjLose.setRang("2e");

        partiePerdue.getJoueurs().add(pjLose);
        PartieJoueur testWin = new PartieJoueur(partiePerdue, test, "red");
        testWin.setPoints(50);
        testWin.setRang("1er");

        partiePerdue.getJoueurs().add(testWin);
        partieRepository.save(partiePerdue);
    }

    public Partie lancerPartie(UUID id, String nomPartie, String modeJeu, int time) {
        if (nomPartie == null || nomPartie.trim().isEmpty()) {
            throw new NomPartieInvalideException("Le nom de la partie ne peut pas être vide.");
        }

        if (partieRepository.findByNom(nomPartie).isPresent()) {
            throw new NomPartieInvalideException("Le nom de partie « " + nomPartie + " » est déjà utilisé.");
        }

        couleurs = new ArrayList<>(COULEURS_DISPONIBLES);

        Optional<Utilisateur> opt = utilisateurRepository.findById(id);
        if (opt.isEmpty()) throw new UtilisateurNonTrouveException("Utilisateur non trouvé.");
        Utilisateur user = opt.get();

        Partie partie = new Partie();
        partie.setNom(nomPartie);
        partie.setModeJeu(ModeJeu.valueOf(modeJeu));
        partie.setTempsMin(time);
        partie.setStatus(Status.EN_ATTENTE);
        partie.setTour(1);
        partie.setCreateurId(id);

        partie.ajouterJoueur(user, COULEURS_DISPONIBLES.get(0));
        couleurs.remove(0);

        partieRepository.save(partie);
        for (int i = 1; i <= 3; i++) {
            rejoindrePartieRobot(partie.getId(), "Robot " + i, i + 1);
        }
        return partie;
    }

    public boolean estDansLaPartie(UUID idPartie, UUID idUtilisateur) {
        Partie partie = partieRepository.findById(idPartie)
                .orElseThrow(() -> new PartieNotFoundException(PARTIE_NON_TROUVE));

        for (PartieJoueur pj : partie.getJoueurs()) {
            if (pj.getUtilisateur().getId().equals(idUtilisateur)) {
                return true;
            }
        }
        return false;
    }


    public Partie rejoindrePartie(UUID idPartie, UUID idJoueur) {
        Partie partie = partieRepository.findById(idPartie)
                .orElseThrow(() -> new RuntimeException(PARTIE_NON_TROUVE));

        if (partie.getStatus() != Status.EN_ATTENTE) {
            throw new PartieNonRejoignableException("On ne peut pas rejoindre une partie déjà commencée ou terminée.");
        }

        Utilisateur user = utilisateurRepository.findById(idJoueur)
                .orElseThrow(() -> new RuntimeException(PARTIE_NON_TROUVE));

        if (estDansLaPartie(idPartie, idJoueur)) {
            return partie;
        }

        if (partie.getJoueurs().size() + partie.getRobots().size() >= 4) {
            if (!partie.getRobots().isEmpty()) {
                PartieRobot robotToRemove = partie.getRobots().stream().findFirst().orElseThrow();

                String couleur = robotToRemove.getCouleur();

                partie.getRobots().remove(robotToRemove);
                robotRepository.delete(robotToRemove);

                partie.ajouterJoueur(user, couleur);
            } else {
                throw new PartieNonRejoignableException("La partie est déjà pleine");
            }
        } else {
            String couleurAttribuee = couleurs.get(0);
            couleurs.remove(0);
            partie.ajouterJoueur(user, couleurAttribuee);
        }

        partieRepository.save(partie);
        return partie;
    }

    public Partie findById(UUID id) {
        return partieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(PARTIE_NON_TROUVE));
    }

    public Partie demarrerPartie(UUID idPartie) {
        Partie partie = partieRepository.findById(idPartie)
                .orElseThrow(() -> new RuntimeException(PARTIE_NON_TROUVE));
        int i = 1;
        for (PartieJoueur pj : partie.getJoueurs()) {
            pj.setTour(i);
            pj.initialiserPositionsPremierTour();
            i++;
        }

        for (PartieRobot robot : partie.getRobots()) {
            robot.setTour(i);
            robot.initialiserPositionsPremierTour();
            i++;
        }
        if (partie.getModeJeu().equals(ModeJeu.SANS_TIMER)) {
            modeJeuStrategy = jeuSansTimer;

        } else {
            modeJeuStrategy = jeuAvecTimer;
        }
        modeJeuStrategy.startGame(partie);
        partie.setStatus(Status.EN_COURS);
        partieRepository.save(partie);
        return partie;
    }

    public Partie validerJeu(UUID idPartie, List<int[]> positionOccupPiece, int idPiece) {
        Partie partie = partieRepository.findById(idPartie)
                .orElseThrow(() -> new PartieNotFoundException(PARTIE_NON_TROUVE));
        PartieJoueur pj = partie.getJoueurs().get(partie.getTour() - 1);

        Piece piece = pj.getPieces().stream()
                .filter(p -> p.getId() == idPiece)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Pièce introuvable pour idPiece=" + idPiece));

        boolean valide = peutPlacer(partie.getGrille(), positionOccupPiece, partie.getTour());

        if (valide) {
            int[][] grille = partie.getGrille();
            for (int[] paire : positionOccupPiece) {
                int x = paire[0];
                int y = paire[1];
                grille[x][y] = partie.getTour();
            }

            mettreAJourPositionsValides(partie, partie.getTour());

            pj.getPieces().remove(piece);
            pj.ajouterPointsPiece(piece);
            pj.setLastPieceSize(piece.getSize());

            int nbhumains = partie.getJoueurs().size();
            int nbrobots = partie.getRobots().size();

            if (partie.getTour() == nbhumains + nbrobots) {
                partie.setTour(1);
            } else {
                partie.setTour(partie.getTour() + 1);
            }

            partie.setGrille(grille);
            checkAndFinishIfNoOneCanPlay(partie);
            partieRepository.save(partie);
        } else {
            throw new IllegalArgumentException("Votre placement n'est pas valide.");
        }
        return partie;
    }

    public boolean peutPlacer(int[][] grille, List<int[]> positions, int tour) {
        if (!positionsAreWithinBoundsAndEmpty(grille, positions)) {
            return false;
        }
        if (touchesOwnSide(grille, positions, tour)) {
            return false;
        }
        if (estPremierTour(grille, tour)) {
            return coversStartingCorner(positions);
        }
        return touchesOwnCorner(grille, positions, tour);
    }

    private boolean positionsAreWithinBoundsAndEmpty(int[][] grille, List<int[]> positions) {
        for (int[] pos : positions) {
            int x = pos[0];
            int y = pos[1];
            if (x < 0 || x >= 20 || y < 0 || y >= 20 || grille[x][y] != 0) {
                return false;
            }
        }
        return true;
    }

    private boolean touchesOwnSide(int[][] grille, List<int[]> positions, int tour) {
        for (int[] pos : positions) {
            int x = pos[0];
            int y = pos[1];
            int[][] sides = {{x - 1, y}, {x + 1, y}, {x, y - 1}, {x, y + 1}};
            for (int[] side : sides) {
                int sx = side[0];
                int sy = side[1];
                if (sx >= 0 && sx < 20 && sy >= 0 && sy < 20 && grille[sx][sy] == tour) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean coversStartingCorner(List<int[]> positions) {
        for (int[] pos : positions) {
            int x = pos[0];
            int y = pos[1];
            if ((x == 0 && y == 0) || (x == 0 && y == 19) ||
                    (x == 19 && y == 0) || (x == 19 && y == 19)) {
                return true;
            }
        }
        return false;
    }

    private boolean touchesOwnCorner(int[][] grille, List<int[]> positions, int tour) {
        List<int[]> coinsDisponibles = getCoinsDisponibles(grille, tour);
        Set<String> coinsSet = new HashSet<>();
        for (int[] coin : coinsDisponibles) {
            coinsSet.add(coin[0] + "," + coin[1]);
        }
        for (int[] pos : positions) {
            if (coinsSet.contains(pos[0] + "," + pos[1])) {
                return true;
            }
        }
        return false;
    }

    private boolean estPremierTour(int[][] grille, int tour) {
        for (int[] row : grille) {
            for (int cell : row) {
                if (cell == tour) {
                    return false;
                }
            }
        }
        return true;
    }

    public void rejoindrePartieRobot(UUID idPartie, String nom, int tour) {
        Partie partie = partieRepository.findById(idPartie)
                .orElseThrow(() -> new PartieNotFoundException(PARTIE_NON_TROUVE));
        PartieRobot robot = new PartieRobot();
        robot.setPartie(partie);
        robot.setNom(nom);
        robot.setTour(tour);
        robot.setCouleur(couleurs.get(0));
        couleurs.remove(0);
        robotRepository.save(robot);
        partie.getRobots().add(robot);
        partieRepository.save(partie);
    }

    public boolean nextIsHuman(Partie partie) {
        int nbhumains = partie.getJoueurs().size();
        return partie.getTour() <= nbhumains;
    }

    public int[][] rotationnerPiece(int[][] piece, int rotations) {
        int[][] result = piece;
        for (int i = 0; i < rotations; i++) {
            result = rotationner90Degres(result);
        }
        return result;
    }

    public int[][] rotationner90Degres(int[][] piece) {
        int rows = piece.length;
        int cols = piece[0].length;
        int[][] rotated = new int[cols][rows];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                rotated[j][rows - 1 - i] = piece[i][j];
            }
        }

        return rotated;
    }

    public int[][] symetriePiece(int[][] piece) {
        int rows = piece.length;
        int cols = piece[0].length;
        int[][] mirrored = new int[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                mirrored[i][cols - 1 - j] = piece[i][j];
            }
        }

        return mirrored;
    }

    public List<int[]> calculerPositionsPiece(int[][] forme, int x, int y) {
        List<int[]> positions = new ArrayList<>();

        if (x + forme.length > 20 || y + forme[0].length > 20) {
            return Collections.emptyList();
        }

        for (int i = 0; i < forme.length; i++) {
            for (int j = 0; j < forme[i].length; j++) {
                if (forme[i][j] == 1) {
                    positions.add(new int[]{x + i, y + j});
                }
            }
        }
        return positions;
    }

    public List<String> getCouleurs() {
        return new ArrayList<>(COULEURS_DISPONIBLES);
    }

    public void mettreAJourPositionsValides(Partie partie, int tourJoueur) {
        PartieJoueur joueur = partie.getJoueurs().stream()
                .filter(j -> j.getTour() == tourJoueur)
                .findFirst()
                .orElse(null);

        PartieRobot robot = partie.getRobots().stream()
                .filter(r -> r.getTour() == tourJoueur)
                .findFirst()
                .orElse(null);

        List<int[]> nouvellesPositions = getCoinsDisponibles(partie.getGrille(), tourJoueur);

        if (joueur != null) {
            joueur.setPositionsValidesPourCoin(nouvellesPositions);
        } else if (robot != null) {
            robot.setPositionsValidesPourCoin(nouvellesPositions);
        }
    }

    public void jouerRobot(PartieRobot robot, Partie partie) {
        int[][] grille = partie.getGrille();
        List<Piece> piecesDisponibles = robot.getPieces();
        int tourRobot = robot.getTour();

        List<RobotMove> coupsPossibles = new ArrayList<>();

        for (Piece piece : piecesDisponibles) {
            int[][] forme = piece.getForme();
            for (int rotation = 0; rotation < 4; rotation++) {
                int[][] formeRotee = rotationnerPiece(forme, rotation);
                coupsPossibles.addAll(getValidMoves(formeRotee, piece, grille, robot, partie, tourRobot));
                int[][] formeMirrored = symetriePiece(formeRotee);
                coupsPossibles.addAll(getValidMoves(formeMirrored, piece, grille, robot, partie, tourRobot));
            }
        }

        if (!coupsPossibles.isEmpty()) {
            RobotMove move = coupsPossibles.get(new Random().nextInt(coupsPossibles.size()));
            placerPieceEtFinirTour(move.positions, move.piece, grille, robot, partie, tourRobot);
            checkAndFinishIfNoOneCanPlay(partie);
            return;
        }

        avancerTour(partie);
        partieRepository.save(partie);
        checkAndFinishIfNoOneCanPlay(partie);
    }


    public static class RobotMove {
        public List<int[]> positions;
        public Piece piece;
        public RobotMove(List<int[]> positions, Piece piece) {
            this.positions = positions;
            this.piece = piece;
        }
    }

    public List<RobotMove> getValidMoves(int[][] forme, Piece piece, int[][] grille, PartieRobot robot, Partie partie, int tourRobot) {
        List<RobotMove> moves = new ArrayList<>();
        for (int x = 0; x < grille.length; x++) {
            for (int y = 0; y < grille[0].length; y++) {
                List<int[]> positions = calculerPositionsPiece(forme, x, y);
                if (peutPlacer(grille, positions, tourRobot)) {
                    moves.add(new RobotMove(positions, piece));
                }
            }
        }
        return moves;
    }


    public boolean tryAllTransformationsAndPlace(Piece piece, int[][] grille, PartieRobot robot, Partie partie, int tourRobot) {
        int[][] forme = piece.getForme();
        for (int rotation = 0; rotation < 4; rotation++) {
            int[][] formeRotee = rotationnerPiece(forme, rotation);
            if (tryPlacingWithAllPositions(formeRotee, piece, grille, robot, partie, tourRobot)) return true;
            int[][] formeMirrored = symetriePiece(formeRotee);
            if (tryPlacingWithAllPositions(formeMirrored, piece, grille, robot, partie, tourRobot)) return true;
        }
        return false;
    }

    public boolean tryPlacingWithAllPositions(int[][] forme, Piece piece, int[][] grille, PartieRobot robot, Partie partie, int tourRobot) {
        for (int x = 0; x < grille.length; x++) {
            for (int y = 0; y < grille[0].length; y++) {
                List<int[]> positions = calculerPositionsPiece(forme, x, y);
                if (peutPlacer(grille, positions, tourRobot)) {
                    placerPieceEtFinirTour(positions, piece, grille, robot, partie, tourRobot);
                    return true;
                }
            }
        }
        return false;
    }

    public void placerPieceEtFinirTour(List<int[]> positions, Piece piece, int[][] grille, PartieRobot robot, Partie partie, int tourRobot) {
        for (int[] pos : positions) {
            grille[pos[0]][pos[1]] = tourRobot;
        }

        mettreAJourPositionsValides(partie, robot.getTour());
        robot.setLastPieceSize(piece.getSize());
        robot.ajouterPointsPiece(piece);

        robot.getPieces().remove(piece);

        partie.setGrille(grille);

        avancerTour(partie);
        partieRepository.save(partie);
    }


    public void avancerTour(Partie partie) {
        int nbhumains = partie.getJoueurs().size();
        int nbrobots = partie.getRobots().size();

        if (partie.getTour() == nbhumains + nbrobots) {
            partie.setTour(1);
        } else {
            partie.setTour(partie.getTour() + 1);
        }
    }

    public List<int[]> getCoinsDisponibles(int[][] grille, int tour) {
        if (estPremierTour(grille, tour)) {
            return getCoinsInitiauxDisponibles(grille);
        }

        Set<String> seen = new HashSet<>();
        List<int[]> coinsDisponibles = new ArrayList<>();

        for (int x = 0; x < grille.length; x++) {
            for (int y = 0; y < grille[0].length; y++) {
                if (grille[x][y] == tour) {
                    for (int[] coin : getDiagonales(x, y)) {
                        String key = coin[0] + "," + coin[1];
                        if (estCoinValide(grille, coin, tour) && seen.add(key)) {
                            coinsDisponibles.add(coin);
                        }
                    }
                }
            }
        }
        return coinsDisponibles;
    }

    public List<int[]> getCoinsInitiauxDisponibles(int[][] grille) {
        int[][] coinsInitiaux = {{0, 0}, {0, 19}, {19, 0}, {19, 19}};
        List<int[]> coinsDisponibles = new ArrayList<>();
        for (int[] coin : coinsInitiaux) {
            if (grille[coin[0]][coin[1]] == 0) {
                coinsDisponibles.add(coin);
            }
        }
        return coinsDisponibles;
    }

    public int[][] getDiagonales(int x, int y) {
        return new int[][]{
                {x - 1, y - 1},
                {x - 1, y + 1},
                {x + 1, y - 1},
                {x + 1, y + 1}
        };
    }

    public boolean estCoinValide(int[][] grille, int[] coin, int tour) {
        int x = coin[0];
        int y = coin[1];

        if (x < 0 || x >= 20 || y < 0 || y >= 20 || grille[x][y] != 0) {
            return false;
        }

        int[][] cotes = {
                {x - 1, y}, {x + 1, y},
                {x, y - 1}, {x, y + 1}
        };

        for (int[] cote : cotes) {
            int cx = cote[0];
            int cy = cote[1];
            if (cx >= 0 && cx < 20 && cy >= 0 && cy < 20 && grille[cx][cy] == tour) {
                return false;
            }

        }

        return true;
    }

    public List<int[]> getCoinsDisponiblesPourJoueur(UUID idPartie, UUID idJoueur) {
        Partie partie = partieRepository.findById(idPartie)
                .orElseThrow(() -> new RuntimeException(PARTIE_NON_TROUVE));

        PartieJoueur joueur = partie.getJoueurs().stream()
                .filter(j -> j.getUtilisateur().getId() == idJoueur)
                .findFirst()
                .orElse(null);

        if (joueur != null) {
            return getCoinsDisponibles(partie.getGrille(), joueur.getTour());
        }
        return new ArrayList<>();
    }

    public List<Partie> getPartiesParEtatPourUtilisateur(UUID utilisateurId, Status status) {
        return partieRepository.findByJoueursUtilisateurIdAndStatus(utilisateurId, status);
    }

    public Map<Status, List<Partie>> getPartiesGroupByEtat(UUID utilisateurId) {
        Map<Status, List<Partie>> map = new EnumMap<>(Status.class);
        for (Status s : Status.values()) {
            map.put(s, getPartiesParEtatPourUtilisateur(utilisateurId, s));
        }
        return map;
    }

    public Partie finDePartie(UUID idPartie) {
        modeJeuStrategy.stopGame(idPartie);
        Partie partie = partieRepository.findById(idPartie)
                .orElseThrow(() -> new PartieNotFoundException(PARTIE_NON_TROUVE));
        partie.trouverGagnant();

        List<Player> allPlayers = new ArrayList<>();
        allPlayers.addAll(partie.getJoueurs());
        allPlayers.addAll(partie.getRobots());
        attribuerRangs(allPlayers);

        partieRepository.save(partie);
        return partie;
    }

    public boolean auMoinsUnPeutJouer(UUID idPartie) {
        Partie partie = partieRepository.findById(idPartie)
                .orElseThrow(() -> new PartieNotFoundException(PARTIE_NON_TROUVE));

        for (PartieJoueur joueur : partie.getJoueurs()) {
            if (peutEncoreJouer(partie, joueur.getTour(), joueur.getPieces())) {
                return true;
            }
        }
        for (PartieRobot robot : partie.getRobots()) {
            if (peutEncoreJouer(partie, robot.getTour(), robot.getPieces())) {
                return true;
            }
        }

        return false;
    }

    public boolean peutEncoreJouer(Partie partie, int tour, List<Piece> piecesPossedees) {
        int[][] grille = partie.getGrille();
        List<int[]> coins = getCoinsDisponibles(grille, tour);

        for (Piece piece : piecesPossedees) {
            int[][] forme = piece.getForme();

            for (int rotation = 0; rotation < 4; rotation++) {
                int[][] pieceRot = rotationnerPiece(forme, rotation);

                for (int miroir = 0; miroir < 2; miroir++) {
                    int[][] pieceFinale = (miroir == 1) ? symetriePiece(pieceRot) : pieceRot;

                    for (int[] coin : coins) {
                        int maxX = 20 - pieceFinale.length;
                        int maxY = 20 - pieceFinale[0].length;
                        for (int x = 0; x <= maxX; x++) {
                            for (int y = 0; y <= maxY; y++) {
                                List<int[]> positions = calculerPositionsPiece(pieceFinale, x, y);
                                if (positions == null) continue;

                                boolean couvreCoin = false;
                                for (int[] pos : positions) {
                                    if (pos[0] == coin[0] && pos[1] == coin[1]) {
                                        couvreCoin = true;
                                        break;
                                    }
                                }
                                if (!couvreCoin) continue;

                                if (peutPlacer(grille, positions, tour)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public void checkAndFinishIfNoOneCanPlay(Partie partie) {
        if (partie.getStatus() == Status.TERMINEE) {
            return;
        }
        if (!auMoinsUnPeutJouer(partie.getId())) {
            partie.setStatus(Status.TERMINEE);
            finDePartie(partie.getId());
        }
    }


    public static void attribuerRangs(List<? extends Player> tousLesJoueurs) {
        if (tousLesJoueurs == null) {
            throw new IllegalArgumentException("La liste des joueurs ne doit pas être nulle !");
        }
        tousLesJoueurs.sort(Comparator.comparingInt(Player::getPoints).reversed());

        int rangActuel = 1;
        int compteur = 1;
        Integer scorePrecedent = null;
        for (Player joueur : tousLesJoueurs) {
            if (scorePrecedent != null && joueur.getPoints() < scorePrecedent) {
                rangActuel = compteur;
            }
            joueur.setRang(getSuffixRang(rangActuel));
            scorePrecedent = joueur.getPoints();
            compteur++;
        }
    }

    @Transactional
    public void terminerPartieAutomatiquement(UUID idPartie) {
        Partie partie = partieRepository.findById(idPartie)
                .orElseThrow(() -> new PartieNotFoundException("Partie non trouvée"));

        partie.getJoueurs().size();
        partie.getRobots().size();

        if (partie.getStatus() != Status.TERMINEE) {
            partie.setStatus(Status.TERMINEE);

            modeJeuStrategy.stopGame(partie.getId());

            partie.trouverGagnant();

            List<Player> allPlayers = new ArrayList<>();
            allPlayers.addAll(partie.getJoueurs());
            allPlayers.addAll(partie.getRobots());
            attribuerRangs(allPlayers);

            partieRepository.save(partie);
            Map<String, Object> endMsg = new HashMap<>();
            endMsg.put("fin", true);
            endMsg.put("partieId", partie.getId());
            messagingTemplate.convertAndSend("/topic/fin-partie/" + partie.getId(), endMsg);
        }
    }


    public static String getSuffixRang(int rang) {
        if (rang == 1) return "1er";
        return rang + "e";
    }

}

