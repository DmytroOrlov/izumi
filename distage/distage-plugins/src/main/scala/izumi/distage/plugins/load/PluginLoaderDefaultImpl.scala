package izumi.distage.plugins.load

import izumi.distage.plugins.{PluginBase, PluginDef}
import izumi.functional.Value
import io.github.classgraph.ClassGraph

import scala.collection.JavaConverters._
import scala.reflect.runtime.universe

class PluginLoaderDefaultImpl(pluginConfig: PluginLoader.PluginConfig) extends PluginLoader {
  def load(): Seq[PluginBase] = {
    val base = classOf[PluginBase]
    val defClass = classOf[PluginDef]
    // Add package with PluginDef & PluginBase so that classgraph will resolve them
    val config = pluginConfig.copy(packagesEnabled = base.getPackage.getName +: defClass.getPackage.getName +: pluginConfig.packagesEnabled)

    val enabledPackages: Seq[String] = config.packagesEnabled.filterNot(config.packagesDisabled.contains)
    val disabledPackages: Seq[String] = config.packagesDisabled

    PluginLoaderDefaultImpl.load[PluginBase](base, Seq(defClass.getCanonicalName), enabledPackages, disabledPackages, config.debug)
//    val scanResult = Value(new ClassGraph())
//      .map(_.whitelistPackages(enabledPackages: _*))
//      .map(_.whitelistClasses(base.getCanonicalName, defClass.getCanonicalName))
//      .map(_.blacklistPackages(disabledPackages: _*))
//      .map(_.enableMethodInfo())
//      .map(if (pluginConfig.debug) _.verbose() else identity)
//      .map(_.scan())
//      .get
//
//    try {
//      val implementors = scanResult.getClassesImplementing(base.getCanonicalName)
//      implementors
//        .asScala
//        .filterNot(_.isAbstract)
//        .flatMap {
//          classInfo =>
//            val constructors = classInfo.getConstructorInfo.asScala
//
//            val clz = classInfo.loadClass()
//            val runtimeMirror = universe.runtimeMirror(clz.getClassLoader)
//            val symbol = runtimeMirror.classSymbol(clz)
//            if (symbol.isModuleClass) {
//              Seq(runtimeMirror.reflectModule(symbol.thisPrefix.termSymbol.asModule).instance.asInstanceOf[PluginBase])
//            } else {
//              val clz = classInfo.loadClass()
//              clz.getDeclaredConstructors.find(_.getParameterCount == 0).map(_.newInstance().asInstanceOf[PluginBase]).toSeq
//            }
//
//
//        }
//        .toSeq // 2.13 compat
//    } finally {
//      scanResult.close()
//    }
  }
}

object PluginLoaderDefaultImpl {
  def load[T](base: Class[_], whitelist: Seq[String], enabledPackages: Seq[String], disabledPackages: Seq[String], debug: Boolean): Seq[T] = {
    val scanResult = Value(new ClassGraph())
      .map(_.whitelistPackages(enabledPackages: _*))
      .map(_.whitelistClasses(whitelist :+ base.getCanonicalName: _*))
      .map(_.blacklistPackages(disabledPackages: _*))
      .map(_.enableMethodInfo())
      .map(if (debug) _.verbose() else identity)
      .map(_.scan())
      .get

    try {
      val implementors = scanResult.getClassesImplementing(base.getCanonicalName)
      implementors
        .asScala
        .filterNot(_.isAbstract)
        .flatMap {
          classInfo =>
            val clz = classInfo.loadClass()
            val runtimeMirror = universe.runtimeMirror(clz.getClassLoader)
            val symbol = runtimeMirror.classSymbol(clz)

            if (symbol.isModuleClass) {
              Seq(runtimeMirror.reflectModule(symbol.thisPrefix.termSymbol.asModule).instance.asInstanceOf[T])
            } else {
              clz.getDeclaredConstructors.find(_.getParameterCount == 0).map(_.newInstance().asInstanceOf[T]).toSeq
            }
        }
        .toSeq // 2.13 compat
    } finally {
      scanResult.close()
    }
  }
}
