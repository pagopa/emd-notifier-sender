package it.gov.pagopa.common.configuration;

import com.mongodb.MongoClientSettings;
import it.gov.pagopa.common.utils.CommonConstants;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        MongoConfig.class
})
@TestPropertySource(properties = {
        "spring.data.mongodb.config.connectionPool.maxSize=50",
        "spring.data.mongodb.config.connectionPool.minSize=5",
        "spring.data.mongodb.config.connectionPool.maxWaitTimeMS=1000",
        "spring.data.mongodb.config.connectionPool.maxConnectionLifeTimeMS=60000",
        "spring.data.mongodb.config.connectionPool.maxConnectionIdleTimeMS=30000",
        "spring.data.mongodb.config.connectionPool.maxConnecting=2"
})
class MongoConfigTest {

    @Autowired
    private MongoConfig.MongoDbCustomProperties mongoDbCustomProperties;


    @Test
     void testConnectionPoolSettings() {
        assertThat(mongoDbCustomProperties).isNotNull();
        assertThat(mongoDbCustomProperties.getConnectionPool()).isNotNull();

        Assertions.assertEquals(1000L, mongoDbCustomProperties.getConnectionPool().getMaxWaitTimeMS());
        Assertions.assertEquals(2, mongoDbCustomProperties.getConnectionPool().getMaxConnecting());
        Assertions.assertEquals(5L, mongoDbCustomProperties.getConnectionPool().getMinSize());
        Assertions.assertEquals(50L, mongoDbCustomProperties.getConnectionPool().getMaxSize());
        Assertions.assertEquals(60000, mongoDbCustomProperties.getConnectionPool().getMaxConnectionLifeTimeMS());
        Assertions.assertEquals(30000, mongoDbCustomProperties.getConnectionPool().getMaxConnectionIdleTimeMS());
    }

    @Test
    void testCustomizer() {
        MongoClientSettingsBuilderCustomizer customizer = new MongoConfig().customizer(mongoDbCustomProperties);

        MongoClientSettings.Builder builder = MongoClientSettings.builder();
        customizer.customize(builder);

        MongoClientSettings settings = builder.build();

        Assertions.assertEquals(mongoDbCustomProperties.getConnectionPool().getMaxSize(), settings.getConnectionPoolSettings().getMaxSize());
        Assertions.assertEquals(mongoDbCustomProperties.getConnectionPool().getMinSize(), settings.getConnectionPoolSettings().getMinSize());
        Assertions.assertEquals(mongoDbCustomProperties.getConnectionPool().getMaxWaitTimeMS(), settings.getConnectionPoolSettings().getMaxWaitTime(TimeUnit.MILLISECONDS));
        Assertions.assertEquals(mongoDbCustomProperties.getConnectionPool().getMaxConnectionLifeTimeMS(), settings.getConnectionPoolSettings().getMaxConnectionLifeTime(TimeUnit.MILLISECONDS));
        Assertions.assertEquals(mongoDbCustomProperties.getConnectionPool().getMaxConnectionIdleTimeMS(), settings.getConnectionPoolSettings().getMaxConnectionIdleTime(TimeUnit.MILLISECONDS));
        Assertions.assertEquals(mongoDbCustomProperties.getConnectionPool().getMaxConnecting(), settings.getConnectionPoolSettings().getMaxConnecting());
    }

    @Test
    void testBigDecimalToDecimal128Conversion() {
        BigDecimal bigDecimal = new BigDecimal("12345.6789");

        MongoConfig.BigDecimalDecimal128Converter converter = new MongoConfig.BigDecimalDecimal128Converter();
        Decimal128 decimal128 = converter.convert(bigDecimal);

        Assertions.assertEquals(new Decimal128(bigDecimal), decimal128);
    }

    @Test
    void testDecimal128ToBigDecimalConversion() {
        BigDecimal bigDecimal = new BigDecimal("12345.6789");
        Decimal128 decimal128 = new Decimal128(bigDecimal);

        MongoConfig.Decimal128BigDecimalConverter converter = new MongoConfig.Decimal128BigDecimalConverter();
        BigDecimal result = converter.convert(decimal128);

        Assertions.assertEquals(bigDecimal, result);
    }

    @Test
    void testOffsetDateTimeToDateConversion() {
        OffsetDateTime offsetDateTime = OffsetDateTime.now();

        MongoConfig.OffsetDateTimeWriteConverter converter = new MongoConfig.OffsetDateTimeWriteConverter();
        Date date = converter.convert(offsetDateTime);

        Assertions.assertEquals(Date.from(offsetDateTime.toInstant()), date);
    }

    @Test
    void testDateToOffsetDateTimeConversion() {
        Date date = new Date();

        MongoConfig.OffsetDateTimeReadConverter converter = new MongoConfig.OffsetDateTimeReadConverter();
        OffsetDateTime offsetDateTime = converter.convert(date);


        Assertions.assertEquals(date.toInstant().atZone(CommonConstants.ZONEID).toOffsetDateTime(), offsetDateTime);
    }


}

