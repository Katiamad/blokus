package fr.orleans.m1miage.project.s2.projetS2.model;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Converter(autoApply = true)
public class GrilleConverter implements AttributeConverter<int[][], String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(int[][] grille) {
        try {
            return objectMapper.writeValueAsString(grille);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erreur lors de la conversion de la grille en JSON", e);
        }
    }

    @Override
    public int[][] convertToEntityAttribute(String dbData) {
        try {
            return objectMapper.readValue(dbData, int[][].class);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la conversion du JSON en grille", e);
        }
    }
}
