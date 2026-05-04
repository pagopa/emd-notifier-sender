import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.annotation.PreDestroy;

@Component
@Slf4j
public class TacticalShutdownBlocker {

    @PreDestroy
    public void blockShutdown() {
        log.warn("[TACTICAL-SHUTDOWN] SIGTERM ricevuto. Metto in pausa lo spegnimento di Spring per 20 secondi per far finire le catene WebFlux staccate...");
        try {
            // Blocca la chiusura del context di Spring, dando tempo ai thread 
            // in background (Netty/Reactor) di svuotare il batch Kafka in corso.
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.warn("[TACTICAL-SHUTDOWN] Pausa terminata, procedo con la distruzione dei bean.");
    }
}