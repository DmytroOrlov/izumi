package izumi.distage.roles.model

import izumi.distage.model.definition.Axis.AxisValue
import izumi.distage.model.definition.AxisBase

final case class AppActivation(choices: Map[AxisBase, Set[AxisValue]], active: Map[AxisBase, AxisValue])

object AppActivation {
  def empty: AppActivation = AppActivation(Map.empty, Map.empty)
}
