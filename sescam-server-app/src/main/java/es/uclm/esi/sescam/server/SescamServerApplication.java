package es.uclm.esi.sescam.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point of the SESCAM server: starts the REST API and wires the domain modules. */
@SpringBootApplication
public class SescamServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SescamServerApplication.class, args);
    }
}
