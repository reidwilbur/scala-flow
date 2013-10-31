package com.wilb0t.flow.api

/** The result of executing an [[com.wilb0t.flow.api.Action]]
  */
abstract class ExitPort {
  def description: String
  override def toString: String = {
    this.getClass().getSimpleName()+":"+description
  }
}

/** [[com.wilb0t.flow.api.ParSubFlowNode]]s have an implicit sync point at the end of the subflow and 
  * always return this exit port.
  * 
  * This is because executions in different contexts can exit with different [[com.wilb0t.flow.api.ExitPort]]s
  */
case class ParSubFlowExit() extends ExitPort {
  val description = "Parallel subflow exit"
}

