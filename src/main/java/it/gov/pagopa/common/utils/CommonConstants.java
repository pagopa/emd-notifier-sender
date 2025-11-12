package it.gov.pagopa.common.utils;

import java.time.ZoneId;

/**
 * Common constants used across the application
 */
public class CommonConstants {


    public static final class ExceptionCode {
        public static final String GENERIC_ERROR = "GENERIC_ERROR";
        private ExceptionCode() {}
    }

    public static final ZoneId ZONEID = ZoneId.of("Europe/Rome");

    private CommonConstants(){}
}
