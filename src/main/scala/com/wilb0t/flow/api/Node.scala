package com.wilb0t.flow.api

import com.weiglewilczek.slf4s.Logging

abstract class ExitPort {
  def description: String
  override def toString: String = {
    this.getClass().getSimpleName()+":"+description
  }
}

trait FlowContext {
  def name: String
  override def toString: String = "FlowContext:"+name
}

trait Action {
  def execute(context: FlowContext): ExitPort
}

class FlowNode(
  val name: String, 
  val action: Action, 
  val exitPorts: Map[ExitPort, String]) extends Logging {

  def execute(context: FlowContext): (ExitPort, Option[String]) = {
    logger.info("Executing node "+name+" with context "+context.name)
    val exitPort = action.execute(context)
    val nextNode = exitPorts(exitPort)
    (exitPort, Some(nextNode))
  }

  override def toString: String = {
    name
  }
}

class EndFlowNode(
  name: String, 
  action: Action) extends FlowNode(name, action, Map.empty) {

  override def execute(context: FlowContext): (ExitPort, Option[String]) = {
    logger.info("Executing node "+name+" with context "+context.name)
    val exitPort = action.execute(context)
    (exitPort, None)
  }
}

class Flow(name: String, val nodes: List[FlowNode]) extends Logging {

  val nodeMap: Map[String, FlowNode] = 
    nodes.foldLeft(Map[String, FlowNode]())( (m, n) => m + (n.name -> n))

  def execute: List[(FlowNode, Map[FlowContext, ExitPort])] = {
    def execNode(
        node: Option[FlowNode], 
        path: List[(FlowNode, Map[FlowContext, ExitPort])]
      )
      : List[(FlowNode, Map[FlowContext, ExitPort])] = {

      node match {
        case None => 
          logger.info("Flow finished")
          path.reverse
        case Some(n) =>
          val context = new FlowContext { val name = "Main" }
          val (exitPort, nextNodeName) = n.execute(context)
          logger.info("Got exit port: "+exitPort)
          logger.info("Next node: "+nextNodeName)
          val nextNode = 
            nextNodeName match {
              case Some(name) => nodeMap.get(name)
              case _ => None
            }
          execNode(nextNode, (n, Map(context -> exitPort)) :: path)
      }

    }
    
    logger.info("Starting flow: "+name)
    execNode(nodes.headOption, Nil)
  }

}

