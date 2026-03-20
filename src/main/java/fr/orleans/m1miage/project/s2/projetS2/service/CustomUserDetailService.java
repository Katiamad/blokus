package fr.orleans.m1miage.project.s2.projetS2.service;

import fr.orleans.m1miage.project.s2.projetS2.model.Utilisateur;
import fr.orleans.m1miage.project.s2.projetS2.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomUserDetailService implements UserDetailsService {

    @Autowired
    public UtilisateurRepository utilisateurRepository;

    @Override
    public UserDetails loadUserByUsername(String nomOuEmail) throws UsernameNotFoundException {
        Optional<Utilisateur> opt = utilisateurRepository.findByNomUtilisateur(nomOuEmail);
        if (opt.isEmpty()) {
            Utilisateur byEmail = utilisateurRepository.findByEmail(nomOuEmail);
            opt = Optional.ofNullable(byEmail);
        }
        Utilisateur user = opt.orElseThrow(() ->
                new UsernameNotFoundException("Utilisateur introuvable pour : " + nomOuEmail)
        );

        return User.builder()
                .username(user.getNom())
                .password(user.getMotDePasse())
                .roles("USER")
                .build();
    }

}

