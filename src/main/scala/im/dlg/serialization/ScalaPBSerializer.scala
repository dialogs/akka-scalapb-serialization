package im.dlg.serialization

import akka.serialization._
import com.google.common.reflect.{ClassPath, TypeToken}
import com.google.protobuf.{ByteString, GeneratedMessage ⇒ GGeneratedMessage}
import com.trueaccord.scalapb.GeneratedMessage
import org.slf4j.Logger

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

  def apply(clazz: Class[_], log: Option[Logger] = None): Int =
    get(clazz) match {
      case Some(sernum) ⇒ sernum
      case None ⇒
        val sernum = clazz.getDeclaredFields
          .find(_.getName startsWith sernumPrefix)
          .map { field ⇒
            val sernumStr = field.getName.drop(sernumPrefix.length)

            Try(sernumStr.toInt).getOrElse(throw new IllegalArgumentException(
              s"The field '${field.getName}' of class ${clazz.getName} has not contains valid (integer) suffix '^$sernumPrefix\\d+$$'"
            ))
          }
          .getOrElse {
            val code = clazz.getName.hashCode
            log
              .map(l ⇒ l.warn(_: String))
              .getOrElse(println(_: String))(
                s"Class ${clazz.getName} has no field which matches '^$sernumPrefix\\d+$$'. Trying with hashCode $code"
              )
            code
          }

        register(sernum, clazz)

        sernum
    }

  def register(id: Int, clazz: Class[_]): Unit = {
    get(id) match {
      case None ⇒
        get(clazz) match {
          case Some(regId) ⇒ throw new IllegalArgumentException(s"There is already a mapping for class: $clazz, id: $regId")
          case None ⇒
            val companion = Class.forName(clazz.getName + '$')
            if (!companion.getDeclaredFields.exists(_.getName == "MODULE$"))
              throw new IllegalArgumentException(s"Class ${clazz.getName} has no companion")
            map.put(id, companion)
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
    val id = ScalaPBSerializer(o.getClass)
    o match {
      case m: GeneratedMessage  ⇒ SerializedMessage(id, ByteString.copyFrom(m.toByteArray))
      case m: GGeneratedMessage ⇒ SerializedMessage(id, ByteString.copyFrom(m.toByteArray))
      case _                    ⇒ throw new IllegalArgumentException(s"Can't serialize non-scalapb message [${o}]")
    }
  }

  def toBinary(o: AnyRef): Array[Byte] = toMessage(o).toByteArray

  def registerAllGeneratedMessages(packageName: String, classloader: ClassLoader, log: Logger): Unit = {
    log.info("Start register the generated messages")

    val it = ClassPath.from(classloader).getAllClasses.iterator()
    val res = Iterator
      .continually(if (it.hasNext) (true, it.next()) else (false, null))
      .takeWhile(_._1)
      .map(_._2)
      .toList

    val registered = res.count { ci ⇒
      if (ci.getName startsWith packageName)
        try {
          val clazz = ci.load
          if (TypeToken.of(clazz).getTypes.toArray.toList.map(_.toString)
            .contains("com.trueaccord.scalapb.GeneratedMessage")) {
            log.info(s"Register class ${clazz.getName} with sernum: ${apply(clazz)}")
            true
          } else false
        } catch {
          case e: Throwable ⇒
            log.warn(s"Registration of class ${ci.getName} failed with ${e.getMessage}.")
            throw e
        }
      else false
    }

    log.info(s"Finish loading generated messages. Total registered: $registered.")
  }
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
