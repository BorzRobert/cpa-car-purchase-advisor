package ro.cpa.advisor.repository;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

/** Repository-urile pe H2 (mod PostgreSQL), baza folosita local si in testele rapide. */
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryH2Test extends RepositoryContract {}
