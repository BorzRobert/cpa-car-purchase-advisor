package ro.cpa.advisor.repository;

import jakarta.persistence.AttributeConverter;
import java.util.Arrays;
import java.util.List;

/**
 * Lista de enum-uri ca text separat prin virgula intr-o singura coloana. Alternativa (tabele de legatura) ar adauga
 * doua tabele pentru liste scurte (max. 5 valori) pe care nu le interogam niciodata individual.
 */
abstract class EnumListConverter<E extends Enum<E>> implements AttributeConverter<List<E>, String> {

    private final Class<E> type;

    EnumListConverter(Class<E> type) {
        this.type = type;
    }

    @Override
    public String convertToDatabaseColumn(List<E> values) {
        return values == null || values.isEmpty() ? null : String.join(",", values.stream().map(Enum::name).toList());
    }

    @Override
    public List<E> convertToEntityAttribute(String column) {
        if (column == null || column.isBlank()) {
            return List.of();
        }
        return Arrays.stream(column.split(",")).map(name -> Enum.valueOf(type, name)).toList();
    }
}
