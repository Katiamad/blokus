package fr.orleans.m1miage.project.s2.projetS2.exeptions;

public class EmailDejaExistantException extends RuntimeException {
    public EmailDejaExistantException(String email) {
        super("Email '" + email + "' déjà utilisé");
    }
    }

