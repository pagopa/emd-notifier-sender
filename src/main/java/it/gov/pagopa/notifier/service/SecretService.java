package it.gov.pagopa.notifier.service;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SecretService {

    private final SecretClient secretClient;

    public SecretService(@Value("${app.key-vault-uri}") String keyVaultUri) {
        this.secretClient = new SecretClientBuilder()
                .vaultUrl(keyVaultUri)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
    }
    public String getSecret(String secretName) {
            KeyVaultSecret retrievedSecret = secretClient.getSecret(secretName);
            return retrievedSecret.getValue();
    }

}
