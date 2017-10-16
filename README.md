# lift-cluster

The Lift modules in this project allow Lift utilize a clustered servlet container. 

## Motivation

By design, a page served by Lift can only be further serviced with Ajax calls and comet updates by the application server instance which served the page.
While this provides a strong layer of security for Lift applications, it has a drawback that if the Lift application server were to fail, all pages served must be reloaded against a new instance.

This project provides tooling to deploy Lift in a clustered mode which allows peer instances of the Lift application server to service pages served by a failed instance.
Please read [Deployment Considerations](#deployment-considerations) below for the implications of running Lift in this manner.

### How These Modules Solve the Problem

Firstly, the `lift-cluster-jetty9` module gives you a configurable bootstrap for running Jetty 9 embedded in your Lift application.
This bootstrap can be optionally configured for Jetty clustering, which is implemented by storing session state in a SQL DB.

Secondly, the `lift-cluster-kryo` library utilizes [Twitter chill](https://github.com/twitter/chill) which in turn uses [Kryo](https://github.com/EsotericSoftware/kryo) for serializing the `LiftSession` object into the container's session.

With both modules configured for clustering, a Lift server can get killed and replaced while continuing to serve existing pages with the `LiftSession` deserialized from the cluster.

Other containers, backend stores, and serialization libraries can readily be supported in the existing design of this solution.

## Configuration

You can configure an existing Lift project to utilize container clustering with the `lift-cluster-kryo` and `lift-cluster-jetty9`.

### Build Configuration

Add both `lift-cluster-kryo` and `lift-cluster-jetty9` as dependencies in your `build.sbt`.

```scala
libraryDependencies ++= {
  val liftVersion = "3.2.0-SNAPSHOT" 
  val liftEdition = liftVersion.value.replaceAllLiterally("-SNAPSHOT", "").split('.').take(2).mkString(".")
  val clusterVersion = "0.0.2"
  Seq(
    // Other dependencies ...
    "net.liftmodules"   %% ("lift-cluster-kryo_"+liftEdition)   % clusterVersion % "compile",
    "net.liftmodules"   %% ("lift-cluster-jetty9_"+liftEdition) % clusterVersion % "compile"
   )
}
```

### Jetty Cluster Configuration

First you need to define an entry point (i.e. a `main` method) which can configure and start Jetty.
See the below simplistic example.

```scala
import net.liftmodules.cluster.jetty9._
import net.liftweb.common.Loggable
import net.liftweb.util.{LoggingAutoConfigurer, Props, StringHelpers}

import util.Properties

object Start extends App with Loggable {
  startLift()

  def startLift(): Unit = {
    LoggingAutoConfigurer().apply()
    
    val endpoint = Some(SqlEndpointConfig.forMySQL("host", 3306, "my_db", "user", "password", "extra" -> "param"))
    val clusterConfig = Some(Jetty9ClusterConfig(
      workerName = "node1",
      jdbcDriver = DriverMariaDB,
      sqlEndpointConfig = endpoint
    ))

    val startConfig = Jetty9Config(
      port = port,
      contextPath = contextPath,
      clusterConfig = clusterConfig
    )

    Jetty9Starter.start(startConfig)
  }

}
```

### Lift Cluster Configuration

Secondly, you need to configure and initialize `LiftCluster` in your `Boot` class

```scala
package bootstrap.liftweb

class Boot {
  def boot {
    // Other stuff...
    
    val clusterConfig = LiftClusterConfig(
      serializer = KryoSerializableLiftSession.serializer 
    )
    
    LiftCluster.init(clusterConfig)
  }
}
```

## Development and Testing Considerations

Utilizing lift-cluster places some constraints on the way you can write your Lift application code.
The two primary considerations are session serialization/deserialization and comet rehydration.

### Serialization

The lift-cluster-kryo module serializes each `LiftSession` instance in the servlet container.
(More precisely, we wrap the `LiftSession` instance such that when Java serialization is utilized by the container, we use Kryo to do the dirty work).

TODO: Watch for serialization exceptions

### Deserialization

TODO: Watch for null pointer exceptions

### Comet Rehydration

TODO: Beware of state in comets

## Deployment Considerations

TODO DB-per cluster 

TODO Canary releases 

## Community/Support

Need help?  Hit us up on the [Lift Google group](https://groups.google.com/forum/#!forum/liftweb).
We'd love to help you out and hear about what you're building.

## Contributing

As with any open source project, contributions are greatly appreciated.
If you find an issue or have a feature idea, we'd love to know about it!
Any of the following will help this effort tremendously.

1. Issue a Pull Request with the fix/enhancement and unit tests to validate the changes.  OR
3. [Open an issue](https://github.com/joescii/lift-cluster/issues/new) to let us know about what you've discovered.

### Pull Requests

Below is the recommended procedure for git:

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push the branch (`git push origin my-new-feature`)
5. Create new Pull Request

Please include as much as you are able, such as tests, documentation, updates to this README, etc.

## Change log
* *0.0.1*: First release which seems to work.
Modules affected: `lift-cluster-common`, `lift-cluster-kryo`, and `lift-cluster-jetty9`.
* *0.0.2*: Added `PatienceConfig` and `host` as config parameters for `JettyStart`.
Modules affected: `lift-cluster-jetty9`.

## Credits
This project and updates to the Lift project were developed in collaboration with and support from [Fractal Industries Inc](http://www.fractalindustries.com/).

## License

**lift-cluster** is licensed under [APL 2.0](http://www.apache.org/licenses/LICENSE-2.0).

Copyright 2017 net.liftweb

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.


