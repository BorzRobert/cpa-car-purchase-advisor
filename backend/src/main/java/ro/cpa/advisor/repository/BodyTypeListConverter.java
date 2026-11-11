package ro.cpa.advisor.repository;

import jakarta.persistence.Converter;
import ro.cpa.advisor.domain.BodyType;

@Converter
class BodyTypeListConverter extends EnumListConverter<BodyType> {

    BodyTypeListConverter() {
        super(BodyType.class);
    }
}
