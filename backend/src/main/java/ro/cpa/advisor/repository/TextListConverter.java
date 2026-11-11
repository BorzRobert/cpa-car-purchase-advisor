package ro.cpa.advisor.repository;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.List;

/**
 * Lista de texte scurte (conditii obligatorii/eliminatorii) separate prin `\n`. Sigur, fiindca `TextRules` garanteaza
 * ca elementele nu contin niciodata un rand nou.
 */
@Converter
class TextListConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join("\n", values);
    }

    @Override
    public List<String> convertToEntityAttribute(String column) {
        return column == null || column.isEmpty() ? List.of() : Arrays.asList(column.split("\n"));
    }
}
