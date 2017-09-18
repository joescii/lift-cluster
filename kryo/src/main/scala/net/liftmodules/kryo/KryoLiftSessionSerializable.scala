package net.liftmodules.kryo

import java.io.ObjectOutputStream

import com.twitter.chill.{KryoInstantiator, KryoPool, ScalaKryoInstantiator}
import net.liftmodules.cluster.SerializableLiftSession
import net.liftweb.http.LiftSession


object KryoSerializableLiftSession {
  val serializer: LiftSession => SerializableLiftSession = new KryoSerializableLiftSession(_)
}

class KryoSerializableLiftSession(@transient val _session: LiftSession) extends SerializableLiftSession {
  private[this] def this() = this(null)

  private[this] val kryo = KryoSerializable(_session)

  override def session: LiftSession = kryo.obj
}

/**
  * Converts any object into a java.io.Serializable object by using twitter chill (and hence kryo) under the hood.
  * @param obj any object you wish to serialize
  * @tparam T the type of object to serialize
  */
@SerialVersionUID(1L)
final class KryoSerializable[T](@transient val obj: T) extends Serializable {
  private[this] def kryo: KryoInstantiator = {
    val instantiator = new ScalaKryoInstantiator
    instantiator.setRegistrationRequired(false)
    instantiator.setReferences(true)
  }

  private[this] def deserialize(): T = {
    KryoPool.withByteArrayOutputStream(1, kryo)
      .fromBytes(bytes)
      .asInstanceOf[T]
  }

  private[this] lazy val bytes: Array[Byte] = {
    val kpool = KryoPool.withByteArrayOutputStream(1, kryo)
    val bytes = kpool.toBytesWithClass(obj)
    bytes
  }

  /**
    * Called during serialization
    * See http://www.oracle.com/technetwork/articles/java/javaserial-1536170.html
    */
  private[this] def writeObject(out: ObjectOutputStream): Unit = {
    // Touch bytes to force the serialization
    bytes

    // We don't need any special handling
    out.defaultWriteObject()
  }

  /**
    * Called AFTER deserialization, providing our own replacement. This is what allows us to provide a non-null
    * value for the constructor parameter obj.
    * @return an object to replace this one after deserialization.
    */
  protected def readResolve(): Any = KryoSerializable(this.deserialize())
}

object KryoSerializable {
  def apply[T](obj: T): KryoSerializable[T] = new KryoSerializable(obj)
}

