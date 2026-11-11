package ro.cpa.advisor.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Evenimentele unui raspuns in streaming (contractul SSE din docs/architecture/communication.md). Sunt date simple,
 * fara legatura cu transportul: serviciul le produce, stratul `api` le scrie ca SSE. Numele evenimentului este
 * parte din contract, deci sta aici, langa payload, nu in controller.
 */
public sealed interface AdvisorEvent {

    /** Numele evenimentului SSE (campul `event:`). */
    String name();

    /** Inceputul raspunsului: id-ul pe care mesajul consilierului il va avea in baza de date. */
    record Meta(UUID messageId, UUID conversationId) implements AdvisorEvent {
        @Override
        public String name() {
            return "meta";
        }
    }

    /** O bucata de text din raspuns. */
    record Token(String delta) implements AdvisorEvent {
        @Override
        public String name() {
            return "token";
        }
    }

    /** Eroare aparuta dupa inceperea fluxului; `code` are aceeasi semnificatie ca in `ProblemDetail`. */
    record Failed(String code, String detail) implements AdvisorEvent {
        @Override
        public String name() {
            return "error";
        }
    }

    /** Sfarsitul fluxului: raspunsul este salvat si consumul inregistrat. */
    record Done(TokenUsage usage) implements AdvisorEvent {
        @Override
        public String name() {
            return "done";
        }
    }

    /** O unealta pornita sau terminata; `summary` este textul scurt pe care UI-ul il arata („Caut masini…”). */
    record Tool(String toolName, String status, String summary) implements AdvisorEvent {

        public static final String STARTED = "started";
        public static final String FINISHED = "finished";

        @Override
        public String name() {
            return "tool";
        }
    }

    /** Carduri de vehicule gasite de o unealta, pe care UI-ul le arata in loc de tabele Markdown. */
    record Vehicles(List<VehicleCard> cards, LocalDate dataAsOf) implements AdvisorEvent {
        @Override
        public String name() {
            return "vehicles";
        }
    }

    /** Campurile de profil schimbate de agent (panoul lateral se actualizeaza live); doar ce s-a schimbat, cu valorile curatate. */
    record ProfileUpdated(Map<String, Object> patch) implements AdvisorEvent {
        @Override
        public String name() {
            return "profile";
        }
    }

    record TokenUsage(int input, int output) {}
}
