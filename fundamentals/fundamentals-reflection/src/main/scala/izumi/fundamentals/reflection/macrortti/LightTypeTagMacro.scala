package izumi.fundamentals.reflection.macrortti

import boopickle.{PickleImpl, Pickler}
import izumi.fundamentals.platform.console.TrivialLogger
import izumi.fundamentals.reflection.TrivialMacroLogger
import izumi.fundamentals.reflection.macrortti.LightTypeTag.ParsedLightTypeTag.SubtypeDBs

import scala.reflect.macros.blackbox

final class LightTypeTagMacro(override val c: blackbox.Context) extends LightTypeTagMacro0[blackbox.Context](c)

class LightTypeTagMacro0[C <: blackbox.Context](val c: C) {

  import c.universe._

  final val lightTypeTag: Tree = q"${symbolOf[LightTypeTag.type].asClass.module}"

  private val logger: TrivialLogger = TrivialMacroLogger.make[this.type](c, LightTypeTag.loggerId)

  def cacheEnabled: Boolean = c.settings.contains("ltt-cache")

  protected val impl = new LightTypeTagImpl[c.universe.type](c.universe, withCache = cacheEnabled, logger)

  @inline def makeWeakHKTag[ArgStruct: c.WeakTypeTag]: c.Expr[LTag.WeakHK[ArgStruct]] = {
    makeHKTagRaw[ArgStruct](weakTypeOf[ArgStruct])
  }

  @inline def makeWeakTag[T: c.WeakTypeTag]: c.Expr[LTag.Weak[T]] = {
    val res = makeParsedLightTypeTagImpl(weakTypeOf[T])
    c.Expr[LTag.Weak[T]](q"new ${weakTypeOf[LTag.Weak[T]]}($res)")
  }

  @inline def makeParsedLightTypeTag[T: c.WeakTypeTag]: c.Expr[LightTypeTag] = {
    makeParsedLightTypeTagImpl(weakTypeOf[T])
  }

  def makeHKTagRaw[ArgStruct](argStruct: Type): c.Expr[LTag.WeakHK[ArgStruct]] = {
    def badShapeError(t: TypeApi) = {
      c.abort(c.enclosingPosition, s"Expected type shape RefinedType `{ type Arg[A] = X[A] }` for summoning `LightTagK[X]`, but got $t (raw: ${showRaw(t)} ${t.getClass})")
    }

    argStruct match {
      case r: RefinedTypeApi =>
        r.decl(TypeName("Arg")) match {
          case sym: TypeSymbolApi =>
            val res = makeParsedLightTypeTagImpl(sym.info.typeConstructor)
            // FIXME: `appliedType` doesn't work here for some reason; have to write down the entire name
            c.Expr[LTag.WeakHK[ArgStruct]](q"new _root_.izumi.fundamentals.reflection.macrortti.LTag.WeakHK[$argStruct]($res)")
          case _ => badShapeError(r)
        }
      case other => badShapeError(other)
    }
  }

  def makeParsedLightTypeTagImpl(tpe: Type): c.Expr[LightTypeTag] = {
    val res = impl.makeFullTagImpl(tpe)

    @inline def serialize[A: Pickler](a: A): String = {
      val bytes = PickleImpl(a).toByteBuffer.array()
      new String(bytes, 0, bytes.length, "ISO-8859-1")
    }

    val strRef = serialize(res.ref)(LightTypeTag.lttRefSerializer)
    val strDBs = serialize(SubtypeDBs(res.basesdb, res.idb))(LightTypeTag.subtypeDBsSerializer)

    c.Expr[LightTypeTag](q"$lightTypeTag.parse($strRef : _root_.java.lang.String, $strDBs : _root_.java.lang.String)")
  }
}
