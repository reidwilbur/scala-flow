package com.wilb0t.flow.api

/** Nodes represent the individual steps in a [[com.wilb0t.flow.api.Flow]]
  */
abstract class Node {
  def name: String
  override def toString: String = this.getClass().getSimpleName()+":"+name
}

/** ActionNodes represent a simple executable step of a [[com.wilb0t.flow.api.Flow]]
  *
  * The action is executed to get an [[com.wilb0t.flow.api.ExitPort]]
  *
  * nextNode is used to get the String name of the next Node to execute
  * based on an ExitPort (presumably resulting from executing action).
  */
case class ActionNode(
  val name: String, 
  val action: Action, 
  val nextNode: NextNode) extends Node {
}

/** EndNodes are similar to [[com.wilb0t.flow.api.ActionNode]]s except
  * they have no next Node to execute.  Execution of a Flow directly 
  * containing and EndNode will end after execution of the EndNode
  * action regardless of that action's resulting ExitPort
  */
case class EndNode(
  val name: String, 
  val action: Action) extends Node {
}

/** SubFlowNodes indicate a list of [[com.wilbot.flow.api.Nodes]]s to
  * execute in the same [[com.wilb0t.flow.api.FlowContext]] as the containing
  * [[com.wilb0t.flow.api.Flow]].
  */
case class SubFlowNode(
  val name: String, 
  val nodes: List[Node],
  val nextNode: NextNode) extends Node {

  val nodeMap: Map[String, Node] = 
    nodes.foldLeft(Map[String, Node]())( (m, n) => m + (n.name -> n))
}

/** ParSubFlowNodes indicate a list of [[com.wilb0t.flow.api.Node]]s that
  * should be executed in all sub [[com.wilb0t.flow.api.FlowContext]]s
  * supplied to the [[com.wilb0t.flow.api.FlowRunner]]
  *
  * Based on the FlowRunner implementation, node executions in each 
  * FlowContext may be parallel or serial.
  */
case class ParSubFlowNode(
  val name: String, 
  val nodes: List[Node],
  val nextNode: String) extends Node {

  val nodeMap: Map[String, Node] = 
    nodes.foldLeft(Map[String, Node]())( (m, n) => m + (n.name -> n))

}

/** NodeResult contains the information about the execution of a Node
  *
  * [[com.wilb0t.flow.api.FlowRunner]]s return lists of these as a result
  * of [[com.wilb0t.flow.api.Flow]] execution.
  */
case class NodeResult(val node: Node, val context: FlowContext, val exitPort: ExitPort)

