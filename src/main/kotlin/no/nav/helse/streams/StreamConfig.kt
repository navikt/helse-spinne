package no.nav.helse.streams

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.errors.LogAndFailExceptionHandler
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

private val log = LoggerFactory.getLogger("StreamConfig")

fun streamConfig(
        appId: String,
        env: Environment
): Properties = Properties().apply {
    put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, env.bootstrapServersUrl)
    put(StreamsConfig.APPLICATION_ID_CONFIG, appId)

    put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndFailExceptionHandler::class.java)

    env.username?.let {
        log.info("Using user name ${it} to authenticate against Kafka brokers ")
        put(SaslConfigs.SASL_MECHANISM, "PLAIN")
        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
        put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${it}\" password=\"${env.password}\";")
    }

    env.navTruststorePath?.let {
        try {
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
            put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, File(it).absolutePath)
            put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, env.navTruststorePassword)
            log.info("Configured '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location ")
        } catch (ex: Exception) {
            log.error("Failed to set '${SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG}' location", ex)
        }
    }
}

