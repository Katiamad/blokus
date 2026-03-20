package fr.orleans.m1miage.project.s2.projetS2.model;

import fr.orleans.m1miage.project.s2.projetS2.exeptions.PartieNotFoundException;
import fr.orleans.m1miage.project.s2.projetS2.repository.PartieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JeuSansTimer implements ModeJeuStrategy {

    @Autowired
    private PartieRepository partieRepository;
    @Override
    public Partie startGame(Partie partie) {
return partie;
    }

    @Override
    public void stopGame (UUID idPartie) {
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
