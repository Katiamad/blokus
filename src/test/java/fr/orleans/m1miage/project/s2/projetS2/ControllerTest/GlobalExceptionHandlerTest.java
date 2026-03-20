package fr.orleans.m1miage.project.s2.projetS2.ControllerTest;

import fr.orleans.m1miage.project.s2.projetS2.controller.GlobalExceptionHandler;
import fr.orleans.m1miage.project.s2.projetS2.exeptions.InvalidEmailFormatException;
import fr.orleans.m1miage.project.s2.projetS2.exeptions.PartieNonRejoignableException;
import fr.orleans.m1miage.project.s2.projetS2.exeptions.PartieNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleUUIDFormatError_uuidType_returnsRedirectAndAttribute() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getRequiredType()).thenAnswer(inv -> UUID.class);

        String view = handler.handleUUIDFormatError(ex, redirectAttributes);

        assertEquals("redirect:/blokus/home", view);
        verify(redirectAttributes).addAttribute(eq("errorJoin"), contains("identifiant de la partie"));
    }


    @Test
    void handleUUIDFormatError_nonUuidType_returnsRedirectAndFallback() {
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        String view = handler.handleUUIDFormatError(ex, redirectAttributes);

        assertEquals("redirect:/blokus/home", view);
        verify(redirectAttributes).addAttribute(eq("errorJoin"), eq("Lien invalide."));
    }

    @Test
    void handleNotFound_setsAttributeAndRedirects() {
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

        PartieNotFoundException ex = new PartieNotFoundException("1234"); // ← ou l’id que tu veux
        String view = handler.handleNotFound(ex, redirectAttributes);

        assertEquals("redirect:/blokus/home", view);
        verify(redirectAttributes).addAttribute(eq("errorJoin"), eq("Partie avec l'id 1234 introuvable."));
    }

    @Test
    void handleNonRejoignable_setsAttributeAndRedirects() {
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

        PartieNonRejoignableException ex = new PartieNonRejoignableException("Non rejoignable !");
        String view = handler.handleNonRejoignable(ex, redirectAttributes);

        assertEquals("redirect:/blokus/home", view);
        verify(redirectAttributes).addAttribute(eq("errorJoin"), eq("Non rejoignable !"));
    }

    @Test
    void handleIllegalArg_uuidMessage_setsAttributeAndRedirects() {
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

        IllegalArgumentException ex = new IllegalArgumentException("For input string: 'bad' (UUID error)");
        String view = handler.handleIllegalArg(ex, redirectAttributes);

        assertEquals("redirect:/blokus/home", view);
        verify(redirectAttributes).addAttribute(eq("errorJoin"), contains("mal formé"));
    }

    @Test
    void handleIllegalArg_other_setsFallbackAndRedirects() {
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

        IllegalArgumentException ex = new IllegalArgumentException("Autre erreur");
        String view = handler.handleIllegalArg(ex, redirectAttributes);

        assertEquals("redirect:/blokus/home", view);
        verify(redirectAttributes).addAttribute(eq("errorJoin"), contains("Autre erreur"));
    }

    @Test
    void handleInvalidEmail_setsModelAndReturnErreur() {
        Model model = mock(Model.class);

        InvalidEmailFormatException ex = new InvalidEmailFormatException("Mauvais format mail !");
        String view = handler.handleInvalidEmail(ex, model);

        assertEquals("erreur", view);
        verify(model).addAttribute(eq("error"), eq("Mauvais format mail !"));
    }

    @Test
    void handleAnyException_setsFlashAndRedirects() {
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

        Exception ex = new Exception("plop");
        String view = handler.handleAnyException(ex, redirectAttributes);

        assertEquals("redirect:/blokus/home", view);
        verify(redirectAttributes).addFlashAttribute(eq("errorJoin"), contains("Lien invalide"));
    }

    @Test
    void handleIllegalArg_messageContainsUuid_setsProperAttribute() {
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

        IllegalArgumentException ex = new IllegalArgumentException("UUID not valid!");
        String view = handler.handleIllegalArg(ex, redirectAttributes);

        assertEquals("redirect:/blokus/home", view);
        verify(redirectAttributes).addAttribute(eq("errorJoin"), contains("identifiant de partie mal formé"));
    }

    @Test
    void handleIllegalArg_messageDoesNotContainUuid_setsFallbackAttribute() {
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

        IllegalArgumentException ex = new IllegalArgumentException("Autre erreur");
        String view = handler.handleIllegalArg(ex, redirectAttributes);

        assertEquals("redirect:/blokus/home", view);
        verify(redirectAttributes).addAttribute(eq("errorJoin"), contains("Erreur : Autre erreur"));
    }

    @Test
    void handleIllegalArg_messageIsNull_setsFallbackAttribute() {
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

        IllegalArgumentException ex = mock(IllegalArgumentException.class);
        when(ex.getMessage()).thenReturn(null);

        String view = handler.handleIllegalArg(ex, redirectAttributes);

        assertEquals("redirect:/blokus/home", view);
        verify(redirectAttributes).addAttribute(eq("errorJoin"), eq("Erreur : null"));
    }

    @Test
    void handleIllegalArg_messageIsEmpty_setsFallbackAttribute() {
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

        IllegalArgumentException ex = mock(IllegalArgumentException.class);
        when(ex.getMessage()).thenReturn("");

        String view = handler.handleIllegalArg(ex, redirectAttributes);

        assertEquals("redirect:/blokus/home", view);
        verify(redirectAttributes).addAttribute(eq("errorJoin"), eq("Erreur : "));
    }



}
