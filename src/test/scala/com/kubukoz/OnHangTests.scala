package com.kubukoz

import cats.effect.concurrent.Deferred
import cats.effect.IO

import cats.effect.implicits._
import cats.implicits._

class OnHangTests extends IOSuite {
  import catharsis.onHang

  test("fa completes first") {
    val fa = IO.unit

    Deferred[IO, Unit].flatMap { hangerCanceled =>
      val hanger = IO.never.onCancel(hangerCanceled.complete(()))

      onHang(fa)(hanger) *> hangerCanceled.get
    }
  }

  test("hanger completes first") {

    Deferred[IO, Unit].flatMap { hangerCompleted =>
      val fa = hangerCompleted.get
      val hanger = hangerCompleted.complete(())

      onHang(fa)(hanger)
    }
  }

  test("canceled before hanger completes") {
    (Deferred[IO, Unit], Deferred[IO, Unit], Deferred[IO, Unit], Deferred[IO, Unit]).tupled.flatMap {
      case (faStarted, hangerStarted, faCanceled, hangerCanceled) =>
        val fa = (faStarted.complete(()) *> IO.never).onCancel(faCanceled.complete(()))
        val hanger = (hangerStarted.complete(()) *> IO.never).onCancel(hangerCanceled.complete(()))

        onHang(fa)(hanger).race(faStarted.get *> hangerStarted.get) *>
          faCanceled.get *>
          hangerCanceled.get
    }
  }

  test("canceled after hanger completes") {
    (Deferred[IO, Unit], Deferred[IO, Unit], Deferred[IO, Unit]).tupled.flatMap {
      case (faStarted, faCanceled, hangerCompleted) =>
        val fa = (faStarted.complete(()) *> IO.never).onCancel(faCanceled.complete(()))
        val hanger = faStarted.get *> hangerCompleted.complete(())

        onHang(fa)(hanger).race(hangerCompleted.get) *>
          faCanceled.get
    }
  }
}
