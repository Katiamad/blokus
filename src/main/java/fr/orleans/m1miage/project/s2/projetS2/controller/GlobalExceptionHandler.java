package fr.orleans.m1miage.project.s2.projetS2.controller;

import fr.orleans.m1miage.project.s2.projetS2.exeptions.InvalidEmailFormatException;
import fr.orleans.m1miage.project.s2.projetS2.exeptions.PartieNonRejoignableException;
import fr.orleans.m1miage.project.s2.projetS2.exeptions.PartieNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String handleUUIDFormatError(MethodArgumentTypeMismatchException ex, RedirectAttributes redirectAttributes) {
        if (ex.getRequiredType() == UUID.class) {
            redirectAttributes.addAttribute("errorJoin", "Lien invalide : l'identifiant de la partie est incorrect.");
            return "redirect:/blokus/home";
        }
        redirectAttributes.addAttribute("errorJoin", "Lien invalide.");
        return "redirect:/blokus/home";
    }

    @ExceptionHandler(PartieNotFoundException.class)
    public String handleNotFound(PartieNotFoundException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("errorJoin", ex.getMessage());
        return "redirect:/blokus/home";
    }

    @ExceptionHandler(PartieNonRejoignableException.class)
    public String handleNonRejoignable(PartieNonRejoignableException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("errorJoin", ex.getMessage());
        return "redirect:/blokus/home";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArg(IllegalArgumentException ex, RedirectAttributes redirectAttributes) {
        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("uuid")) {
            redirectAttributes.addAttribute("errorJoin", "Lien invalide : identifiant de partie mal formé.");
            return "redirect:/blokus/home";
        }
        redirectAttributes.addAttribute("errorJoin", "Erreur : " + ex.getMessage());
        return "redirect:/blokus/home";
    }

    @ExceptionHandler(InvalidEmailFormatException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleInvalidEmail(InvalidEmailFormatException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "erreur";
    }

    @ExceptionHandler(Exception.class)
    public String handleAnyException(Exception ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorJoin", "Lien invalide ou partie inexistante.");
        return "redirect:/blokus/home";
    }


}
