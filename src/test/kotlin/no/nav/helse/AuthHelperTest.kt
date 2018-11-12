package no.nav.helse

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class AuthHelperTest {

    @get: Rule
    val wireMockRule = WireMockRule(0)

    @Test
    fun `should parse a token successfully`() {
        stubFor(stsRequestMapping
                .willReturn(ok(auth_token))
                .inScenario("default")
                .whenScenarioStateIs(STARTED)
                .willSetStateTo("token acquired"))

        val token: String = AuthHelper(baseUrl = wireMockRule.baseUrl(), username = "foo", password = "bar").token()
        assertEquals("default access token", token)
    }

    @Test
    fun `should cache tokens`() {
        stubFor(stsRequestMapping
                .willReturn(ok(auth_token))
                .inScenario("caching")
                .whenScenarioStateIs(STARTED)
                .willSetStateTo("token acquired"))

        stubFor(stsRequestMapping
                .willReturn(ok(bad_token))
                .inScenario("caching")
                .whenScenarioStateIs("token acquired")
        )

        val authHelper = AuthHelper(baseUrl = wireMockRule.baseUrl(), username = "foo", password = "bar")
        authHelper.token()

        val token: String = authHelper.token()
        assertEquals("default access token", token)
    }

    @Test
    fun `should get new token when old has expired`() {
        stubFor(stsRequestMapping
                .willReturn(ok(short_lived_token))
                .inScenario("expiry")
                .whenScenarioStateIs(STARTED)
                .willSetStateTo("expired token sent"))

        stubFor(stsRequestMapping
                .willReturn(ok(auth_token))
                .inScenario("expiry")
                .whenScenarioStateIs("expired token sent")
        )

        // get the short-lived one
        AuthHelper(baseUrl = wireMockRule.baseUrl(), username = "foo", password = "bar").token()
        Thread.sleep(1050)

        // get the new one
        val token: String = AuthHelper(baseUrl = wireMockRule.baseUrl(), username = "foo", password = "bar").token()
        assertEquals("default access token", token)
    }

}

val stsRequestMapping: MappingBuilder = get(urlPathEqualTo("/rest/v1/sts/token"))
        .withQueryParam("grant_type", equalTo("client_credentials"))
        .withQueryParam("scope", equalTo("openid"))
        .withBasicAuth("foo", "bar")
        .withHeader("Accept", equalTo("application/json"))

val auth_token = """{
  "access_token": "default access token",
  "token_type": "Bearer",
  "expires_in": 3600
}""".trimIndent()

val short_lived_token = """{
  "access_token": "short lived token",
  "token_type": "Bearer",
  "expires_in": 1
}""".trimIndent()

val bad_token = """{
  "access_token": "this token shouldn't be requested",
  "token_type": "Bearer",
  "expires_in": 1000000000000
}""".trimIndent()