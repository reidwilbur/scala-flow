package com.wilb0t.flow.impl

import com.wilb0t.flow.api._
import com.weiglewilczek.slf4s.Logging

case class PassExit() extends ExitPort {
  val description = "Node Passed"
}

case class FailExit() extends ExitPort {
  val description = "Node Failed"
}

class PassAction extends Action with Logging {
  override def apply(context: FlowContext): ExitPort = {
    logger.info("Emitting PassExit")

    PassExit()
  }
}

class FailAction extends Action with Logging {
  override def apply(context: FlowContext): ExitPort = {
    logger.info("Emitting FailExit")

    FailExit()
  }
}

