package net.liftmodules.cluster.kryo

import com.twitter.chill._
import net.liftmodules.cluster.SessionMaster
import net.liftweb.http.provider.HTTPSession

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

