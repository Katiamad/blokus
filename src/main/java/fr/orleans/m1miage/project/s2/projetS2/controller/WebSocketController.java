package fr.orleans.m1miage.project.s2.projetS2.controller;
import fr.orleans.m1miage.project.s2.projetS2.model.Partie;
import fr.orleans.m1miage.project.s2.projetS2.service.PartieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
public class WebSocketController {

    @Autowired
    private PartieService partieService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

}