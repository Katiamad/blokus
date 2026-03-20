package fr.orleans.m1miage.project.s2.projetS2.sevicesTest;

import fr.orleans.m1miage.project.s2.projetS2.model.Utilisateur;
import fr.orleans.m1miage.project.s2.projetS2.repository.UtilisateurRepository;
import fr.orleans.m1miage.project.s2.projetS2.service.CustomUserDetailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomUserDetailServiceTest {

    private UtilisateurRepository utilisateurRepository;
    private CustomUserDetailService service;

    @BeforeEach
    void setUp() {
        utilisateurRepository = mock(UtilisateurRepository.class);
        service = new CustomUserDetailService();
        service.utilisateurRepository = utilisateurRepository;
    }

    @Test
    void loadUserByUsername_foundByUsername_returnsUserDetails() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom("lucie");
        utilisateur.setMotDePasse("pw");

        when(utilisateurRepository.findByNomUtilisateur("lucie"))
                .thenReturn(Optional.of(utilisateur));

        UserDetails userDetails = service.loadUserByUsername("lucie");

        assertEquals("lucie", userDetails.getUsername());
        assertEquals("pw", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        verify(utilisateurRepository).findByNomUtilisateur("lucie");
        verify(utilisateurRepository, never()).findByEmail(any());
    }

    @Test
    void loadUserByUsername_notFoundByUsername_foundByEmail_returnsUserDetails() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom("lucie");
        utilisateur.setMotDePasse("pw");

        when(utilisateurRepository.findByNomUtilisateur("lucie@mail.com"))
                .thenReturn(Optional.empty());
        when(utilisateurRepository.findByEmail("lucie@mail.com"))
                .thenReturn(utilisateur);

        UserDetails userDetails = service.loadUserByUsername("lucie@mail.com");

        assertEquals("lucie", userDetails.getUsername());
        assertEquals("pw", userDetails.getPassword());
        verify(utilisateurRepository).findByNomUtilisateur("lucie@mail.com");
        verify(utilisateurRepository).findByEmail("lucie@mail.com");
    }

    @Test
    void loadUserByUsername_notFoundAnywhere_throwsException() {
        when(utilisateurRepository.findByNomUtilisateur("inconnu"))
                .thenReturn(Optional.empty());
        when(utilisateurRepository.findByEmail("inconnu"))
                .thenReturn(null);

        UsernameNotFoundException ex = assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername("inconnu")
        );

        assertTrue(ex.getMessage().contains("Utilisateur introuvable"));
        verify(utilisateurRepository).findByNomUtilisateur("inconnu");
        verify(utilisateurRepository).findByEmail("inconnu");
    }
}
