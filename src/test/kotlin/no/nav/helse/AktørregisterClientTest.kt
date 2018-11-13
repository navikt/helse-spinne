package no.nav.helse

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
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

    @BeforeEach
    fun configure() {
        WireMock.configureFor(server.port())
    }

    @Test
    fun `should return gjeldende aktørId`() {
        val accessTokenResponse = """
            {
  "access_token": "foobar",
  "token_type": "Bearer",
  "expires_in": 3600
}
        """.trimIndent()
        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/rest/v1/sts/token"))
                .withQueryParam("grant_type", WireMock.equalTo("client_credentials"))
                .withQueryParam("scope", WireMock.equalTo("openid"))
                .withBasicAuth("foo", "bar")
                .withHeader("Accept", WireMock.equalTo("application/json"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withBody(accessTokenResponse)
                )
        )

        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/aktoerregister/api/v1/identer"))
                .withQueryParam("gjeldende", WireMock.equalTo("true"))
                .withQueryParam("identgruppe", WireMock.equalTo("AktoerId"))
                .withHeader("Authorization", WireMock.equalTo("Bearer foobar"))
                .withHeader("Nav-Call-Id", WireMock.equalTo("anything"))
                .withHeader("Nav-Consumer-Id", WireMock.equalTo("sykepengebehandling"))
                .withHeader("Nav-Personidenter", WireMock.equalTo("12345678911"))
                .withHeader("Accept", WireMock.equalTo("application/json"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withBody("""
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
}""".trimIndent())
                )
        )

        val authHelper = AuthHelper(baseUrl = server.baseUrl(), username = "foo", password = "bar")

        var gjeldendeIdent = AktørregisterClient(baseUrl = server.baseUrl(), authHelper = authHelper).gjeldendeIdent("12345678911")

        Assertions.assertEquals("1573082186699", gjeldendeIdent)
    }
}
