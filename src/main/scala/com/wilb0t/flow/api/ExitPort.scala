package com.wilb0t.flow.api

abstract class ExitPort {
  def description: String
  override def toString: String = {
    this.getClass().getSimpleName()+":"+description
  }
}

case class ParSubFlowExit() extends ExitPort {
  val description = "Parallel subflow exit"
}

