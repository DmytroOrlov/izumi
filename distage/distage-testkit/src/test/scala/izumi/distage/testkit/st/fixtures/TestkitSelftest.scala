package izumi.distage.testkit.st.fixtures

import izumi.distage.testkit.st.adapter.specs.DistagePluginSpec
import distage.TagK

abstract class TestkitSelftest[F[_]: TagK] extends DistagePluginSpec[F]{
  override protected def pluginPackages: Seq[String] = thisPackage
}
