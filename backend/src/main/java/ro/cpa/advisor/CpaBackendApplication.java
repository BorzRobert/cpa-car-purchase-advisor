package ro.cpa.advisor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punctul de intrare al backend-ului CPA; toata logica sta in pachetele `api`, `service`, `domain`, `repository`, `ai`, `config`. */
@SpringBootApplication
public class CpaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CpaBackendApplication.class, args);
    }
}
