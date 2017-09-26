package net.liftmodules.cluster.jetty9

import net.liftweb.common.Loggable
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.session.{JDBCSessionIdManager, JDBCSessionManager}
import org.eclipse.jetty.webapp.WebAppContext

/**
  * Object for starting a Lift app with Jetty 9
  */
object Jetty9Starter extends Loggable {
  def start(config: Jetty9Config): Unit = {
    logger.info("Starting Lift server...")

    val webappDir: String = Option(this.getClass.getClassLoader.getResource("webapp"))
      .map(_.toExternalForm)
      .filter(_.contains("jar:file:")) // this is a hack to distinguish in-jar mode from "expanded"
      .getOrElse("target/webapp")

    logger.debug(s"webappDir: $webappDir")

    val server = new Server(config.port)
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
    server.start()
    logger.info(s"Lift server started on port ${config.port}")
    server.join()
  }

}
