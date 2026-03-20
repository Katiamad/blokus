package fr.orleans.m1miage.project.s2.projetS2.service;

import fr.orleans.m1miage.project.s2.projetS2.exeptions.InvalidEmailFormatException;
import fr.orleans.m1miage.project.s2.projetS2.exeptions.UtilisateurDejaExistantException;
import fr.orleans.m1miage.project.s2.projetS2.model.Utilisateur;

import java.util.List;

public interface UtilisateurService {

    public Utilisateur findUtilisateurByEmail(String email);

    public Utilisateur findUtilisateurByUsername(String username);

    public Utilisateur findUtilisateurByUsernameOuEmail(String nomOuEmail);

    public void ajouterUtilisateur(Utilisateur utilisateur) throws Exception, InvalidEmailFormatException;

    public void modifierNomEmailUtilisateur(Utilisateur utilisateur) throws Exception, InvalidEmailFormatException, UtilisateurDejaExistantException;

    public void modifierMotDePasseUtilisateur(Utilisateur utilisateur) throws Exception;

    public void deleteUtilisateur(Utilisateur utilisateur) throws Exception;
}
