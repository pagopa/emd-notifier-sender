package it.gov.pagopa.notifier.utils.faker;

import it.gov.pagopa.notifier.dto.TokenSection;
import it.gov.pagopa.notifier.dto.TppDTO;

import java.util.HashMap;

import static it.gov.pagopa.notifier.utils.TestUtils.AUTHENTICATION_URL;
import static it.gov.pagopa.notifier.utils.TestUtils.MESSAGE_URL;

public class TppDTOFaker {
    public static TppDTO mockInstance() {
        return TppDTO.builder()
                .tppId("tppId")
                .entityId("entityId")
                .authenticationUrl(AUTHENTICATION_URL)
                .messageUrl(MESSAGE_URL)
                .tokenSection(mockToken())
                .messageTemplate("""
                {
                  "messageId": "${messageId?json_string}",
                  "recipientId": "${recipientId?json_string}",
                  "triggerDateTimeUTC": "${triggerDateTimeUTC?json_string}",
                  "triggerDateTime": "${triggerDateTime?json_string}",
                  "messageUrl": "${messageUrl?json_string}",
                  "idPsp": "${idPsp?json_string}",
                  "senderDescription": "${(senderDescription! == '')?then('', senderDescription?json_string)}",
                  "originId": ${originId???then('"' + originId?json_string + '"', 'null')},
                  "title": ${title???then('"' + title?json_string + '"', 'null')},
                  "content": ${content???then('"' + content?json_string + '"', 'null')},
                  "analogSchedulingDate": ${analogSchedulingDate???then('"' + analogSchedulingDate?json_string + '"', 'null')},
                  "workflowType": ${workflowType???then('"' + workflowType?json_string + '"', 'null')},
                  "associatedPayment": ${associatedPayment???then(associatedPayment?c, 'null')}
                }
                """)
                .build();
    }

    private static TokenSection mockToken(){
        HashMap<String,String> map = new HashMap<>();
        map.put("test","test");

        return TokenSection.builder()
                .contentType("application/x-www-form-urlencoded")
                .bodyAdditionalProperties(map)
                .pathAdditionalProperties(map)
                .build();

    }
}
