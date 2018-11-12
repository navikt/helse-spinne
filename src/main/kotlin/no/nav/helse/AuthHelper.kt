package no.nav.helse

import com.github.kittinunf.fuel.httpGet
import org.json.JSONObject

/**
 * henter jwt token fra STS
 *
 * bør sikkert gjøre en jobb med å refreshe ved expiry
 */
class AuthHelper(val baseUrl: String, val username: String, val password: String) {
    fun token():String {
        val (_, _, result) = "$baseUrl/rest/v1/sts/token?grant_type=client_credentials&scope=openid".httpGet()
                .authenticate(username, password)
                .header(mapOf("Accept" to "application/json"))
                .response()
        val asjson = JSONObject(String(result.component1()!!))
        return asjson.getString("access_token")
    }
}