package io.github.mkotsur.aws.handler

import io.github.mkotsur.aws.handler.LambdaFailureException.message

object LambdaFailureException {
  private lazy val message =
    "This exception indicates that the execution " +
      "of your lambda returned a 'failed' effect, " +
      "which may or my not have been what you want."

  def apply(cause: Throwable): LambdaFailureException =
    new LambdaFailureException(cause)
}

class LambdaFailureException(cause: Throwable)
    extends RuntimeException(message, cause) {}
