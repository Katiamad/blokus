package fr.orleans.m1miage.project.s2.projetS2.controller;

import fr.orleans.m1miage.project.s2.projetS2.exeptions.NomPartieInvalideException;
import fr.orleans.m1miage.project.s2.projetS2.exeptions.PartieNonRejoignableException;
import fr.orleans.m1miage.project.s2.projetS2.model.*;
import fr.orleans.m1miage.project.s2.projetS2.repository.PartieRepository;
import fr.orleans.m1miage.project.s2.projetS2.repository.RobotRepository;
import fr.orleans.m1miage.project.s2.projetS2.repository.UtilisateurRepository;
import fr.orleans.m1miage.project.s2.projetS2.service.PartieService;
import fr.orleans.m1miage.project.s2.projetS2.service.StatistiquesService;
import fr.orleans.m1miage.project.s2.projetS2.service.UtilisateurService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/blokus")
public class JeuControleur {

    @Autowired
    public SimpMessagingTemplate messagingTemplate;

    private final UtilisateurService utilisateurService;
    private final StatistiquesService statistiquesService;
    private final PartieService partieService;

    public JeuControleur(UtilisateurService utilisateurService, StatistiquesService statistiquesService, PartieService partieService) {
        this.utilisateurService = utilisateurService;
        this.statistiquesService = statistiquesService;
        this.partieService = partieService;
    }

    @PostMapping("/start/{idPartie}")
    public String DemarrerLaPartie(Model model, @PathVariable("idPartie") UUID idPartie, @AuthenticationPrincipal UserDetails userDetails) {

        String username = userDetails.getUsername();
        Utilisateur user = utilisateurService.findUtilisateurByUsername(username);
        Partie partiedemarree = partieService.demarrerPartie(idPartie);
        model.addAttribute("partie", partiedemarree);
        model.addAttribute("utilisateur", user);

        sendStatusUpdate(partiedemarree.getId(),partiedemarree);

        return "redirect:/blokus/partie-en-cours/" + partiedemarree.getId();
    }

    @GetMapping("/partie-en-cours/{idPartie}")
    public String nouvellePartie(Model model, @PathVariable("idPartie") UUID idPartie, @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        Utilisateur user = utilisateurService.findUtilisateurByUsername(username);
        Partie partiedemarree = partieService.findById(idPartie);

        if (partiedemarree.getStatus().equals(Status.TERMINEE)) {
            return "redirect:/blokus/fin/" + partiedemarree.getId();
        }

        PartieJoueur joueurConnecte = null;
        for (PartieJoueur joueur : partiedemarree.getJoueurs()) {
            if (joueur.getUtilisateur().getId() == user.getId()) {
                joueurConnecte = joueur;
                break;
            }
        }

        if (joueurConnecte == null) {
            throw new RuntimeException("Joueur non trouvé dans la partie");
        }

        String couleurJoueurConnecte = joueurConnecte.getCouleur();

        List<Piece> piecesJoueur = joueurConnecte.getPieces();

        List<int[]> coinsDisponibles = partieService.getCoinsDisponiblesPourJoueur(idPartie, user.getId());

        model.addAttribute("pieces", piecesJoueur);
        model.addAttribute("couleurJoueurConnecte", couleurJoueurConnecte);
        model.addAttribute("coinsDisponibles", coinsDisponibles);
        model.addAttribute("partie", partiedemarree);
        model.addAttribute("utilisateur", user);

        List<String> couleursOrdonnees = new ArrayList<>();

        partiedemarree.getJoueurs().stream()
                .sorted(Comparator.comparingInt(PartieJoueur::getTour))
                .forEach(j -> couleursOrdonnees.add(j.getCouleur()));

        partiedemarree.getRobots().stream()
                .sorted(Comparator.comparingInt(PartieRobot::getTour))
                .forEach(r -> couleursOrdonnees.add(r.getCouleur()));

        model.addAttribute("couleurs", couleursOrdonnees);
        if (partiedemarree.getModeJeu() == ModeJeu.AVEC_TIMER) {
            java.util.Date dateStart = Date.from(partiedemarree.getStartTime().atZone(ZoneId.systemDefault()).toInstant());
            model.addAttribute("startTime", dateStart);
            model.addAttribute("timer", partiedemarree.getTempsMin());
        }

        return "partie-de-jeu";
    }

    @PostMapping("/partie")
    public String lancerPartie(
            @RequestParam("nomPartie") String nomPartie,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("modeJeu") String modeJeu,
            @RequestParam("time") int time,
            Model model) {

        Utilisateur user = utilisateurService.findUtilisateurByUsername(userDetails.getUsername());
        model.addAttribute("utilisateur", user);

        try {
            Partie partie = partieService.lancerPartie(user.getId(), nomPartie, modeJeu, time);
            sendGridUpdate(partie.getId(), partie.getGrille(), partie);
            return "redirect:/blokus/partie/" + partie.getId();
        } catch (NomPartieInvalideException | IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("nomPartie", nomPartie);
            return "home";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/partie/{idPartie}")
    public String rejoindreOuLancerPartie(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model,
            @PathVariable("idPartie") UUID idPartie,
            @RequestParam(value = "errorJoin", required = false) String errorJoin
    ) {
        Utilisateur user = utilisateurService.findUtilisateurByUsername(userDetails.getUsername());
        model.addAttribute("utilisateur", user);

        Partie partie;

        if (partieService.estDansLaPartie(idPartie, user.getId())) {
            partie = partieService.findById(idPartie);
        } else {
            partie = partieService.rejoindrePartie(idPartie, user.getId());
            sendJoueurUpdate(partie.getId(), partie);
        }

        model.addAttribute("partie", partie);
        model.addAttribute("joueurs", partie.getJoueurs());
        model.addAttribute("robots", partie.getRobots());

        return "attente";
    }

    @PostMapping("/tour-partie")
    public String jouerPiece(
            @RequestParam("idPartie") UUID idPartie,
            @RequestParam("idPiece")  int idPiece,
            Model model,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {

        Utilisateur user = utilisateurService.findUtilisateurByUsername(userDetails.getUsername());
        model.addAttribute("utilisateur", user);

        String[] rawPositions = request.getParameterValues("positionOccupee");
        if (rawPositions == null || rawPositions.length == 0) {
            model.addAttribute("error", "Aucune position reçue");
            return "partie-de-jeu";
        }

        List<int[]> positions = new ArrayList<>();
        for (String raw : rawPositions) {
            String[] coords = raw.split(",");
            if (coords.length != 2) {
                model.addAttribute("error", "Coordonnées mal formées : " + raw);
                return "partie-de-jeu";
            }
            try {
                int x = Integer.parseInt(coords[0]);
                int y = Integer.parseInt(coords[1]);
                positions.add(new int[]{x, y});
            } catch (NumberFormatException ex) {
                model.addAttribute("error", "Coordonnée non numérique : " + raw);
                return "partie-de-jeu";
            }
        }

        Partie partie = partieService.validerJeu(idPartie, positions, idPiece);
        model.addAttribute("partie", partie);

        partieService.checkAndFinishIfNoOneCanPlay(partie);

        if (partie.getStatus().equals(Status.TERMINEE)) {
            return "redirect:/blokus/fin/" + partie.getId();
        } else {
            model.addAttribute("couleurs", partieService.getCouleurs());
            sendGridUpdate(idPartie, partie.getGrille(), partie);

            if (partieService.nextIsHuman(partie)) {
                return "redirect:/blokus/partie-en-cours/" + partie.getId();
            }

            for (PartieRobot r : new ArrayList<>(partie.getRobots())) {
                partieService.jouerRobot(r, partie);
                sendGridUpdate(idPartie, partie.getGrille(), partie);
            }
            return "redirect:/blokus/partie-en-cours/" + partie.getId();
        }
    }

    @GetMapping("/fin/{idPartie}")
    public String afficherFinPartie(
            @PathVariable("idPartie") UUID idPartie,
            Model model,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Partie partie = partieService.findById(idPartie);
        model.addAttribute("partie", partie);
        model.addAttribute("joueurs", partie.getJoueurs());

        Utilisateur user = utilisateurService.findUtilisateurByUsername(userDetails.getUsername());
        model.addAttribute("utilisateur", user);

        List<Player> gagnants = new ArrayList<>();

        for (UUID gagnantId : partie.getGagnants()) {
            partie.getJoueurs().stream()
                    .filter(j -> j.getId().equals(gagnantId))
                    .findFirst()
                    .ifPresent(gagnants::add);

            partie.getRobots().stream()
                    .filter(r -> r.getId().equals(gagnantId))
                    .findFirst()
                    .ifPresent(gagnants::add);
        }

        List<String> nomsGagnants = new ArrayList<>();
        for (Player gagnant : gagnants) {
            if (gagnant instanceof PartieJoueur) {
                nomsGagnants.add(((PartieJoueur) gagnant).getUtilisateur().getNom());
            } else if (gagnant instanceof PartieRobot) {
                nomsGagnants.add(gagnant.getNom());
            }
        }
        model.addAttribute("nomsGagnants", nomsGagnants);

        int gagnantPoints = gagnants.isEmpty() ? 0 : gagnants.get(0).getPoints();
        model.addAttribute("gagnantPoints", gagnantPoints);

        return "fin-de-partie";
    }

    @PostMapping(value = "/tour-partie-ajax", produces = "application/json")
    @ResponseBody
    public Map<String, Object> jouerPieceAjax(
            @RequestParam("idPartie") UUID idPartie,
            @RequestParam("idPiece") int idPiece,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request) {

        Map<String, Object> result = new HashMap<>();
        Utilisateur user = utilisateurService.findUtilisateurByUsername(userDetails.getUsername());

        String[] rawPositions = request.getParameterValues("positionOccupee");
        if (rawPositions == null || rawPositions.length == 0) {
            result.put("error", "Aucune position reçue");
            return result;
        }

        List<int[]> positions = new ArrayList<>();
        for (String raw : rawPositions) {
            String[] coords = raw.split(",");
            if (coords.length != 2) {
                result.put("error", "Coordonnées mal formées : " + raw);
                return result;
            }
            try {
                int x = Integer.parseInt(coords[0]);
                int y = Integer.parseInt(coords[1]);
                positions.add(new int[]{x, y});
            } catch (NumberFormatException ex) {
                result.put("error", "Coordonnée non numérique : " + raw);
                return result;
            }
        }

        try {
            Partie partie = partieService.validerJeu(idPartie, positions, idPiece);

            if (partie.getStatus().equals(Status.TERMINEE)) {
                result.put("fin", true);
                result.put("partieId", partie.getId());
                sendFinUpdate(partie);
                return result;
            }

            sendGridUpdate(idPartie, partie.getGrille(), partie);

            if (!partieService.nextIsHuman(partie)) {
                for (PartieRobot r : new ArrayList<>(partie.getRobots())) {
                    partieService.jouerRobot(r, partie);
                    sendGridUpdate(idPartie, partie.getGrille(), partie);
                }
                sendGridUpdate(idPartie, partie.getGrille(), partie);
            }

            result.put("success", true);
            return result;
        } catch (IllegalArgumentException ex) {
            result.put("error", "Placement invalide");
            return result;
        }
    }

    @GetMapping("/historique")
    public String mesPartiesParEtat(Model model,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        Utilisateur user = utilisateurService.findUtilisateurByUsername(userDetails.getUsername());
        model.addAttribute("utilisateur", user);

        Map<Status,List<Partie>> partiesParEtat =
                partieService.getPartiesGroupByEtat(user.getId());

        List<Partie> enAttente   = partiesParEtat.get(Status.EN_ATTENTE);
        List<Partie> enCours     = partiesParEtat.get(Status.EN_COURS);
        List<Partie> terminees   = partiesParEtat.get(Status.TERMINEE);

        model.addAttribute("enAttente", enAttente);
        model.addAttribute("enCours",   enCours);
        model.addAttribute("terminees", terminees);

        Map<UUID, String> rangMap = new HashMap<>();
        for (Partie p : terminees) {
            String rang = null;
            for (PartieJoueur pj : p.getJoueurs()) {
                if (pj.getUtilisateur().getId().equals(user.getId())) {
                    rang = pj.getRang();
                    break;
                }
            }
            rangMap.put(p.getId(), rang);
        }
        model.addAttribute("rangMap", rangMap);
        return "parties";
    }


    public void sendGridUpdate(UUID partieId, int[][] grid, Partie partie) {
        Map<String, Object> update = new HashMap<>();
        update.put("grid", grid);
        update.put("tour", partie.getTour());
        update.put("reload", true);
        messagingTemplate.convertAndSend("/topic/grid-updates/" + partieId, update);
    }

    public void sendFinUpdate(Partie partie) {
        Map<String, Object> endMsg = new HashMap<>();
        endMsg.put("fin", true);
        endMsg.put("partieId", partie.getId());
        messagingTemplate.convertAndSend("/topic/fin-partie/" + partie.getId(), endMsg);
    }


    public void sendStatusUpdate(UUID partieId, Partie partie) {
        Map<String,Object> statusUpdate = new HashMap<>();
        statusUpdate.put("status", partie.getStatus().name());
        statusUpdate.put("reload", true);
        messagingTemplate.convertAndSend(
                "/topic/status-updates/" + partie.getId(),
                statusUpdate
        );
    }

    public void sendJoueurUpdate(UUID partieId, Partie partie) {
        List<JoueurAttenteDto> dto = partie.getJoueurs().stream()
                .map(j -> new JoueurAttenteDto(
                        j.getUtilisateur().getNom(),
                        j.getCouleur()
                ))
                .toList();

        Map<String, Object> payload = Map.of(
                "joueurs", dto,
                "reload", true
        );
        messagingTemplate.convertAndSend(
                "/topic/attente-updates/" + partieId,
                payload
        );
    }

    public void rejoindrePartieUpdate(UUID partieId, Partie partie) {
        Map<String, Object> update = new HashMap<>();
        update.put("partie", partie);
        update.put("reload", true);
        messagingTemplate.convertAndSend("/topic/status-updates/" + partieId, update);
    }

    @GetMapping("/statistique")
    public String afficherHomePage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Utilisateur user = utilisateurService.findUtilisateurByUsername(userDetails.getUsername());
        model.addAttribute("utilisateur", user);

        JoueurStatistique stats = statistiquesService.getStatisticsForUser(user.getId());
        model.addAttribute("stats", stats);

        return "statistiques";
    }

}
record JoueurAttenteDto(String nom, String couleur) {}