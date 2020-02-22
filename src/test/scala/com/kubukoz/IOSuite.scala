package com.kubukoz

import cats.effect._
import munit.FunSuite
import scala.concurrent.Future
import scala.concurrent.duration._
import cats.implicits._
import scala.concurrent.ExecutionContext

trait IOSuite extends FunSuite {
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  val timeout = 5.seconds

  private def modifyIO[A]: IO[A] => IO[A] =
    _.timeoutTo(
      timeout,
      IO(println("test timing out")) *> IO.raiseError(new Throwable("Test timed out after " + timeout))
    ).flatMap {
      case _: IO[_] => IO.raiseError(new Throwable("Execution of IO resulted in more IO. Did you forget to flatMap?"))
      case other    => IO.pure(other)
    }

  override def munitTestValue(testValue: => Any): Future[Any] =
    testValue match {
      case io: IO[_] =>
        super.munitTestValue(modifyIO(io).unsafeToFuture())

      case result => super.munitTestValue(result)
    }
}
