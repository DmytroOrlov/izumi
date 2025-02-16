package izumi.distage.testkit.distagesuite

import zio.{IO => ZIO}
import cats.effect.{IO => CIO}
import izumi.distage.testkit.distagesuite.fixtures.{ApplePaymentProvider, MockCache, MockCachedUserService, MockUserRepository}
import izumi.distage.testkit.services.st.dtest.{DistageAbstractScalatestSpec, TestConfig}
import izumi.distage.testkit.st.specs.{DistageBIOSpecScalatest, DistageSpecScalatest}
import distage._

trait DistageMemoizeExample[F[_]] { this: DistageAbstractScalatestSpec[F] =>
  override protected def config: TestConfig = TestConfig(memoizedKeys = Set(DIKey.get[MockCache[CIO]], DIKey.get[MockCache[ZIO[Throwable, ?]]]))
}

class DistageTestExampleBIO extends DistageBIOSpecScalatest[ZIO] with DistageMemoizeExample[ZIO[Throwable, ?]] {

  "distage test runner" should {
    "support bifunctor" in {
      service: MockUserRepository[ZIO[Throwable, ?]] =>
        for {
          _ <- ZIO(assert(service != null))
        } yield ()
    }
  }

}

class DistageTestExample extends DistageSpecScalatest[CIO] with DistageMemoizeExample[CIO] {
  "distage test runner" should {
    "test 1" in {
      service: MockUserRepository[CIO] =>
        for {
          _ <- CIO.delay(assert(service != null))
          _ <- CIO.delay(println("test2"))
        } yield ()
    }

    "test 2" in {
      service: MockCachedUserService[CIO] =>
        for {
          _ <- CIO.delay(assert(service != null))
          _ <- CIO.delay(println("test1"))
        } yield ()
    }

    "test 3" in {
      service: MockCachedUserService[CIO] =>
        CIO.delay(assert(service != null))
    }

    "test 4 (should be ignored)" in {
      _: ApplePaymentProvider[CIO] =>
//        ???
    }
  }

}

class DistageTestExample1 extends DistageSpecScalatest[CIO] with DistageMemoizeExample[CIO] {

  "distage test custom runner" should {
    "test 1" in {
      service: MockUserRepository[CIO] =>
        for {
          _ <- CIO.delay(assert(service != null))
          _ <- CIO.delay(println("test2"))
        } yield ()
    }

    "test 2" in {
      service: MockCachedUserService[CIO] =>
        for {
          _ <- CIO.delay(assert(service != null))
          _ <- CIO.delay(println("test1"))
        } yield ()
    }

    "test 3" in {
      service: MockCachedUserService[CIO] =>
        CIO.delay(assert(service != null))
    }

    "test 4" in {
      _: ApplePaymentProvider[CIO] =>
        //???
    }
  }


}
