package no.nav.helse

import com.github.kittinunf.fuel.httpGet
import org.json.JSONObject

class AktørregisterClient(val baseUrl: String, val authHelper: AuthHelper) {
    fun gjeldendeIdent(fnr: String): String {
        val bearer = authHelper.token()

        val (_, _, result) = "$baseUrl/aktoerregister/api/v1/identer?gjeldende=true&identgruppe=AktoerId".httpGet()
                .header(mapOf(
                        "Authorization" to "Bearer " + bearer,
                        "Accept" to "application/json",
                        "Nav-Call-Id" to "anything",
                        "Nav-Consumer-Id" to "sykepengebehandling",
                        "Nav-Personidenter" to fnr
                ))
                .responseString()

        val json = JSONObject(result.get())

        val identObj = json.getJSONObject(fnr)
        val identer = identObj.getJSONArray("identer")
        assert(identer.length() == 1)

        val aktørIdent = identer[0] as JSONObject
        assert(aktørIdent.getString("identgruppe").equals("AktoerId"))
        assert(aktørIdent.getBoolean("gjeldende"))

        return aktørIdent.getString("ident")
    }
}
