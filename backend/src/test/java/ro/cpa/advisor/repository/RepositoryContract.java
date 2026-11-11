package ro.cpa.advisor.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import ro.cpa.advisor.domain.BodyType;
import ro.cpa.advisor.domain.BudgetKind;
import ro.cpa.advisor.domain.BuyerProfile;
import ro.cpa.advisor.domain.ConditionPreference;
import ro.cpa.advisor.domain.FinancingMode;
import ro.cpa.advisor.domain.MessageRole;
import ro.cpa.advisor.domain.Powertrain;

/**
 * Aceleasi verificari pe H2 si pe PostgreSQL (vezi subclasele): dovedesc ca migrarile Flyway si maparile JPA merg pe
 * ambele motoare. `ddl-auto: validate` face ca orice nepotrivire dintre entitati si schema sa pice la pornirea contextului.
 */
abstract class RepositoryContract {

    private static final Instant T0 = Instant.parse("2026-11-11T10:00:00Z");

    @Autowired
    EntityManager em;

    @Autowired
    VisitorSessionRepository sessions;

    @Autowired
    BuyerProfileRepository profiles;

    @Autowired
    ConversationRepository conversations;

    @Autowired
    MessageRepository messages;

    @Autowired
    VehicleRepository vehicles;

    @Autowired
    VehicleVariantRepository variants;

    @Autowired
    ShortlistItemRepository shortlist;

    @Autowired
    UsageLogRepository usage;

    @Test
    void migrationsSeedTheCatalogue() {
        assertThat(vehicles.count()).isEqualTo(14);
        assertThat(variants.count()).isEqualTo(21);
        VehicleVariantEntity spring = variants.findById("spring-electric-65").orElseThrow();
        assertThat(spring.toDomain().powertrain()).isEqualTo(Powertrain.ELECTRIC);
        assertThat(spring.toDomain().consumptionLitersPer100Km()).isNull();
        assertThat(spring.toDomain().consumptionKwhPer100Km()).isEqualByComparingTo("13.9");
        assertThat(variants.findByVehicleId("vw-golf")).hasSize(3);
        assertThat(vehicles.findById("dacia-sandero").orElseThrow().toDomain(List.of()).euroNcapStars()).isNull();
    }

    @Test
    void sessionTokenHashIsUnique() {
        sessions.saveAndFlush(new VisitorSessionEntity(UUID.randomUUID(), "a".repeat(64), T0));
        assertThat(sessions.findByTokenHash("a".repeat(64))).isPresent();
        assertThatThrownBy(() -> sessions.saveAndFlush(new VisitorSessionEntity(UUID.randomUUID(), "a".repeat(64), T0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void profileRoundTripsEnumsListsAndDecimals() {
        UUID sessionId = newSession("b");
        BuyerProfile profile = new BuyerProfile(
                new BigDecimal("20000.50"), BudgetKind.TOTAL, FinancingMode.CASH, 15_000, 70, 4,
                List.of(BodyType.HATCHBACK, BodyType.SUV), List.of(Powertrain.HYBRID, Powertrain.ELECTRIC), false,
                ConditionPreference.NEW, List.of("ISOFIX", "camera de mers inapoi"), List.of("fara cutie automata"), null);

        BuyerProfileEntity entity = new BuyerProfileEntity(sessionId);
        entity.apply(profile, T0);
        profiles.saveAndFlush(entity);
        em.clear();

        BuyerProfile loaded = profiles.findById(sessionId).orElseThrow().toDomain();
        assertThat(loaded.budgetAmount()).isEqualByComparingTo("20000.50");
        assertThat(loaded.bodyTypes()).containsExactly(BodyType.HATCHBACK, BodyType.SUV);
        assertThat(loaded.powertrains()).containsExactly(Powertrain.HYBRID, Powertrain.ELECTRIC);
        assertThat(loaded.homeCharging()).isFalse();
        assertThat(loaded.mustHaves()).containsExactly("ISOFIX", "camera de mers inapoi");
        assertThat(loaded.dealBreakers()).containsExactly("fara cutie automata");
        assertThat(loaded.updatedAt()).isEqualTo(T0);
    }

    @Test
    void emptyProfileRoundTripsAsNullsAndEmptyLists() {
        UUID sessionId = newSession("c");
        BuyerProfileEntity entity = new BuyerProfileEntity(sessionId);
        entity.apply(BuyerProfile.empty(), T0);
        profiles.saveAndFlush(entity);
        em.clear();

        BuyerProfile loaded = profiles.findById(sessionId).orElseThrow().toDomain();
        assertThat(loaded.budgetAmount()).isNull();
        assertThat(loaded.bodyTypes()).isEmpty();
        assertThat(loaded.mustHaves()).isEmpty();
    }

    @Test
    void conversationsAreScopedToTheirSessionAndMessagesAreOrdered() {
        UUID mine = newSession("d");
        UUID other = newSession("e");
        ConversationEntity conversation = conversations.saveAndFlush(new ConversationEntity(UUID.randomUUID(), mine, "Titlu", T0));
        UUID conversationId = conversation.toDomain().id();

        messages.saveAndFlush(new MessageEntity(UUID.randomUUID(), conversationId, MessageRole.ASSISTANT, "al doilea", T0.plusSeconds(5)));
        messages.saveAndFlush(new MessageEntity(UUID.randomUUID(), conversationId, MessageRole.USER, "primul", T0));

        assertThat(conversations.findByIdAndSessionId(conversationId, mine)).isPresent();
        assertThat(conversations.findByIdAndSessionId(conversationId, other)).isEmpty();
        assertThat(messages.findByConversationIdOrderByCreatedAtAscIdAsc(conversationId))
                .extracting(m -> m.toDomain().content())
                .containsExactly("primul", "al doilea");
        assertThat(messages.countByConversationId(conversationId)).isEqualTo(2);
    }

    @Test
    void deletingASessionCascadesToItsDataButKeepsAnonymousUsage() {
        UUID sessionId = newSession("f");
        UUID conversationId = conversations.saveAndFlush(new ConversationEntity(UUID.randomUUID(), sessionId, null, T0)).toDomain().id();
        messages.saveAndFlush(new MessageEntity(UUID.randomUUID(), conversationId, MessageRole.USER, "salut", T0));
        shortlist.saveAndFlush(new ShortlistItemEntity(UUID.randomUUID(), sessionId, "golf-1-5-tsi-130", null, T0));
        BuyerProfileEntity profile = new BuyerProfileEntity(sessionId);
        profile.apply(BuyerProfile.empty(), T0);
        profiles.saveAndFlush(profile);
        LocalDate day = LocalDate.of(2026, 11, 11);
        usage.saveAndFlush(new UsageLogEntity(UUID.randomUUID(), sessionId, day, "gpt-test", 100, 50, T0));
        long globalBefore = usage.sumAllTokens(day);
        em.clear();

        sessions.deleteById(sessionId);
        sessions.flush();
        em.clear();

        assertThat(conversations.countBySessionId(sessionId)).isZero();
        assertThat(messages.countByConversationId(conversationId)).isZero();
        assertThat(shortlist.countBySessionId(sessionId)).isZero();
        assertThat(profiles.findById(sessionId)).isEmpty();
        // „Sterge-mi datele” nu trebuie sa reseteze bugetul global: randul de consum ramane, fara legatura cu cineva.
        assertThat(usage.sumAllTokens(day)).isEqualTo(globalBefore);
        assertThat(usage.sumInputTokens(sessionId, day)).isZero();
    }

    @Test
    void aVariantCanBeShortlistedOncePerSession() {
        UUID sessionId = newSession("g");
        shortlist.saveAndFlush(new ShortlistItemEntity(UUID.randomUUID(), sessionId, "golf-1-5-tsi-130", "nota", T0));
        assertThat(shortlist.existsBySessionIdAndVariantId(sessionId, "golf-1-5-tsi-130")).isTrue();
        assertThatThrownBy(() -> shortlist.saveAndFlush(new ShortlistItemEntity(UUID.randomUUID(), sessionId, "golf-1-5-tsi-130", null, T0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shortlistRejectsAnUnknownVariant() {
        UUID sessionId = newSession("h");
        assertThatThrownBy(() -> shortlist.saveAndFlush(new ShortlistItemEntity(UUID.randomUUID(), sessionId, "no-such-variant", null, T0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void usageSumsAreSplitPerSessionAndDay() {
        UUID sessionId = newSession("i");
        LocalDate day = LocalDate.of(2030, 1, 1);
        usage.saveAndFlush(new UsageLogEntity(UUID.randomUUID(), sessionId, day, "m", 10, 5, T0));
        usage.saveAndFlush(new UsageLogEntity(UUID.randomUUID(), sessionId, day, "m", 20, 7, T0));
        usage.saveAndFlush(new UsageLogEntity(UUID.randomUUID(), sessionId, day.plusDays(1), "m", 1000, 1000, T0));

        assertThat(usage.sumInputTokens(sessionId, day)).isEqualTo(30);
        assertThat(usage.sumOutputTokens(sessionId, day)).isEqualTo(12);
        assertThat(usage.sumAllTokens(day)).isEqualTo(42);
    }

    private UUID newSession(String hashSeed) {
        UUID id = UUID.randomUUID();
        sessions.saveAndFlush(new VisitorSessionEntity(id, hashSeed.repeat(64).substring(0, 60) + id.toString().substring(0, 4), T0));
        return id;
    }
}
