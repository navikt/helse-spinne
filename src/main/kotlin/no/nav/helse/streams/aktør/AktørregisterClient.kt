package no.nav.helse.streams.aktør

import com.github.kittinunf.fuel.httpGet
import no.nav.helse.AuthHelper
import org.json.JSONObject

class AktørregisterClient(val baseUrl: String, val authHelper: AuthHelper) {
    enum class IdentType {
        AktoerId, NorskIdent
    }
    fun gjeldendeIdent(ident: String, type: IdentType): String {
        val bearer = authHelper.token()

        val (_, _, result) = "$baseUrl/api/v1/identer?gjeldende=true&identgruppe=${type.name}".httpGet()
                .header(mapOf(
                        "Authorization" to "Bearer $bearer",
                        "Accept" to "application/json",
                        "Nav-Call-Id" to "anything",
                        "Nav-Consumer-Id" to "spinne",
                        "Nav-Personidenter" to ident
                ))
                .responseString()

        val json = JSONObject(result.get())

        val identObj = json.getJSONObject(ident)
        val identer = identObj.getJSONArray("identer")
        assert(identer.length() == 1)

        val aktørIdent = identer[0] as JSONObject
        assert(aktørIdent.getString("identgruppe").equals(type.name))
        assert(aktørIdent.getBoolean("gjeldende"))

        return aktørIdent.getString("ident")
    }

    fun gjeldendeAktørId(ident: String): String {
        return gjeldendeIdent(ident, IdentType.AktoerId)
    }

    fun gjeldendeNorskIdent(ident: String): String {
        return gjeldendeIdent(ident, IdentType.NorskIdent)
    }
}
