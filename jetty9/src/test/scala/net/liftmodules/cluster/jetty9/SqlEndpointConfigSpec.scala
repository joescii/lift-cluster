package net.liftmodules.cluster.jetty9

import org.scalatest.{Matchers, WordSpec}

class SqlEndpointConfigSpec extends WordSpec with Matchers {
  "The SqlEndpointConfig.forMySQL method" should {
    "construct a valid MySQL URL string without extra parameters" in {
      val actual = SqlEndpointConfig.forMySQL("the_host", 3000, "jetty_sessions", "jetty", "secret").endpoint
      actual shouldEqual "jdbc:mysql://the_host:3000/jetty_sessions?user=jetty&password=secret"
    }

    "construct a valid MySQL URL string with extra parameters" in {
      val actual = SqlEndpointConfig.forMySQL("the_host", 3000, "jetty_sessions", "jetty", "secret", "createDatabaseIfNotExist" -> "true", "useSSL" -> "true").endpoint
      actual shouldEqual "jdbc:mysql://the_host:3000/jetty_sessions?user=jetty&password=secret&createDatabaseIfNotExist=true&useSSL=true"
    }

    "construct a valid MySQL URL string with useSSL appended" in {
      val actual = SqlEndpointConfig.forMySQL("the_host", 3000, "jetty_sessions", "jetty", "secret").withUseSSL().endpoint
      actual shouldEqual "jdbc:mysql://the_host:3000/jetty_sessions?user=jetty&password=secret&useSSL=true"
    }

    "construct a valid MySQL URL string with createDatabaseIfNotExist appended" in {
      val actual = SqlEndpointConfig.forMySQL("the_host", 3000, "jetty_sessions", "jetty", "secret").withCreateDatabaseIfNotExist(false).endpoint
      actual shouldEqual "jdbc:mysql://the_host:3000/jetty_sessions?user=jetty&password=secret&createDatabaseIfNotExist=false"
    }
  }
}
