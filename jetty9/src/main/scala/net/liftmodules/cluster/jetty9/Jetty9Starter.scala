package net.liftmodules.cluster.jetty9

import java.net.InetSocketAddress

import net.liftweb.common.Loggable
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.session.{JDBCSessionIdManager, JDBCSessionManager}
import org.eclipse.jetty.webapp.WebAppContext

import scala.util.{Failure, Try}

/**
  * Object for starting a Lift app with Jetty 9
  */
object Jetty9Starter extends Loggable {
  def start(config: Jetty9Config): Unit = {
    logger.info("Starting Lift server...")

    val webappDir: String = Option(this.getClass.getClassLoader.getResource("webapp"))
      .map(_.toExternalForm)
      .filter(_.contains("jar:file:")) // this is a hack to distinguish in-jar mode from "expanded"
      .getOrElse(config.webappPath)

    logger.debug(s"webappDir: $webappDir")

    val server = config.host.map { host =>
      val addr = new InetSocketAddress(host, config.port)
      new Server(addr)
    }.getOrElse( new Server(config.port) )
    val context = new WebAppContext(webappDir, config.contextPath)

    config.clusterConfig.foreach { clusterConfig =>
      logger.info(s"Worker/Node name: ${clusterConfig.workerName}")

      val endpoint = clusterConfig.sqlEndpointConfig.endpoint

      val idMgr = new JDBCSessionIdManager(server)
      idMgr.setWorkerName(clusterConfig.workerName)
      idMgr.setDriverInfo(clusterConfig.jdbcDriver.toString, endpoint)
      idMgr.setScavengeInterval(60)
      idMgr.setBlobType("LONGBLOB")
      server.setSessionIdManager(idMgr)

      val jdbcMgr = new JDBCSessionManager()
      jdbcMgr.setSessionIdManager(server.getSessionIdManager())
      context.getSessionHandler().setSessionManager(jdbcMgr)
    }

    server.setHandler(context)

    val attempts = Stream.from(1).takeWhile(_ <= config.patienceConfig.attempts)
      .map { attemptNumber =>
        val attempt = Try(server.start())
        attempt.failed.foreach { ex =>
          logger.info(s"Attempt number $attemptNumber of ${config.patienceConfig.attempts} to start jetty failed.")
          logger.debug("The exception", ex)
          Thread.sleep(config.patienceConfig.millisBetweenAttempts)
        }
        attempt
      }

    val firstSuccess = attempts.find(_.isSuccess)

    firstSuccess match {
      case Some(_) =>
        logger.info(s"Lift server started on port ${config.port}")
        server.join()
      case _ =>
        logger.error(s"Exhausted ${config.patienceConfig.attempts} attempts to start Jetty!")
        attempts.zipWithIndex.collect {
          case (Failure(ex), i) => logger.error(s"Exception from attempt $i", ex)
        }
    }
  }

}
