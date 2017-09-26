package net.liftmodules.cluster.jetty9

import java.net.URI

import net.liftweb.common.{Box, Failure, Full}

/**
  * Config object for jetty 9
  * @param port port number in which to run the app
  * @param contextPath the root context path to make your application available at
  * @param clusterConfig optional configuration for clustering
  */
case class Jetty9Config (
  port: Int,
  contextPath: String,
  clusterConfig: Option[Jetty9ClusterConfig]
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
    * Fully-specified SQL endpoint
    * @param vendor the SQL vendor such as MySQL, Postgres, etc
    * @param host the SQL server hostname
    * @param port the SQL server port
    * @param dbName the database to use for jetty clustering tables
    * @param user the SQL db username
    * @param password the SQL db password
    */
  def apply(vendor: SqlVendor, host: String, port: Int, dbName: String, user: String, password: String): SqlEndpointConfig = new SqlEndpointConfig() {
    override def endpoint: String = s"jdbc:$vendor://$host:$port/$dbName?user=$user&password=$password"
  }

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
      override def endpoint: String = s"jdbc:$VendorMysql://${dbUri.getHost}${dbUri.getPath}?user=$username&password=$password&${dbUri.getQuery}"
    })
  }
}

/**
  * A SQL Vendor such as MySQL, etc
  */
sealed trait SqlVendor
case object VendorMysql extends SqlVendor {
  override def toString: String = "mysql"
}
case class VendorOther(name: String) extends SqlVendor {
  override def toString: String = name
}

/**
  * Jdbc SQL driver
  */
sealed trait SqlDriver
case object DriverMysql extends SqlDriver {
  override def toString: String = "com.mysql.jdbc.Driver"
}
case object DriverMariaDB extends SqlDriver {
  override def toString: String = "org.mariadb.jdbc.Driver"
}
case class DriverOther(className: String) extends SqlDriver {
  override def toString: String = className
}
