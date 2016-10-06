package scalacache.ehcache

import cats.Id
import org.slf4j.LoggerFactory

import scalacache.serialization.{ Codec, InMemoryRepr }
import scalacache.{ Cache, LoggingSupport }
import scala.concurrent.duration.Duration
import net.sf.ehcache.{ Element, Cache => Ehcache }

/**
 * Thin wrapper around Ehcache.
 * Since Ehcache is in-memory and non-blocking,
 * all operations are performed synchronously, i.e. ExecutionContext is not needed.
 */
class EhcacheCache(underlying: Ehcache)
    extends Cache[InMemoryRepr, Id]
    with LoggingSupport {

  override protected final val logger = LoggerFactory.getLogger(getClass.getName)

  /**
   * Get the value corresponding to the given key from the cache
   *
   * @param key cache key
   * @tparam V the type of the corresponding value
   * @return the value, if there is one
   */
  override def get[V](key: String)(implicit codec: Codec[V, InMemoryRepr]) = {
    val result = {
      val elem = underlying.get(key)
      if (elem == null) None
      else Option(elem.getObjectValue.asInstanceOf[V])
    }
    if (logger.isDebugEnabled)
      logCacheHitOrMiss(key, result)
    result
  }

  /**
   * Insert the given key-value pair into the cache, with an optional Time To Live.
   *
   * @param key cache key
   * @param value corresponding value
   * @param ttl Time To Live
   * @tparam V the type of the corresponding value
   */
  override def put[V](key: String, value: V, ttl: Option[Duration])(implicit codec: Codec[V, InMemoryRepr]) = {
    val element = new Element(key, value)
    ttl.foreach(t => element.setTimeToLive(t.toSeconds.toInt))
    underlying.put(element)
    logCachePut(key, ttl)
  }

  /**
   * Remove the given key and its associated value from the cache, if it exists.
   * If the key is not in the cache, do nothing.
   *
   * @param key cache key
   */
  override def remove(key: String) = underlying.remove(key)

  override def removeAll() = underlying.removeAll()

  override def close(): Unit = {
    // Nothing to do
  }

}

object EhcacheCache {

  /**
   * Create a new cache utilizing the given underlying Ehcache cache.
   *
   * @param underlying an Ehcache cache
   */
  def apply(underlying: Ehcache): EhcacheCache = new EhcacheCache(underlying)

}
