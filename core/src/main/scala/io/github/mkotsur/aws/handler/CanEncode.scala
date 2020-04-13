package io.github.mkotsur.aws.handler

import java.io.OutputStream

import com.amazonaws.services.lambda.runtime.Context
import io.github.mkotsur.aws.handler.CanEncode.WriteStream

trait CanEncode[O] {
  def writeStream: WriteStream[O]
}

object CanEncode {

  type WriteStream[O] = (OutputStream,
                         //TODO: take O instead? or write a note why is it a contextualized value.
                         //the unhappy part of the either doesn't seem to be handled anywhere
                         Either[Throwable, O],
                         Context) => Either[Throwable, Unit]

  def apply[A: CanEncode]: CanEncode[A] =
    implicitly[CanEncode[A]]

  def instance[A](func: WriteStream[A]): CanEncode[A] = new CanEncode[A] {
    override def writeStream: WriteStream[A] = func
  }

}
