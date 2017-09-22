package net.liftmodules.cluster

import java.util.concurrent.ConcurrentHashMap

import net.liftweb.common.{Box, Full, Loggable}
import net.liftweb.http.{LiftRules, LiftSession}
import net.liftweb.http.js.JsCmds
import net.liftweb.http.provider.HTTPSession

trait SerializableLiftSession extends Serializable {
  def session: LiftSession
}

case class LiftClusterConfig(serializer: LiftSession => SerializableLiftSession)

object LiftCluster extends Loggable {
  def init(config: LiftClusterConfig): Unit = {
    LiftRules.redirectAsyncOnSessionLoss = false
    LiftRules.noCometSessionCmd.default.set(() => JsCmds.Run("lift.rehydrateComets()"))

    LiftRules.sessionCreator = {
      case (httpSession, contextPath) => {
        SessionMaster.addHttpSession(httpSession)

        (Box !! (httpSession.attribute("net.liftweb.http.LiftSession")))
          .asA[SerializableLiftSession]
          .map { ser =>
            logger.debug(s"LiftSession restored for container session ID ${httpSession.sessionId}")
            ser.session
          }
          .openOr {
            logger.debug(s"New LiftSession created for container session ID ${httpSession.sessionId}")
            new LiftSession(contextPath, httpSession.sessionId, Full(httpSession))
          }
      }
    }

    LiftRules.afterSend.append((res, httpRes, headers, maybeReq) => {
      maybeReq.foreach { request =>
        val session = LiftRules.getLiftSession(request)
        session.httpSession.foreach(_.setAttribute("net.liftweb.http.LiftSession", config.serializer(session)))
        logger.debug("Placing LiftSession in container for session ID " + session.httpSession.map(_.sessionId))
      }
    })
    
    LiftSession.onShutdownSession :+= {session: LiftSession => SessionMaster.removeHttpSession(session.underlyingId)}
  }
}

object SessionMaster {
  private val httpSessions: ConcurrentHashMap[String, HTTPSession] = new ConcurrentHashMap()

  def addHttpSession(httpSession: HTTPSession): Unit = httpSessions.put(httpSession.sessionId, httpSession)
  def getHttpSession(id: String): Box[HTTPSession] = Box.legacyNullTest(httpSessions.get(id))
  def removeHttpSession(id: String): Unit = httpSessions.remove(id)
}
