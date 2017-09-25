package net.liftmodules.cluster.kryo

import com.twitter.chill._
import net.liftmodules.cluster.SessionMaster
import net.liftweb.http.provider.HTTPSession
import _root_.java.util.{ ResourceBundle, Locale }

class LiftInstantiator extends ScalaKryoInstantiator {
  override def newKryo(): KryoBase = {
    val k = super.newKryo()
    (new LiftRegistrar).apply(k)
    k
  }
}

class LiftRegistrar extends IKryoRegistrar {
  override def apply(k: Kryo): Unit = {
    k.forSubclass[HTTPSession](new HTTPSessionSerializer)
    k.forSubclass[ResourceBundle](new ResourceBundleSerializer)
  }
}

class HTTPSessionSerializer extends KSerializer[HTTPSession] {
  override def read(kryo: Kryo, input: Input, t: Class[HTTPSession]): HTTPSession = {
    val id = kryo.readObject(input, classOf[String])
    SessionMaster.getHttpSession(id).openOrThrowException(s"Unable to find underlying HTTPSession with ID $id")
  }

  override def write(kryo: Kryo, output: Output, session: HTTPSession): Unit = {
    kryo.writeObject(output, session.sessionId)
  }
}

class ResourceBundleSerializer extends KSerializer[ResourceBundle] {
  override def read(kryo: Kryo, input: Input, t: Class[ResourceBundle]): ResourceBundle = {
    val name = kryo.readObject(input, classOf[String])
    val locale = kryo.readObject(input, classOf[Locale])
    ResourceBundle.getBundle(name, locale)
  }

  override def write(kryo: Kryo, output: Output, bundle: ResourceBundle): Unit = {
    kryo.writeObject(output, bundle.getBaseBundleName)
    kryo.writeObject(output, bundle.getLocale)
  }
}