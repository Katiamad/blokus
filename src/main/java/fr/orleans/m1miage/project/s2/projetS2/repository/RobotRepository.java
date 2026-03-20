package fr.orleans.m1miage.project.s2.projetS2.repository;

import fr.orleans.m1miage.project.s2.projetS2.model.PartieRobot;
import fr.orleans.m1miage.project.s2.projetS2.model.Piece;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RobotRepository  extends JpaRepository<PartieRobot, Integer> {
}