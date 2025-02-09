package izumi.distage.roles.services

import izumi.distage.model.Locator
import izumi.distage.model.monadic.{DIEffect, DIEffectRunner}
import izumi.distage.roles.services.RoleAppPlanner.AppStartupPlans
import izumi.fundamentals.platform.functional.Identity
import distage.{Injector, TagK}

class StartupPlanExecutorImpl(
                               injector: Injector,
                               integrationChecker: IntegrationChecker
                             ) extends StartupPlanExecutor {
  final def execute[F[_] : TagK](appPlan: AppStartupPlans, filters: StartupPlanExecutor.Filters[F])(doRun: (Locator, DIEffect[F]) => F[Unit]): Unit = {

    injector.produceFX[Identity](appPlan.runtime, filters.filterId)
      .use {
        runtimeLocator =>
          val runner = runtimeLocator.get[DIEffectRunner[F]]
          implicit val effect: DIEffect[F] = runtimeLocator.get[DIEffect[F]]

          runner.run {
            Injector.inherit(runtimeLocator)
              .produceFX[F](appPlan.integration, filters.filterF)
              .use {
                integrationLocator =>
                  integrationChecker.checkOrFail(appPlan.integrationKeys, integrationLocator)


                  Injector.inherit(integrationLocator)
                    .produceFX[F](appPlan.app, filters.filterF)
                    .use(doRun(_, effect))
              }
          }
      }
  }

}
