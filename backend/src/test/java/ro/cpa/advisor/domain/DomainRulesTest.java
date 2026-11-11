package ro.cpa.advisor.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Regulile pure ale domeniului: normalizarea textului primit de la browser, tokenul de sesiune, profilul, filtrul. */
class DomainRulesTest {

    @Test
    void singleLineCollapsesWhitespaceAndStripsInvisibleCharacters() {
        // U+202E (suprascriere de directie) si U+200B (zero-width) sunt folosite sa ascunda text in prompturi.
        String dirty = "  Fara\u202E  \u200Bcomplicatii \n\t ieftina ";
        assertThat(TextRules.singleLine(dirty, 80, "x")).isEqualTo("Fara complicatii ieftina");
    }

    @Test
    void blankTextBecomesNullAndTooLongTextIsRejectedNotTruncated() {
        assertThat(TextRules.singleLine("  \u200B ", 10, "x")).isNull();
        assertThatThrownBy(() -> TextRules.singleLine("a".repeat(11), 10, "title"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("title");
    }

    @Test
    void multiLineKeepsLineBreaksButNormalizesThem() {
        assertThat(TextRules.multiLine("one\r\ntwo\rthree\u0007", 100, "x")).isEqualTo("one\ntwo\nthree");
    }

    @Test
    void listsAreDeduplicatedAndCapped() {
        assertThat(TextRules.singleLineList(List.of("ISOFIX", " ISOFIX ", "", "camera"), 5, 80, "mustHaves"))
                .containsExactly("ISOFIX", "camera");
        assertThatThrownBy(() -> TextRules.singleLineList(List.of("a", "b", "c"), 2, 10, "mustHaves"))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void sessionTokensAreRandomShapedAndHashedDeterministically() {
        String token = SessionToken.generate();
        assertThat(SessionToken.hasValidShape(token)).isTrue();
        assertThat(token).isNotEqualTo(SessionToken.generate());
        assertThat(SessionToken.hash(token)).hasSize(64).isEqualTo(SessionToken.hash(token)).isNotEqualTo(token);
        assertThat(SessionToken.hasValidShape("cpa_short")).isFalse();
        assertThat(SessionToken.hasValidShape(null)).isFalse();
        assertThat(SessionToken.hasValidShape("x".repeat(47))).isFalse();
    }

    @Test
    void profileRejectsAmountWithoutKindAndOutOfRangeValues() {
        assertThatThrownBy(() -> profile(new BigDecimal("20000"), null, 10, 50, 4)).hasMessageContaining("budgetKind");
        assertThatThrownBy(() -> profile(null, null, -1, 50, 4)).hasMessageContaining("yearlyKm");
        assertThatThrownBy(() -> profile(null, null, 10, 101, 4)).hasMessageContaining("cityPercent");
        assertThatThrownBy(() -> profile(null, null, 10, 50, 0)).hasMessageContaining("passengers");
    }

    @Test
    void emptyProfileHasNoNullLists() {
        BuyerProfile empty = BuyerProfile.empty();
        assertThat(empty.bodyTypes()).isEmpty();
        assertThat(empty.mustHaves()).isEmpty();
        assertThat(empty.updatedAt()).isNull();
    }

    @Test
    void vehicleFilterKeepsOnlyMatchingVariantsAndDropsEmptyModels() {
        VehicleVariant cheapPetrol = variant("a-petrol", Powertrain.PETROL, 15_000);
        VehicleVariant pricierEv = variant("a-ev", Powertrain.ELECTRIC, 35_000);
        Vehicle car = new Vehicle("a", "Make", "A", BodyType.HATCHBACK, 5, 300, 5, "notes", List.of(cheapPetrol, pricierEv));

        assertThat(new VehicleFilter(null, null, null, null).apply(car).variants()).hasSize(2);
        assertThat(new VehicleFilter(null, Powertrain.ELECTRIC, null, null).apply(car).variants()).containsExactly(pricierEv);
        assertThat(new VehicleFilter(null, null, 20_000, null).apply(car).variants()).containsExactly(cheapPetrol);
        assertThat(new VehicleFilter(BodyType.SUV, null, null, null).apply(car)).isNull();
        assertThat(new VehicleFilter(null, null, 1_000, null).apply(car)).isNull();
        assertThat(new VehicleFilter(null, null, null, 7).apply(car)).isNull();
    }

    private static BuyerProfile profile(BigDecimal amount, BudgetKind kind, Integer km, Integer city, Integer passengers) {
        return new BuyerProfile(amount, kind, null, km, city, passengers, null, null, null, null, null, null, null);
    }

    private static VehicleVariant variant(String id, Powertrain powertrain, int price) {
        return new VehicleVariant(id, id, powertrain, price, 90, null, null, null, null, 0, 500, 0, 300, 50, LocalDate.of(2026, 11, 11));
    }
}
