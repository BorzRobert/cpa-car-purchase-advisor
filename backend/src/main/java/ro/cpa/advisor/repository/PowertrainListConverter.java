package ro.cpa.advisor.repository;

import jakarta.persistence.Converter;
import ro.cpa.advisor.domain.Powertrain;

@Converter
class PowertrainListConverter extends EnumListConverter<Powertrain> {

    PowertrainListConverter() {
        super(Powertrain.class);
    }
}
