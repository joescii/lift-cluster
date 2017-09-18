package net.liftmodules.kryo

import org.scalatest.{FlatSpec, Matchers}

import java.io._

class KryoTestClass(val data: String) {
  def this() = this("")

  def canEqual(other: Any): Boolean = other.isInstanceOf[KryoTestClass]

  override def equals(other: Any): Boolean = other match {
    case that: KryoTestClass =>
      (that canEqual this) &&
        data == that.data
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(data)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

class KryoSpec extends FlatSpec with Matchers {
  def serialize[T](in: T): Array[Byte] = {
    val bos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(bos)
    oos.writeObject(in)
    oos.flush()
    bos.toByteArray()
  }

  def deserialize[T](in: Array[Byte]): T = {
    val bis = new ByteArrayInputStream(in)
    val ois = new ObjectInputStream(bis)
    ois.readObject.asInstanceOf[T]
  }

  "Two KryoTestClass instances with different data" should "not be equal" in {
    (new KryoTestClass("a") == new KryoTestClass(null)) shouldBe false
  }

  "Two KryoTestClass instances with the same data" should "be equal" in {
    (new KryoTestClass("a") == new KryoTestClass("a")) shouldBe true
  }

  "The KryoTestClass" should "fail java serialization" in {
    intercept[NotSerializableException] {
      serialize(new KryoTestClass("a"))
    }
  }

  "KryoSerializable" should "round trip the KryoTestClass preserving equality" in {
    val obj = new KryoTestClass("round trip")
    val kryo = KryoSerializable(obj)
    val bytes = serialize(kryo)

    bytes.length shouldNot equal(0)

    val rt = deserialize[KryoSerializable[KryoTestClass]](bytes).obj

    rt.data shouldBe "round trip"
    (obj eq rt) shouldBe false
    (obj == rt) shouldBe true
  }
}

