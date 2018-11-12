package no.nav.helse

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit.WireMockRule
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class AuthHelperTest {

    @get: Rule
    val wireMockRule = WireMockRule(0)

    @Test
    fun `should parse a token successfully`() {
        stubFor(get(urlPathEqualTo("/rest/v1/sts/token"))
                .withQueryParam("grant_type", equalTo("client_credentials"))
                .withQueryParam("scope", equalTo("openid"))
                .withBasicAuth("foo", "bar")
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(auth_token)
                )
        )

        val token: String = AuthHelper(baseUrl = wireMockRule.baseUrl(), username = "foo", password = "bar").token()
        assertEquals("eyJraWQiOiI1MTUxODMwNy0xNGY4LTQ0ZGQtYTI5OS04ZTA5ZjkwMGVkNjUiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJzcnZzeWtlcGVuZ2ViZWhhbmQiLCJhdWQiOlsic3J2c3lrZXBlbmdlYmVoYW5kIiwicHJlcHJvZC5sb2NhbCJdLCJ2ZXIiOiIxLjAiLCJuYmYiOjE1NDIwMjQyNTcsImF6cCI6InNydnN5a2VwZW5nZWJlaGFuZCIsImlkZW50VHlwZSI6IlN5c3RlbXJlc3N1cnMiLCJhdXRoX3RpbWUiOjE1NDIwMjQyNTcsImlzcyI6Imh0dHBzOlwvXC9zZWN1cml0eS10b2tlbi1zZXJ2aWNlLXExLm5haXMucHJlcHJvZC5sb2NhbCIsImV4cCI6MTU0MjAyNzg1NywiaWF0IjoxNTQyMDI0MjU3LCJqdGkiOiIxOTFlMDg2ZS0xMDc4LTQwMGMtYTAxMi03NDk3NDk1ZTRiYTQifQ.cBL4VDiCWLH-u49DsjpCoLprrrKEsiiZDgJuFli4etQPNufdpTVRIfeLiS7wycle7aiWreqh1ADEilumvuaTtFOUs5FuaxoZXRyGEk_IfClE0I0KcwfUynFGZSg-tS_iDEEqhXhNAuogZERB2oyJDrGDbbxh2Ov8ehv4ZDs26xMVQh5FYILO0skqG_w_E6ApbnczJJLvPFa0EW_fZU6kTw5c2yq-sX-yD7L5kyBZ6skx4pAfv9w5PurvaL1IBrIX7r3Xfn1JVzxxRz-gDcxtI2iZr0yUZZFRP7v9GRdcbnEWU5yi9GTIcIh-IXdAfdmnFNupqTkSW0DIFhs-Q4YtzA",
                token)
    }

}

val auth_token = """{
  "access_token": "eyJraWQiOiI1MTUxODMwNy0xNGY4LTQ0ZGQtYTI5OS04ZTA5ZjkwMGVkNjUiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJzcnZzeWtlcGVuZ2ViZWhhbmQiLCJhdWQiOlsic3J2c3lrZXBlbmdlYmVoYW5kIiwicHJlcHJvZC5sb2NhbCJdLCJ2ZXIiOiIxLjAiLCJuYmYiOjE1NDIwMjQyNTcsImF6cCI6InNydnN5a2VwZW5nZWJlaGFuZCIsImlkZW50VHlwZSI6IlN5c3RlbXJlc3N1cnMiLCJhdXRoX3RpbWUiOjE1NDIwMjQyNTcsImlzcyI6Imh0dHBzOlwvXC9zZWN1cml0eS10b2tlbi1zZXJ2aWNlLXExLm5haXMucHJlcHJvZC5sb2NhbCIsImV4cCI6MTU0MjAyNzg1NywiaWF0IjoxNTQyMDI0MjU3LCJqdGkiOiIxOTFlMDg2ZS0xMDc4LTQwMGMtYTAxMi03NDk3NDk1ZTRiYTQifQ.cBL4VDiCWLH-u49DsjpCoLprrrKEsiiZDgJuFli4etQPNufdpTVRIfeLiS7wycle7aiWreqh1ADEilumvuaTtFOUs5FuaxoZXRyGEk_IfClE0I0KcwfUynFGZSg-tS_iDEEqhXhNAuogZERB2oyJDrGDbbxh2Ov8ehv4ZDs26xMVQh5FYILO0skqG_w_E6ApbnczJJLvPFa0EW_fZU6kTw5c2yq-sX-yD7L5kyBZ6skx4pAfv9w5PurvaL1IBrIX7r3Xfn1JVzxxRz-gDcxtI2iZr0yUZZFRP7v9GRdcbnEWU5yi9GTIcIh-IXdAfdmnFNupqTkSW0DIFhs-Q4YtzA",
  "token_type": "Bearer",
  "expires_in": 3600
}""".trimIndent()