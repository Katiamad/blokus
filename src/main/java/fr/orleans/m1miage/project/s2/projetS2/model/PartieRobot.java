package fr.orleans.m1miage.project.s2.projetS2.model;

import jakarta.persistence.*;

@Entity
@Table(name = "partie_robot")
public class PartieRobot extends Player {

    public PartieRobot() {
      super();
    }

}