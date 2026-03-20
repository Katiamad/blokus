package fr.orleans.m1miage.project.s2.projetS2.service;

import fr.orleans.m1miage.project.s2.projetS2.exeptions.PartieNotFoundException;
import fr.orleans.m1miage.project.s2.projetS2.model.*;
import fr.orleans.m1miage.project.s2.projetS2.repository.PartieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class StatistiquesServiceImpl implements StatistiquesService {

    @Autowired
    private PartieRepository partieRepository;

    @Override
    public JoueurStatistique getStatisticsForUser(UUID utilisateurId) {
        List<Partie> finies = partieRepository
                .findByJoueursUtilisateurIdAndStatus(utilisateurId, Status.TERMINEE);

        int played = finies.size();
        int won = 0;

        for (Partie p : finies) {
            int maxScore = Stream
                    .concat(
                            p.getJoueurs().stream().map(PartieJoueur::getPoints),
                            p.getRobots().stream().map(PartieRobot::getPoints)
                    )
                    .max(Integer::compareTo)
                    .orElse(0);

            int playerScore = p.getJoueurs().stream()
                    .filter(pj -> pj.getUtilisateur() != null && utilisateurId.equals(pj.getUtilisateur().getId()))
                    .map(PartieJoueur::getPoints)
                    .findFirst()
                    .orElse(-1);

            if (playerScore == maxScore) {
                won++;
            }
        }

        double ratio = played > 0 ? (double) won / played : 0.0;
        return new JoueurStatistique(played, won, ratio);
    }



}