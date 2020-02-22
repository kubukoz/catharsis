package com.kubukoz

import cats.effect.Concurrent
import cats.effect.implicits._

object catharsis {

  /**
    * Run `fa` and `hanger` concurrently, but:
    *
    * - if `fa` completes first, `hanger` is canceled
    * - if `hanger` completes first, `fa` keeps running
    *
    * If the whole thing is canceled, both actions are canceled.
    *
    * Most likely use-case: Log a message if `fa` takes too long to run (but don't interrupt it):
    *
    * @example{{{
    *
    * onHang(
    *   action = IO.sleep(10.seconds) *> IO(println("finished"))
    * )(
    *   hanger = IO.sleep(5.seconds) *> IO(println("program takes too much time"))
    * )
    * }}}
    */
  def onHang[F[_]: Concurrent, A](action: F[A])(hanger: F[Unit]): F[A] = hanger.background.use(_ => action)
}
