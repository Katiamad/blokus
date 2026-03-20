package fr.orleans.m1miage.project.s2.projetS2.exeptions;

public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException(String message) {
        super(message);
    }
}
