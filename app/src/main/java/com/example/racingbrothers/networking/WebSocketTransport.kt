package com.example.racingbrothers.networking

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

interface WebSocketConnection {
    fun send(text: String): Boolean
    fun close(code: Int, reason: String)
}

interface WebSocketTransportListener {
    fun onOpen(connection: WebSocketConnection)
    fun onMessage(text: String)
    fun onClosed(code: Int, reason: String)
    fun onFailure(message: String)
}

/**
 * Abstract transport so production uses real OkHttp WSS/HTTPS against Cloudflare Workers,
 * while automated Robolectric & JVM tests can inject an in-memory multi-client broker.
 */
interface MultiplayerTransport {
    suspend fun checkServerHealth(httpBaseUrl: String): Boolean = true
    suspend fun createRoomHttp(httpBaseUrl: String): Result<String>
    fun openRoomSocket(wsUrl: String, listener: WebSocketTransportListener): WebSocketConnection
}

class OkHttpMultiplayerTransport(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()
) : MultiplayerTransport {

    private val healthClient: OkHttpClient by lazy {
        client.newBuilder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    override suspend fun checkServerHealth(httpBaseUrl: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val url = "${httpBaseUrl.trimEnd('/')}/health"
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .header("X-Protocol-Version", ProtocolConstants.PROTOCOL_VERSION.toString())
                .build()

            healthClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use false
                val rawBody = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(rawBody) }.getOrNull() ?: return@use false
                json.optString("status") == "ok" &&
                    json.optBoolean("durableObjectsBound", false)
            }
        }.getOrDefault(false)
    }

    override suspend fun createRoomHttp(httpBaseUrl: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "${httpBaseUrl.trimEnd('/')}/api/rooms"
            val body = "{}".toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .header("Accept", "application/json")
                .header("X-Protocol-Version", ProtocolConstants.PROTOCOL_VERSION.toString())
                .build()

            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val serverError = runCatching {
                        JSONObject(rawBody).optString("error", "")
                    }.getOrDefault("")
                    error(
                        if (serverError.isNotBlank()) serverError
                        else "Error del servidor (${response.code}) al crear la sala."
                    )
                }
                val json = JSONObject(rawBody)
                val code = json.optString("roomCode", "")
                if (!RoomCodeValidator.isValid(code)) {
                    error("El servidor devolvió un código de sala inválido.")
                }
                code
            }
        }
    }

    override fun openRoomSocket(
        wsUrl: String,
        listener: WebSocketTransportListener
    ): WebSocketConnection {
        val request = Request.Builder()
            .url(wsUrl)
            .header("X-Protocol-Version", ProtocolConstants.PROTOCOL_VERSION.toString())
            .build()

        lateinit var connectionRef: WebSocketConnection
        val ws = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    val conn = object : WebSocketConnection {
                        override fun send(text: String): Boolean = webSocket.send(text)
                        override fun close(code: Int, reason: String) {
                            webSocket.close(code, reason)
                        }
                    }
                    listener.onOpen(conn)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (text.length <= ProtocolConstants.MAX_MESSAGE_BYTES * 2) {
                        listener.onMessage(text)
                    }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(code, reason)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    listener.onClosed(code, reason)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    val msg = t.message ?: response?.message ?: "Error de conexión WebSocket"
                    listener.onFailure(msg)
                }
            }
        )

        connectionRef = object : WebSocketConnection {
            override fun send(text: String): Boolean = ws.send(text)
            override fun close(code: Int, reason: String) {
                ws.close(code, reason)
            }
        }
        return connectionRef
    }
}
