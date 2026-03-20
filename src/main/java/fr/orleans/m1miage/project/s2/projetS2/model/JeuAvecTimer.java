package fr.orleans.m1miage.project.s2.projetS2.model;

import fr.orleans.m1miage.project.s2.projetS2.exeptions.PartieNotFoundException;
import fr.orleans.m1miage.project.s2.projetS2.repository.PartieRepository;
import fr.orleans.m1miage.project.s2.projetS2.service.PartieService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
@Component
public class JeuAvecTimer implements ModeJeuStrategy {

    private ScheduledExecutorService scheduler;

    @Autowired
    private PartieRepository partieRepository;

    @Autowired
    @Lazy
    private PartieService partieService;

    @PostConstruct
    public void init() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }


    public Partie startGame(Partie partie) {
        partie.setStartTime(LocalDateTime.now());
        Partie savedPartie = partieRepository.save(partie);

        UUID idPartie = savedPartie.getId();
        Partie partieComplet = partieRepository.findById(idPartie)
                .orElseThrow(() -> new PartieNotFoundException("Partie non trouvée"));
        partieComplet.getJoueurs().size();
        partieComplet.getRobots().size();

        ScheduledFuture<?> scheduledFuture = scheduler.schedule(() -> {
            try {
                partieService.terminerPartieAutomatiquement(idPartie);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, partie.getTempsMin(), TimeUnit.MINUTES);

        return savedPartie;
    }

    @Override
    public void stopGame(UUID idPartie) {
        try {

            Partie partie = partieRepository.findById(idPartie)
                    .orElseThrow(() -> new PartieNotFoundException("Partie non trouvée"));

            partie.getJoueurs().forEach(j -> {
                if (j.getPieces() != null && j.getPieces().isEmpty()) {
                    j.setPoints(j.getPoints() + 15);
                }
                if (j.getLastPieceSize() == 1) {
                    j.setPoints(j.getPoints() + 5);
                }
            });

            partie.getRobots().forEach(r -> {
                if (r.getPieces() != null && r.getPieces().isEmpty()) {
                    r.setPoints(r.getPoints() + 15);
                }
                if (r.getLastPieceSize() == 1) {
                    r.setPoints(r.getPoints() + 5);
                }
            });

            partieRepository.save(partie);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
