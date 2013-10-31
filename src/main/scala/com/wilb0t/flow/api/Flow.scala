package com.wilb0t.flow.api

/** FlowContext is passed to each Action via a
  * [[com.wilb0t.flow.api.FlowRunner]]
  *
  * FlowContext can be used by an Action to conditionally
  * determine how to act, or acquire data, or generally 
  * identify this execution in a multi threaded run.
  *
  * When [[com.wilb0t.flow.api.FlowRunner]]s are initialized,
  * they are passed all contexts they will run a Flow with
  * including the contexts for subflows.
  */
case class FlowContext(val name: String) {
  override def toString: String = "FlowContext:"+name
}

/** Flow represents the top level collection of [[com.wilb0t.flow.api.Node]]s
  * that can be executed by a [[com.wilb0t.flow.api.FlowRunner]]
  */
case class Flow(val name: String, val nodes: List[Node]) {

  val nodeMap: Map[String, Node] = 
    nodes.foldLeft(Map[String, Node]())( (m, n) => m + (n.name -> n))
}

/** A FlowRunner executes a given [[com.wilb0t.flow.api.Flow]] with 
  * the given contexts.
  *
  * The FlowRunner implementation is responsible for executing the Node
  * actions with the correct FlowContext based on Node type.
  */
trait FlowRunner {
  /** Executes the [[com.wilb0t.flow.api.Flow]] with the 
    * [[com.wilb0t.flow.api.FlowContext]]s.
    *
    * Returns a list of [[com.wilb0t.flow.api.NodeResult]]s for each node and context
    * execution.
    */
  def execute(flow: Flow, context: FlowContext, subContexts: List[FlowContext]): List[NodeResult]
}

/** Loads [[com.wilb0t.flow.api.Flow]]s from Strings or Files
  *
  * Load methods return the succesfully loaded Flow or a String
  * containing an error message describing the load failure.
  */
trait FlowLoader {
  def load(flowString: String): Either[Flow, String]
  def load(flowFile: java.io.File): Either[Flow, String]
}
