package it.gov.pagopa.common.configuration;

import com.mongodb.lang.NonNull;
import it.gov.pagopa.common.utils.CommonConstants;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.Decimal128;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Configuration class for MongoDB settings and custom conversions.
 */
@Configuration
@EnableConfigurationProperties(MongoConfig.MongoDbCustomProperties.class)
public class MongoConfig {

    /**
     * Configuration properties for MongoDB custom settings.
     */
    @ConfigurationProperties(prefix = "spring.data.mongodb.config")
    @Getter
    @Setter
    public static class MongoDbCustomProperties {

        ConnectionPoolSettings connectionPool;

        @Getter
        @Setter
        public static class ConnectionPoolSettings {
            int maxSize;
            int minSize;
            long maxWaitTimeMS;
            long maxConnectionLifeTimeMS;
            long maxConnectionIdleTimeMS;
            int maxConnecting;
        }

    }

    /**
     * Customizes the MongoDB client settings based on application properties.
     *
     * @param mongoDbCustomProperties the custom MongoDB properties
     * @return a customizer for MongoClientSettings
     */
    @Bean
    public MongoClientSettingsBuilderCustomizer customizer(MongoDbCustomProperties mongoDbCustomProperties) {
        return builder -> builder.applyToConnectionPoolSettings(
                connectionPool -> {
                    connectionPool.maxSize(mongoDbCustomProperties.connectionPool.maxSize);
                    connectionPool.minSize(mongoDbCustomProperties.connectionPool.minSize);
                    connectionPool.maxWaitTime(mongoDbCustomProperties.connectionPool.maxWaitTimeMS, TimeUnit.MILLISECONDS);
                    connectionPool.maxConnectionLifeTime(mongoDbCustomProperties.connectionPool.maxConnectionLifeTimeMS, TimeUnit.MILLISECONDS);
                    connectionPool.maxConnectionIdleTime(mongoDbCustomProperties.connectionPool.maxConnectionIdleTimeMS, TimeUnit.MILLISECONDS);
                    connectionPool.maxConnecting(mongoDbCustomProperties.connectionPool.maxConnecting);
                });
    }

    /**
     * Defines custom conversions for MongoDB to handle BigDecimal and OffsetDateTime types.
     *
     * @return a MongoCustomConversions object with custom converters
     */
    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(Arrays.asList(
                // BigDecimal support
                new BigDecimalDecimal128Converter(),
                new Decimal128BigDecimalConverter(),

                // OffsetDateTime support
                new OffsetDateTimeWriteConverter(),
                new OffsetDateTimeReadConverter()
        ));
    }

    /**
     * Converts BigDecimal to MongoDB Decimal128 format for writing.
     */
    @WritingConverter
    public static class BigDecimalDecimal128Converter implements Converter<BigDecimal, Decimal128> {

        @Override
        public Decimal128 convert(@NonNull BigDecimal source) {
            return new Decimal128(source);
        }
    }

    /**
     * Converts MongoDB Decimal128 to BigDecimal for reading.
     */
    @ReadingConverter
    public static class Decimal128BigDecimalConverter implements Converter<Decimal128, BigDecimal> {

        @Override
        public BigDecimal convert(@NonNull Decimal128 source) {
            return source.bigDecimalValue();
        }

    }

    /**
     * Converts OffsetDateTime to Date for writing to MongoDB.
     */
    @WritingConverter
    public static class OffsetDateTimeWriteConverter implements Converter<OffsetDateTime, Date> {
        @Override
        public Date convert(OffsetDateTime offsetDateTime) {
            return Date.from(offsetDateTime.toInstant());
        }
    }

    /**
     * Converts Date from MongoDB to OffsetDateTime for reading.
     */
    @ReadingConverter
    public static class OffsetDateTimeReadConverter implements Converter<Date, OffsetDateTime> {
        @Override
        public OffsetDateTime convert(Date date) {
            return date.toInstant().atZone(CommonConstants.ZONEID).toOffsetDateTime();
        }
    }
}

