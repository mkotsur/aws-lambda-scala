package io.github.mkotsur.aws.handler

trait CanUnwrap[F[_], A] {
  def unwrapAsync(wrapped: F[A], cb: Either[Throwable, A] => Unit): Unit
}

object CanUnwrap {

//  def apply[F: CanUnwrap[F, *], ]: CanDecode[A] =
//    implicitly[CanDecode[A]]
//
//  def instance[A](func: ReadStream[A]): CanDecode[A] = new CanDecode[A] {
//    override def readStream: ReadStream[A] = func
//  }

}
