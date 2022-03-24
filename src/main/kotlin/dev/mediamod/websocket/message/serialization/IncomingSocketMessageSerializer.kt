package dev.mediamod.websocket.message.serialization

import dev.mediamod.websocket.message.impl.incoming.IncomingSocketMessage
import dev.mediamod.websocket.message.impl.incoming.impl.IncomingHandshakeMessage
import dev.mediamod.websocket.message.impl.incoming.impl.IncomingHeartbeatMessage
import dev.mediamod.websocket.message.impl.incoming.impl.IncomingTrackMessage
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.*

object IncomingSocketMessageSerializer :
    JsonContentPolymorphicSerializer<IncomingSocketMessage>(IncomingSocketMessage::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out IncomingSocketMessage> =
        when (val id = element.contentOrNull("id")) {
            "HANDSHAKE" -> IncomingHandshakeMessage.serializer()
            "HEARTBEAT" -> IncomingHeartbeatMessage.serializer()
            "TRACK" -> IncomingTrackMessage.serializer()
            else -> error("Unknown incoming message id: $id")
        }

    private fun JsonElement.contentOrNull(key: String) =
        jsonObject[key]?.jsonPrimitive?.contentOrNull
}