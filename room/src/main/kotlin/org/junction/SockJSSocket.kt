package org.junction

import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.ext.bridge.BridgeEventType
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import org.jboss.logging.Logger
import org.junction.EventBusAddress.MESSAGES
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.event.Observes
import javax.inject.Inject

@ApplicationScoped
class SockJSSocket {

    @Inject
    lateinit var vertx: Vertx

    @Inject
    lateinit var logger: Logger

    fun init(@Observes router: Router) {
        val corsHandler = CorsHandler.create()
            .addOrigin("http://localhost:8000")
            .allowedMethod(HttpMethod.GET)
            .allowedMethod(HttpMethod.POST)
            .allowedMethod(HttpMethod.PUT)
            .allowedMethod(HttpMethod.DELETE)
            .allowedHeader("X-Requested-With")
            .allowedHeader("Access-Control-Allow-Origin")
            .allowedHeader("origin")
            .allowedHeader("Content-Type")
            .allowedHeader("accept")
            .allowedHeader("X-PINGARUNER")
            .allowCredentials(true)


        val sockJSHandler: SockJSHandler = SockJSHandler.create(vertx)
        logger.info("sockJSHandler created: $sockJSHandler")
        sockJSHandler.bridge(
            SockJSBridgeOptions().addOutboundPermitted(PermittedOptions().setAddress(MESSAGES)), handler
        )
        router.route("/eventbus/*")
            .handler(corsHandler)
            .subRouter(sockJSHandler.socketHandler {
                val writeHandlerID = it.writeHandlerID()
                logger.info("writeHandlerID: $writeHandlerID")
            })
    }


    private val handler: (BridgeEvent) -> Unit = {
        val result = when (it.type()) {
            BridgeEventType.SOCKET_CREATED -> {
                // todo count active connections
                logger.info("SOCKET_CREATED")
                true
            }

            BridgeEventType.SOCKET_IDLE -> {
                // todo count active connections
                logger.info("SOCKET_IDLE")
                true
            }

            BridgeEventType.SOCKET_CLOSED -> {
                // todo count active connections
                logger.info("SOCKET_CLOSED")
                true
            }
            // when sending data to web client
            BridgeEventType.RECEIVE -> {
                // todo different room
                true
            }

            else -> true
        }
        it.complete(result)
    }


}