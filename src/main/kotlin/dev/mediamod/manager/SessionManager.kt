package dev.mediamod.manager

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import dev.mediamod.data.api.mojang.SessionJoinRequest
import dev.mediamod.utils.json
import dev.mediamod.utils.hex
import gg.essential.universal.UMinecraft
import kotlinx.serialization.encodeToString
import java.security.MessageDigest
import java.security.SecureRandom

class SessionManager {
    private val constant = "82074fcd6eef4cafbc954dac50485fb7".encodeToByteArray()
    private val baseURL = "https://sessionserver.mojang.com"

    fun joinServer(): ByteArray? {
        val sharedSecret = generateSharedSecret()
        val serverIdHash = generateServerIdHash(sharedSecret)

        val body = SessionJoinRequest(
            UMinecraft.getMinecraft().session.accessToken,
            UMinecraft.getMinecraft().session.uuid,
            serverIdHash
        )

        val (_, response, _) = Fuel.post("${baseURL}/session/minecraft/join")
            .jsonBody(json.encodeToString(body))
            .response()

        if (response.statusCode != 204)
            return null

        return sharedSecret
    }

    private fun generateServerIdHash(secret: ByteArray): String {
        val id = secret + constant
        return sha1(id).hex()
    }

    private fun generateSharedSecret(): ByteArray {
        val random = SecureRandom()
        val bytes = ByteArray(16)

        random.nextBytes(bytes)
        return bytes
    }

    private fun sha1(input: ByteArray): ByteArray {
        val sha1 = MessageDigest.getInstance("SHA-1")
        return sha1.digest(input)
    }
}