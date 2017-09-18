package net.liftmodules.cluster

import net.liftweb.common.{Box, Empty, Loggable}
import net.liftweb.http.{LiftRules, LiftSession}
import net.liftweb.http.js.JsCmds

trait SerializableLiftSession extends Serializable {
  def session: LiftSession
}

case class LiftClusterConfig(serializer: LiftSession => SerializableLiftSession)

object LiftCluster extends Loggable {
  def init(config: LiftClusterConfig): Unit = {
    LiftRules.redirectAsyncOnSessionLoss = false
    LiftRules.noCometSessionCmd.default.set(() => JsCmds.Run("lift.rehydrateComets()"))

    LiftRules.sessionCreator = {
      case (httpSession, contextPath) =>
        (Box !! (httpSession.attribute("net.liftweb.http.LiftSession")))
          .asA[SerializableLiftSession]
          .map { ser =>
            logger.debug(s"LiftSession restored for container session ID ${httpSession.sessionId}")
            ser.session
          }
          .openOr {
            logger.debug(s"New LiftSession created for container session ID ${httpSession.sessionId}")
            new LiftSession(contextPath, httpSession.sessionId, Empty)
          }
    }

    LiftRules.afterSend.append((res, httpRes, headers, maybeReq) => {
      maybeReq.foreach { request =>
        val session = LiftRules.getLiftSession(request)
        session.httpSession.foreach(_.setAttribute("net.liftweb.http.LiftSession", config.serializer(session)))
        logger.debug("Placing LiftSession in container for session ID " + session.httpSession.map(_.sessionId))
      }
    })
  }
}
