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

case class FlowContext(val name: String) {
  override def toString: String = "FlowContext:"+name
}

case class NodeResult(val node: Node, val context: FlowContext, val exitPort: ExitPort)

trait Action {
  def execute(context: FlowContext): ExitPort
}

abstract class Node {
  def name: String
  override def toString: String = this.getClass().getSimpleName()+":"+name
}

case class ActionNode(
  val name: String, 
  val action: Action, 
  val exitPorts: Map[ExitPort, String]) extends Node {
}

case class EndNode(
  val name: String, 
  val action: Action) extends Node {
}

case class SubFlowNode(
  val name: String, 
  val nodes: List[Node],
  val exitPorts: Map[ExitPort, String]) extends Node {

  val nodeMap: Map[String, Node] = 
    nodes.foldLeft(Map[String, Node]())( (m, n) => m + (n.name -> n))
}

case class ParSubFlowNode(
  val name: String, 
  val nodes: List[Node],
  val nextNode: String) extends Node {

  val nodeMap: Map[String, Node] = 
    nodes.foldLeft(Map[String, Node]())( (m, n) => m + (n.name -> n))

}

