package io.github.mkotsur.aws.handler

trait CanUnwrap[F[_], A] {
  def unwrapAsync(wrapped: F[A], cb: Either[Throwable, A] => Unit): Unit
}

object CanUnwrap {

//  def apply[F: CanUnwrap[F, *], ]: CanDecode[A] =
//    implicitly[CanDecode[A]]
//
  def instance[F[_], A](func: (F[A], Either[Throwable, A] => Unit) => Unit) =
    new CanUnwrap[F, A] {
      def unwrapAsync(wrapped: F[A], cb: Either[Throwable, A] => Unit): Unit = func(wrapped, cb)
    }

}
