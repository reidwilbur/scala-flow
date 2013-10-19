package com.wilb0t.flow.impl

import com.wilb0t.flow.api._
import com.weiglewilczek.slf4s.Logging
import scala.concurrent._

class ParallelFlowRunner(val flow: Flow) extends FlowRunner with Logging {

  def execute(
      context: FlowContext, 
      subContexts: List[FlowContext]
    )
    : List[NodeResult] = {

    def execNode(
        node: Option[Node], 
        path: List[NodeResult]
      )
      : List[NodeResult] = {

      node match {
        case None => 
          logger.info("Flow finished")
          path.reverse

        case Some(n @ EndNode(name, action)) =>
          logger.info("Executing: "+n)
          val exitPort = action.execute(context)
          logger.info("Got exit port: "+exitPort)
          (NodeResult(n, context, exitPort) :: path).reverse

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
          execNode(nextNode, NodeResult(n, context, exitPort) :: path)

        case Some(n @ SubFlowNode(name, nodes, exitPorts)) =>
          logger.info("Executing: "+n)
          val subFlow = new Flow(name, nodes)
          val subFlowRunner = new ParallelFlowRunner(subFlow)

          val subFlowResults = subFlowRunner.execute(context, subContexts)

          val exitPort = path.last.exitPort
          logger.info("Got exit port: "+exitPort)
          val nextNodeName = exitPorts.get(exitPort)
          logger.info("Next node: "+nextNodeName)
          val nextNode = 
            nextNodeName match {
              case Some(name) => flow.nodeMap.get(name)
              case _ => None
            }
          execNode(nextNode, NodeResult(n, context, exitPort) :: subFlowResults.reverse ::: path)

        case Some(n @ ParSubFlowNode(name, nodes, nextNodeName)) =>
          logger.info("Executing: "+n)
          val subFlow: Flow = new Flow(name, nodes)
          val subFlowRunner = new ParallelFlowRunner(subFlow)

          import ExecutionContext.Implicits.global
          val futures: List[Future[List[NodeResult]]] = 
            for(context <- subContexts) 
              yield future { subFlowRunner.execute(context, subContexts) }

          import scala.concurrent.duration.Duration
          val subFlowResults = for(f <- futures; r <- Await.result(f, Duration.Inf)) yield r
          
          val nextNode = flow.nodeMap.get(nextNodeName)
          execNode(nextNode, NodeResult(n, context, ParSubFlowExit()) :: subFlowResults.reverse ::: path)
      }

    }
    
    logger.info("Starting flow: "+flow.name)
    execNode(flow.nodes.headOption, Nil)
  }

}

