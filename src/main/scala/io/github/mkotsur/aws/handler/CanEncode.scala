package io.github.mkotsur.aws.handler

import io.github.mkotsur.aws.handler.Lambda.WriteStream

trait CanEncode[O] {
  def writeStream: WriteStream[O]
}

object CanEncode {

  def apply[A](implicit canEncode: CanEncode[A]): CanEncode[A] =
    canEncode

  def instance[A](func: WriteStream[A]): CanEncode[A] = new CanEncode[A] {
    override def writeStream: WriteStream[A] = func
  }

}
