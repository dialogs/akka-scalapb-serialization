package im.dlg.serialization

import akka.serialization._
import com.google.protobuf.{ByteString, GeneratedMessage ⇒ GGeneratedMessage}
import com.trueaccord.scalapb.GeneratedMessage

import scala.collection.concurrent.TrieMap
import scala.util.{Failure, Success, Try}

object ScalaPBSerializer {
  private val ARRAY_OF_BYTE_ARRAY = Array[Class[_]](classOf[Array[Byte]])

  // FIXME: dynamically increase capacity
  private val map = TrieMap.empty[Int, Class[_]]
  private val reverseMap = TrieMap.empty[Class[_], Int]
  private val sernumPrefix = "sernum"

  def clean(): Unit = {
    map.clear()
    reverseMap.clear()
  }

  def apply(clazz: Class[_]): Int = reverseMap.getOrElseUpdate(clazz, {
    val fieldName = clazz.getDeclaredFields
      .find(_.getName startsWith sernumPrefix)
      .getOrElse(throw new IllegalArgumentException(s"Class ${clazz.getName} has no any field which matches '^$sernumPrefix\\d+$$'"))
      .getName

    val sernumStr = fieldName.drop(sernumPrefix.length)

    val sernum = Try(sernumStr.toInt).getOrElse(throw new IllegalArgumentException(
      s"The field '$fieldName' of class ${clazz.getName} has not contains valid (integer) suffix '^$sernumPrefix\\d+$$'"
    ))

    map.update(sernum, clazz)

    sernum
  })

  def register(id: Int, clazz: Class[_]): Unit = {
    get(id) match {
      case None ⇒
        get(clazz) match {
          case Some(regId) ⇒ throw new IllegalArgumentException(s"There is already a mapping for class: $clazz, id: $regId")
          case None ⇒
            map.put(id, Class.forName(clazz.getName + '$'))
            reverseMap.put(clazz, id)
        }
      case Some(registered) ⇒
        if (!get(clazz).contains(id))
          throw new IllegalArgumentException(s"There is already a mapping with id $id: ${map.get(id).orNull}")
    }
  }

  def register(items: (Int, Class[_])*): Unit =
    items foreach { case (id, clazz) ⇒ register(id, clazz) }

  def get(id: Int): Option[Class[_]] = map.get(id)

  def get(clazz: Class[_]) = reverseMap.get(clazz)

  def getOrElseUpdate(clazz: Class[_], id: ⇒ Int) = reverseMap.getOrElseUpdate(clazz, id)

  def fromMessage(message: SerializedMessage): AnyRef = {
    ScalaPBSerializer.get(message.id) match {
      case Some(clazz) ⇒
        val field = clazz.getField("MODULE$").get(null)

        clazz
          .getDeclaredMethod("validate", ARRAY_OF_BYTE_ARRAY: _*)
          .invoke(field, message.bytes.toByteArray) match {
            case Success(msg) ⇒ msg.asInstanceOf[GeneratedMessage]
            case Failure(e)   ⇒ throw e
          }
      case None ⇒ throw new IllegalArgumentException(s"Can't find mapping for id: ${message.id}")
    }
  }

  def fromBinary(bytes: Array[Byte]): AnyRef = fromMessage(SerializedMessage.parseFrom(bytes))

  def toMessage(o: AnyRef): SerializedMessage = {
    val id = ScalaPBSerializer.getOrElseUpdate(o.getClass, apply(o.getClass))
    o match {
      case m: GeneratedMessage  ⇒ SerializedMessage(id, ByteString.copyFrom(m.toByteArray))
      case m: GGeneratedMessage ⇒ SerializedMessage(id, ByteString.copyFrom(m.toByteArray))
      case _                    ⇒ throw new IllegalArgumentException(s"Can't serialize non-scalapb message [${o}]")
    }
  }

  def toBinary(o: AnyRef): Array[Byte] = toMessage(o).toByteArray
}

class ScalaPBSerializerObsolete extends Serializer {
  override def identifier: Int = 3456
  override def includeManifest: Boolean = false
  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = ScalaPBSerializer.fromBinary(bytes)
  override def toBinary(o: AnyRef): Array[Byte] = ScalaPBSerializer.toBinary(o)
}

class ScalaPBSerializer extends SerializerWithStringManifest {
  override def identifier: Int = 3457

  override def manifest(o: AnyRef): String = ScalaPBSerializer(o.getClass).toString

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    ScalaPBSerializer.fromMessage(SerializedMessage(manifest.toInt, ByteString.copyFrom(bytes)))

  override def toBinary(o: AnyRef): Array[Byte] =
    o match {
      case g: GeneratedMessage ⇒ g.toByteArray
      case _                   ⇒ throw new IllegalArgumentException("Only GeneratedMessage is supported")
    }
}
