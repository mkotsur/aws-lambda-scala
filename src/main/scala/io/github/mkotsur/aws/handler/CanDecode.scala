package io.github.mkotsur.aws.handler

import io.github.mkotsur.aws.handler.Lambda.ReadStream

trait CanDecode[I] {
  def readStream: ReadStream[I]
}

object CanDecode {

  def apply[A](implicit canDecode: CanDecode[A]): CanDecode[A] =
    canDecode

  def instance[A](func: ReadStream[A]): CanDecode[A] = new CanDecode[A] {
    override def readStream: ReadStream[A] = func
  }

}
