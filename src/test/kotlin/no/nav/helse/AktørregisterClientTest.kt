package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.stubbing.Scenario
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.*

class AktørregisterClientTest {

    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())

        @BeforeAll
        @JvmStatic
        fun start() {
            server.start()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            server.stop()
        }
    }

    private val aktørregisterClient: AktørregisterClient
    init {
        val authHelperMock = mockk<AuthHelper>()
        every {
            authHelperMock.token()
        } returns "foobar"

        aktørregisterClient = AktørregisterClient(baseUrl = server.baseUrl(), authHelper = authHelperMock)
    }

    @BeforeEach
    fun configure() {
        WireMock.configureFor(server.port())
    }

    @Test
    fun `should return gjeldende aktørId`() {
        WireMock.stubFor(aktørregisterRequestMapping
                .withQueryParam("identgruppe", WireMock.equalTo("AktoerId"))
                .withHeader("Nav-Personidenter", WireMock.equalTo("12345678911"))
                .willReturn(WireMock.ok(ok_aktoerId_response))
                .inScenario("aktørid")
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo("aktørid hentet"))

        var gjeldendeIdent = aktørregisterClient.gjeldendeAktørId("12345678911")

        Assertions.assertEquals("1573082186699", gjeldendeIdent)
    }

    @Test
    fun `should return gjeldende norsk ident`() {
        WireMock.stubFor(aktørregisterRequestMapping
                .withQueryParam("identgruppe", WireMock.equalTo("NorskIdent"))
                .withHeader("Nav-Personidenter", WireMock.equalTo("1573082186699"))
                .willReturn(WireMock.ok(ok_norskident_response))
                .inScenario("norsk_ident")
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo("norsk_ident hentet"))

        var gjeldendeIdent = aktørregisterClient.gjeldendeNorskIdent("1573082186699")

        Assertions.assertEquals("12345678911", gjeldendeIdent)
    }
}

private val aktørregisterRequestMapping = WireMock.get(WireMock.urlPathEqualTo("/api/v1/identer"))
        .withQueryParam("gjeldende", WireMock.equalTo("true"))
        .withHeader("Authorization", WireMock.equalTo("Bearer foobar"))
        .withHeader("Nav-Call-Id", WireMock.equalTo("anything"))
        .withHeader("Nav-Consumer-Id", WireMock.equalTo("sykepengebehandling"))
        .withHeader("Accept", WireMock.equalTo("application/json"))

private val ok_aktoerId_response = """
{
  "12345678911": {
    "identer": [
      {
        "ident": "1573082186699",
        "identgruppe": "AktoerId",
        "gjeldende": true
      }
    ],
    "feilmelding": null
  }
}""".trimIndent()

private val ok_norskident_response = """
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
