package net.liftmodules.cluster.kryo

import com.twitter.chill._
import net.liftmodules.cluster.SessionMaster
import net.liftweb.http.provider.servlet.HTTPServletSession

class LiftInstantiator extends ScalaKryoInstantiator {
  override def newKryo(): KryoBase = {
    val k = super.newKryo()
    val registrar = new LiftRegistrar
    registrar(k)
    k
  }
}

class LiftRegistrar extends IKryoRegistrar {
  override def apply(k: Kryo): Unit = {
    k.forClass[HTTPServletSession](new HTTPSessionSerializer)
  }
}

class HTTPSessionSerializer extends KSerializer[HTTPServletSession] {
  override def read(kryo: Kryo, input: Input, t: Class[HTTPServletSession]): HTTPServletSession = {
    val id = kryo.readObject(input, classOf[String])
    SessionMaster.getHttpSession(id).asA[HTTPServletSession].openOrThrowException("Well damn...")
  }

  override def write(kryo: Kryo, output: Output, session: HTTPServletSession): Unit = {
    kryo.writeObject(output, session.sessionId)
  }
}
