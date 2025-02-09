package izumi.fundamentals.reflection.macrortti

import izumi.functional.{Renderable, WithRenderableSyntax}
import izumi.fundamentals.reflection.macrortti.LightTypeTagRef._

trait LTTRenderables extends WithRenderableSyntax {

  implicit def r_LightTypeTag: Renderable[LightTypeTagRef] = {
    case a: AbstractReference =>
      a.render()
  }

  implicit def r_AbstractReference: Renderable[AbstractReference] = {
    case a: AppliedReference =>
      a.render()
    case l: Lambda =>
      l.render()
  }

  implicit def r_AppliedReference: Renderable[AppliedReference] = {
    case a: AppliedNamedReference =>
      a.render()
    case i: IntersectionReference =>
      i.render()
    case r: Refinement =>
      r.render()
  }

  implicit def r_Refinement: Renderable[Refinement] = (value: Refinement) => {
    s"(${value.reference.render()} & ${value.decls.map(_.render()).toSeq.sorted.mkString("{", ", ", "}")})"
  }


  implicit def r_RefinementDecl: Renderable[RefinementDecl] = {
    case RefinementDecl.Signature(name, input, output) =>
      s"def $name${input.map(_.render()).mkString("(", ", ", ")")}: ${output.render()}"
    case RefinementDecl.TypeMember(name, tpe) =>
      s"type $name = $tpe"
  }

  implicit def r_AppliedNamedReference: Renderable[AppliedNamedReference] = {
    case n: NameReference =>
      n.render()
    case f: FullReference =>
      f.render()
  }

  implicit def r_Lambda: Renderable[Lambda] = (value: Lambda) => {
    s"λ ${value.input.map(_.render()).mkString(",")} → ${value.output.render()}"
  }

  implicit def r_LambdaParameter: Renderable[LambdaParameter] = (value: LambdaParameter) => {
    s"%${value.name}"
  }

  implicit def r_NameRefRenderer: Renderable[NameReference] = (value: NameReference) => {
    val r = nameToString(value)

    val rr = value.boundaries match {
      case _: Boundaries.Defined =>
        s"$r|${value.boundaries.render()}"
      case Boundaries.Empty =>
        r
    }

    value.prefix match {
      case Some(p) =>
        s"${(p: LightTypeTagRef).render()}::$rr"
      case None =>
        rr
    }
  }

  protected def nameToString(value: NameReference): String

  implicit def r_FullReference: Renderable[FullReference] = (value: FullReference) => {
    s"${value.asName.render()}${value.parameters.map(_.render()).mkString("[", ",", "]")}"
  }

  implicit def r_IntersectionReference: Renderable[IntersectionReference] = (value: IntersectionReference) => {
    value.refs.map(_.render()).mkString("{", " & ", "}")
  }

  implicit def r_TypeParam: Renderable[TypeParam] = (value: TypeParam) => {
    s"${value.variance.render()}${value.ref}"
  }

  implicit def r_Variance: Renderable[Variance] = {
    case Variance.Invariant => "="
    case Variance.Contravariant => "-"
    case Variance.Covariant => "+"
  }

  implicit def r_Boundaries: Renderable[Boundaries] = {
    case Boundaries.Defined(bottom, top) =>
      s"<${bottom.render()}..${top.render()}>"

    case Boundaries.Empty =>
      ""
  }

}

object LTTRenderables {

  object Short extends LTTRenderables {
    protected def nameToString(value: NameReference): String = {
      value.ref.split('.').last
    }
  }

  object Long extends LTTRenderables {
    protected def nameToString(value: NameReference): String = {
      value.ref
    }
  }
}
