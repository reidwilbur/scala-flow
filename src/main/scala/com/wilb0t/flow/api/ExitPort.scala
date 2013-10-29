package com.wilb0t.flow.api

/** The result of executing an [[com.wilb0t.api.Action]]
  */
abstract class ExitPort {
  def description: String
  override def toString: String = {
    this.getClass().getSimpleName()+":"+description
  }
}

/** Since different contexts can exit with different [[com.wilb0t.api.ExitPort]]s
  * parallel subflows have an implicit sync point at the end of the flow and 
  * always return this exit port.
  */
case class ParSubFlowExit() extends ExitPort {
  val description = "Parallel subflow exit"
}

