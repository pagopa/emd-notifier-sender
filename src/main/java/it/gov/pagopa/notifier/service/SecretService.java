package it.gov.pagopa.notifier.service;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretAsyncClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class SecretService {

    private final SecretAsyncClient secretClient;

    public SecretService(@Value("${app.key-vault-uri}") String keyVaultUri) {
        this.secretClient = new SecretClientBuilder()
                .vaultUrl(keyVaultUri)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildAsyncClient();
    }
    public Mono<String> getSecret(String secretName) {
        return secretClient.getSecret(secretName)
                .map(KeyVaultSecret::getValue);
    }
}


