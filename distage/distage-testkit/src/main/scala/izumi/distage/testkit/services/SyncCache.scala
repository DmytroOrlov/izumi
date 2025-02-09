package izumi.distage.testkit.services

import izumi.distage.model.definition.Axis.AxisValue
import izumi.distage.model.definition.{AxisBase, ModuleBase}
import izumi.distage.roles.BootstrapConfig
import izumi.distage.roles.services.PluginSource
import izumi.fundamentals.platform.language.Quirks._

import scala.collection.mutable

class SyncCache[K, V] {

  def enumerate(): Seq[(K, V)] = sync {
    cache.toSeq
  }

  private val cache = new mutable.HashMap[K, V]()

  def getOrCompute(k: K, default: => V): V = sync {
    cache.getOrElseUpdate(k, default)
  }

  def put(k: K, v: V): Unit = sync {
    cache.put(k, v).discard()
  }

  def get(k: K): Option[V] = sync {
    cache.get(k)
  }

  def clear(): Unit = sync {
    cache.clear()
  }

  def hasKey(k: K): Boolean = sync {
    cache.contains(k)
  }

  def putIfNotExist(k: K, v: => V): Unit = sync {
    if (!cache.contains(k)) {
      cache.put(k, v).discard()
    }
  }

  def size: Int = sync {
    cache.size
  }

  private def sync[T](f: => T): T = {
    cache.synchronized {
      f
    }
  }
}

object PluginsCache {
  case class CacheKey(config: BootstrapConfig)
  case class CacheValue(
                         plugins: PluginSource.AllLoadedPlugins,
                         bsModule: ModuleBase,
                         appModule: ModuleBase,
                         availableActivations: Map[AxisBase, Set[AxisValue]],
                       )

  object Instance extends SyncCache[CacheKey, CacheValue] {
    // sbt in nofork mode runs each module in it's own classloader thus we have separate cache per module per run
  }
}
