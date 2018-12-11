package no.nav.helse

import com.github.kittinunf.fuel.httpGet
import io.prometheus.client.CollectorRegistry
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.helse.streams.*
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.json.JSONObject
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.time.*
import java.util.*

class ComponentTest {

    companion object {
        private const val username = "srvkafkaclient"
        private const val password = "kafkaclient"

        val embeddedEnvironment = KafkaEnvironment(
                users = listOf(JAASCredential(username, password)),
                autoStart = false,
                withSchemaRegistry = false,
                withSecurity = true,
                topics = listOf(Topics.SYKEPENGEBEHANDLING.name, Topics.SYKEPENGESØKNADER_INN.name)
        )

        private val env = Environment(username = username,
                password = password,
                bootstrapServersUrl = embeddedEnvironment.brokersURL)

        @BeforeAll
        @JvmStatic
        fun setup() {
            CollectorRegistry.defaultRegistry.clear()
            embeddedEnvironment.start()
        }

        @AfterAll
        @JvmStatic
        fun teardown() {
            embeddedEnvironment.tearDown()
        }
    }

    @Test
    fun `embedded kafka cluster is up and running`() {
        assertEquals(embeddedEnvironment.serverPark.status, KafkaEnvironment.ServerParkStatus.Started)
    }

    @Test
    fun `message from syfo is forwarded as is to behandling topic`() {
        val spinne = Spinne(env)
        spinne.start()

        val msgSent = JSONObject("""{"aktorId": "1573082186699", "soknadstype": "typen", "status": "sendt"}""")
        produceOneMessage(msgSent)

        val resultConsumer = KafkaConsumer<String, JSONObject>(consumerProperties())
        resultConsumer.subscribe(listOf(Topics.SYKEPENGEBEHANDLING.name))
        val consumerRecords = resultConsumer.poll(Duration.ofSeconds(10))

        assertEquals(1, consumerRecords.count())

        val msgRead = consumerRecords.records(Topics.SYKEPENGEBEHANDLING.name).first().value()
        msgRead.keySet().forEach { propName ->
            assertEquals(msgSent[propName], msgRead[propName])
        }

        verifyMetrics()
        spinne.stop()
    }

    private fun producerProperties(): Properties {
        return Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedEnvironment.brokersURL)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer::class.java)
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";")
        }
    }

    private fun consumerProperties(): Properties {
        return Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedEnvironment.brokersURL)

            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer::class.java)
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$username\" password=\"$password\";")
            put(ConsumerConfig.GROUP_ID_CONFIG, "spinne-test-verification")
        }
    }

    private fun produceOneMessage(message: JSONObject) {
        val producer = KafkaProducer<String, JSONObject>(producerProperties())
        producer.send(ProducerRecord(Topics.SYKEPENGESØKNADER_INN.name, null, message))
        producer.flush()
    }

    private fun verifyMetrics() {
        val (_, response, _) = "http://localhost:8080/metrics".httpGet().response()
        val aktorRegex = ".*status=\"(.*)\".* (\\d*\\.\\d*)$".toRegex()
        val counters = String(response.data)
                .lines()
                .filter { it.startsWith("sykepenger_mottatte_soknader{type=") }
                .map { aktorRegex.matchEntire(it)!! }
                .map { Pair(first = it.groupValues[1], second = it.groupValues[2]) }
                .toMap()
        Assertions.assertEquals("1.0", counters["sendt"])
    }
}

