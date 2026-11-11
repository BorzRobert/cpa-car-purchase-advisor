package ro.cpa.advisor.domain;

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

    record TokenUsage(int input, int output) {}
}
