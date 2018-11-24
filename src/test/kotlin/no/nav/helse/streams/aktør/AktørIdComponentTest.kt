package no.nav.helse.streams.aktør;

import com.github.kittinunf.fuel.httpGet
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.stubbing.Scenario
import io.prometheus.client.CollectorRegistry
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.helse.Environment
import no.nav.helse.streams.JsonDeserializer
import no.nav.helse.streams.JsonSerializer
import no.nav.helse.streams.Topics
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SaslConfigs
import org.json.JSONObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.*
import kotlin.test.assertEquals

class AktørIdComponentTest {
    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())
        private const val username = "srvkafkaclient"
        private const val password = "kafkaclient"

        val embeddedEnvironment = KafkaEnvironment(
                users = listOf(JAASCredential(username, password)),
                autoStart = false,
                withSchemaRegistry = false,
                withSecurity = true,
                topics = listOf(Topics.SYKEPENGEBEHANDLING.name)
        )

        @BeforeAll
        @JvmStatic
        fun setup() {
            CollectorRegistry.defaultRegistry.clear()
            server.start()
            embeddedEnvironment.start()
        }

        @AfterAll
        @JvmStatic
        fun teardown() {
            server.stop()
            embeddedEnvironment.tearDown()
        }
    }

    @BeforeEach
    fun configure() {
        WireMock.configureFor(server.port())
    }


    @Test
    fun `embedded kafka cluster is up and running`() {
        assertEquals(embeddedEnvironment.serverPark.status, KafkaEnvironment.ServerParkStatus.Started)
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
        producer.send(ProducerRecord(Topics.SYKEPENGEBEHANDLING.name, null, message))
        producer.flush()
    }

    @Test
    fun `should put norskIdent on message`() {
        val env = Environment(
                username = username,
                password = password,
                bootstrapServersUrl = embeddedEnvironment.brokersURL,
                stsBaseUrl = server.baseUrl(),
                aktørregisterUrl = server.baseUrl()
        )

        WireMock.stubFor(stsRequestMapping
                .willReturn(WireMock.ok(auth_token))
                .inScenario("default")
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo("auth token acquired"))

        WireMock.stubFor(aktørregisterRequestMapping
                .willReturn(WireMock.ok(aktørregisterResponse))
                .inScenario("default")
                .whenScenarioStateIs("auth token acquired")
                .willSetStateTo("norskIdent hentet"))

        val aktørIdStream = AktørIdStream(env)
        aktørIdStream.start()

        produceOneMessage(JSONObject("{\"aktorId\": \"1573082186699\"}"))

        val resultConsumer = KafkaConsumer<String, JSONObject>(consumerProperties())
        resultConsumer.subscribe(listOf(Topics.SYKEPENGEBEHANDLING.name))
        val consumerRecords = resultConsumer.poll(Duration.ofSeconds(10))

        assertEquals(1, consumerRecords.count())

        val record = consumerRecords.records(Topics.SYKEPENGEBEHANDLING.name).elementAt(0)
        assertEquals("12345678911", record.value().getString("norskIdent"))

        verifyMetrics()

        aktørIdStream.stop()
    }

    private fun verifyMetrics() {
        val (_, response, _) = "http://localhost:8080/metrics".httpGet().response()
        val aktorRegex = ".*state=\"(.*)\".* (\\d*\\.\\d*)$".toRegex()
        val counters = String(response.data)
                .lines()
                .filter { it.startsWith("aktor_id_stream_counter{state=\"") }
                .map { aktorRegex.matchEntire(it)!! }
                .map { Pair(first = it.groupValues[1], second = it.groupValues[2]) }
                .toMap()
        assertEquals("1.0", counters["accepted"])
        assertEquals("1.0", counters["success"])
    }
}

private val stsRequestMapping = WireMock.get(WireMock.urlPathEqualTo("/rest/v1/sts/token"))
        .withQueryParam("grant_type", WireMock.equalTo("client_credentials"))
        .withQueryParam("scope", WireMock.equalTo("openid"))
        .withBasicAuth("srvkafkaclient", "kafkaclient")
        .withHeader("Accept", WireMock.equalTo("application/json"))

private val auth_token = """{
  "access_token": "foobar",
  "token_type": "Bearer",
  "expires_in": 3600
}""".trimIndent()

private val aktørregisterRequestMapping = WireMock.get(WireMock.urlPathEqualTo("/api/v1/identer"))
        .withQueryParam("gjeldende", WireMock.equalTo("true"))
        .withQueryParam("identgruppe", WireMock.equalTo("NorskIdent"))
        .withHeader("Authorization", WireMock.equalTo("Bearer foobar"))
        .withHeader("Nav-Call-Id", WireMock.equalTo("anything"))
        .withHeader("Nav-Consumer-Id", WireMock.equalTo("spinne"))
        .withHeader("Nav-Personidenter", WireMock.equalTo("1573082186699"))
        .withHeader("Accept", WireMock.equalTo("application/json"))

private val aktørregisterResponse = """
{
  "1573082186699": {
    "identer": [
      {
        "ident": "12345678911",
        "identgruppe": "NorskIdent",
        "gjeldende": true
      }
    ],
    "feilmelding": null
  }
}
""".trimIndent()
