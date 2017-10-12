package net.liftmodules.cluster.jetty9

import java.net.URI

import net.liftweb.common.{Box, Failure, Full}

/**
  * Config object for jetty 9
  * @param port port number in which to run the app
  * @param host optional hostname/ip to listen to. Default is ultimately 127.0.0.1. Pass 0.0.0.0 if running in docker
  * @param contextPath the root context path to make your application available at
  * @param webappPath path to the target or source webapp directory, allowing embedded jetty to run locally
  * @param clusterConfig optional configuration for clustering
  * @param patienceConfig how patient we will be trying to start the server when Jetty throws us an exception
  */
case class Jetty9Config (
  port: Int = 8080,
  host: Option[String] = None,
  contextPath: String = "/",
  webappPath: String = "target/webapp",
  clusterConfig: Option[Jetty9ClusterConfig] = None,
  patienceConfig: PatienceConfig = PatienceConfig(60, 1000)
)

/**
  * Config object for jetty 9 clustering
  * @param workerName name for the node/worker, must be unique within cluster!
  * @param jdbcDriver jdbc driver for the cluster's backend store
  * @param sqlEndpointConfig jdbc endpoint configuration
  */
case class Jetty9ClusterConfig (
  workerName: String,
  jdbcDriver: SqlDriver,
  sqlEndpointConfig: SqlEndpointConfig
)

case class PatienceConfig(attempts: Int, millisBetweenAttempts: Int)

/**
  * Config object for SQL endpoints
  */
trait SqlEndpointConfig {
  /**
    * Full URL endpoint string including the 'jdbc:' prefix
    */
  def endpoint: String
}

object SqlEndpointConfig {

  /**
    * Fully-specified MySQL endpoint
    * @param host the SQL server hostname
    * @param port the SQL server port
    * @param dbName the database to use for jetty clustering tables
    * @param user the SQL db username
    * @param password the SQL db password
    */
  def forMySQL(host: String, port: Int, dbName: String, user: String, password: String, extraParam: (String, String)*): MySQLEndpointConfig =
    MySQLEndpointConfig(host, port, dbName, user, password, extraParam: _*)

  /**
    * Digs around in the system ENV to give you the correct config while running in Heroku.
    * Currently only their MySQL equivalent ClearDB is supported.
    */
  def forHeroku: Box[SqlEndpointConfig] = if(System.getenv("CLEARDB_DATABASE_URL") == null) {
    Failure("Could not find environment variable CLEARDB_DATABASE_URL")
  } else {
    val dbUri = new URI(System.getenv("CLEARDB_DATABASE_URL"))
    val username = dbUri.getUserInfo.split(":")(0)
    val password = dbUri.getUserInfo.split(":")(1)

    Full(new SqlEndpointConfig {
      override val endpoint: String = s"jdbc:mysql://${dbUri.getHost}${dbUri.getPath}?user=$username&password=$password&${dbUri.getQuery}"
    })
  }
}

case class MySQLEndpointConfig(host: String, port: Int, dbName: String, user: String, password: String, extraParam: (String, String)*) extends SqlEndpointConfig {
  private[this] val params: String = (("user" -> user) :: ("password" -> password) :: extraParam.toList)
    .map { case (k, v) => k + "=" + v }
    .mkString("&")

  override val endpoint: String = s"jdbc:mysql://$host:$port/$dbName?$params"

  def withParam(key: String, value: String): MySQLEndpointConfig = MySQLEndpointConfig(host, port, dbName, user, password, (this.extraParam :+ (key -> value)):_*)

  def withUseSSL(ssl: Boolean = true): MySQLEndpointConfig = withParam("useSSL", ssl.toString)
  def withCreateDatabaseIfNotExist(create: Boolean = true): MySQLEndpointConfig = withParam("createDatabaseIfNotExist", create.toString)
}

/**
  * Jdbc SQL driver
  */
sealed trait SqlDriver
case object DriverMySQL extends SqlDriver {
  override def toString: String = "com.mysql.jdbc.Driver"
}
case object DriverMariaDB extends SqlDriver {
  override def toString: String = "org.mariadb.jdbc.Driver"
}
case class DriverOther(className: String) extends SqlDriver {
  override def toString: String = className
}
