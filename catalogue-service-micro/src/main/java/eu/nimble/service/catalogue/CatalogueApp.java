package eu.nimble.service.catalogue;

import eu.nimble.service.catalogue.sync.MarmottaSynchronizer;
import eu.nimble.service.catalogue.sync.UBLPropertyIndexing;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.RestController;

@Configuration
@EnableCircuitBreaker
@EnableAutoConfiguration
@EnableEurekaClient
@EnableFeignClients(basePackages = "eu")
@RestController
@SpringBootApplication
@ComponentScan(basePackages = "eu")
public class CatalogueApp implements CommandLineRunner {

    @Override
    public void run(String... arg0) throws Exception {
        if (arg0.length > 0 && arg0[0].equals("exitcode")) {
            throw new ExitException();
        }
    }

    public static void main(String[] args) throws Exception {
        new SpringApplication(CatalogueApp.class).run(args);
    }

    class ExitException extends RuntimeException implements ExitCodeGenerator {
        private static final long serialVersionUID = 1L;

        @Override
        public int getExitCode() {
            return 10;
        }

    }

    @EventListener({ContextRefreshedEvent.class})
    void contextRefreshedEvent() {
//        HibernateUtility.getInstance(eu.nimble.utility.Configuration.UBL_PERSISTENCE_UNIT_NAME);
        MarmottaSynchronizer.getInstance().startSynchronization();
        // index catalogue properties
        UBLPropertyIndexing.indexCatalogueProperties();
    }

    @EventListener({ContextClosedEvent.class})
    void contextClosedEvent() {
        MarmottaSynchronizer.getInstance().stopSynchronization();
    }
}
