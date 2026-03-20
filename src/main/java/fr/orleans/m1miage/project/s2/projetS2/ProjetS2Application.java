package fr.orleans.m1miage.project.s2.projetS2;

import fr.orleans.m1miage.project.s2.projetS2.model.Utilisateur;
import fr.orleans.m1miage.project.s2.projetS2.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Base64;

@SpringBootApplication
public class ProjetS2Application {

    public static void main(String[] args) {
        SpringApplication.run(ProjetS2Application.class, args);
    }
}
