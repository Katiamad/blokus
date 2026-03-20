package fr.orleans.m1miage.project.s2.projetS2.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class PieceConverter implements AttributeConverter<List<Piece>, String> {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public String convertToDatabaseColumn(List<Piece> pieceList) {

        try {
            return objectMapper.writeValueAsString(pieceList);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Piece> convertToEntityAttribute(String s) {
        try {
            return objectMapper.readValue(s,new TypeReference<List<Piece>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}