package com.wilb0t.flow.api

import com.weiglewilczek.slf4s.Logging

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
  val exitPorts: Map[ExitPort, String]) extends Node with Logging {
}

case class EndNode(
  val name: String, 
  val action: Action) extends Node with Logging {
}

case class SubFlowNode(
  val name: String, 
  val nodes: List[Node],
  val exitPorts: Map[ExitPort, String]) extends Node with Logging {

  val nodeMap: Map[String, Node] = 
    nodes.foldLeft(Map[String, Node]())( (m, n) => m + (n.name -> n))
}

case class ParSubFlowNode(
  val name: String, 
  val nodes: List[Node],
  val nextNode: String) extends Node with Logging {

  val nodeMap: Map[String, Node] = 
    nodes.foldLeft(Map[String, Node]())( (m, n) => m + (n.name -> n))

}

class Flow(val name: String, val nodes: List[Node]) extends Logging {

  val nodeMap: Map[String, Node] = 
    nodes.foldLeft(Map[String, Node]())( (m, n) => m + (n.name -> n))
}

class FlowRunner(val flow: Flow) extends Logging {

  def execute(
      context: FlowContext, 
      subContexts: List[FlowContext]
    )
    : List[(Node, Map[FlowContext, ExitPort])] = {

    def execNode(
        node: Option[Node], 
        path: List[(Node, Map[FlowContext, ExitPort])]
      )
      : List[(Node, Map[FlowContext, ExitPort])] = {

      node match {
        case None => 
          logger.info("Flow finished")
          path.reverse

        case Some(n @ EndNode(name, action)) =>
          logger.info("Executing: "+n)
          val exitPort = action.execute(context)
          logger.info("Got exit port: "+exitPort)
          ((n, Map(context -> exitPort)) :: path).reverse

        case Some(n @ ActionNode(name, action, exitPorts)) =>
          logger.info("Executing: "+n)
          val exitPort = action.execute(context)
          val nextNodeName = exitPorts.get(exitPort)
          logger.info("Got exit port: "+exitPort)
          logger.info("Next node: "+nextNodeName)
          val nextNode = 
            nextNodeName match {
              case Some(name) => flow.nodeMap.get(name)
              case _ => None
            }
          execNode(nextNode, (n, Map(context -> exitPort)) :: path)

        case Some(n @ SubFlowNode(name, nodes, exitPorts)) =>
          logger.info("Executing: "+n)
          val subFlow = new Flow(name, nodes)
          val subFlowRunner = new FlowRunner(subFlow)

          val subFlowPath = subFlowRunner.execute(context, subContexts)

          val exitPort = (path.last._2)(context)
          logger.info("Got exit port: "+exitPort)
          val nextNodeName = exitPorts.get(exitPort)
          logger.info("Next node: "+nextNodeName)
          val nextNode = 
            nextNodeName match {
              case Some(name) => flow.nodeMap.get(name)
              case _ => None
            }
          execNode(nextNode, (n, Map(context -> exitPort)) :: subFlowPath.reverse ::: path)

        case Some(n @ ParSubFlowNode(name, nodes, nextNodeName)) =>
          logger.info("Executing: "+n)
          val subFlow: Flow = new Flow(name, nodes)
          val subFlowRunner = new FlowRunner(subFlow)

          val subFlowPaths = 
            subContexts.flatMap{ subFlowRunner.execute(_, subContexts) }
         
          val executedNodes: Iterable[Node] = subFlowPaths.foldLeft(Map[Node, Boolean]())( (m: Map[Node, Boolean], e: (Node, Map[FlowContext, ExitPort])) => m + (e._1 -> true)).keys

          val coalescedPaths: Iterable[(Node, Map[FlowContext, ExitPort])] = 
            executedNodes.map( 
              n => (n, 
                subFlowPaths
                  .filter( _._1 == n)
                  .foldLeft(Map[FlowContext, ExitPort]())( 
                    (m: Map[FlowContext, ExitPort], e: (Node, Map[FlowContext, ExitPort])) => m ++ e._2 )
                )
            )

          val nextNode = flow.nodeMap.get(nextNodeName)
          execNode(nextNode, (n, Map(context -> ParSubFlowExit())) :: (coalescedPaths ++: path))
      }

    }
    
    logger.info("Starting flow: "+flow.name)
    execNode(flow.nodes.headOption, Nil)
  }

}
