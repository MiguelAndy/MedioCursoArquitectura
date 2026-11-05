package pe.abc.comercial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Monolito modular ABC Comercial (ADR-02): MVC por fuera, hexágono por dentro. */
@SpringBootApplication
public class AbcComercialApplication {

    public static void main(String[] args) {
        SpringApplication.run(AbcComercialApplication.class, args);
    }
}
