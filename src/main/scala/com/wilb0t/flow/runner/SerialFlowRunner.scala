package com.wilb0t.flow.runner

import com.wilb0t.flow.api._
import com.weiglewilczek.slf4s.Logging

import scala.annotation.tailrec

/** The SerialFlowRunner implementation executes all 
  * [[com.wilb0t.flow.api.ParSubFlowNode]] instances in serial
  * using each subContext supplied to the execute method.
  *
  * All nodes are executed in the calling thread (execute blocks)
  *
  * [[com.wilb0t.flow.api.Flow]] execution continues until
  * a None is encountered as the result of applying a 
  * [[com.wilbot.flow.api.NextNode]] to an ExitPort resulting
  * from exectuing an Action
  */
class SerialFlowRunner extends FlowRunner with Logging {

  def execute(
      flow: Flow,
      context: FlowContext, 
      subContexts: List[FlowContext]
    )
    : List[NodeResult] = {

    /** Recursively execute nodes until it gets a None
      * Returns the list of [[com.wilb0t.flow.api.NodeResult]]s for 
      * the executed Actions/Contexts.
      */
    @tailrec
    def execNode(
        node: Option[Node], 
        path: List[NodeResult]
      )
      : List[NodeResult] = {

      node match {
        case None => 
          logger.info("Flow finished")
          path.reverse

        //Executing an EndNode always results in ending execution of the current flow
        case Some(n @ EndNode(name, action)) =>
          logger.info("Executing: "+n)
          val exitPort = action(context)
          logger.info("Got exit port: "+exitPort)
          (NodeResult(n, context, exitPort) :: path).reverse

        case Some(n @ ActionNode(name, action, getNextNode)) =>
          logger.info("Executing: "+n)
          val exitPort = action(context)
          val nextNodeName = getNextNode(exitPort)
          logger.info("Got exit port: "+exitPort)
          logger.info("Next node: "+nextNodeName)
          val nextNode = 
            nextNodeName match {
              case Some(name) => flow.nodeMap.get(name)
              case _ => None
            }
          execNode(nextNode, NodeResult(n, context, exitPort) :: path)

        //SubFlowNode execution creates a new instance of this runner
        // and runs the subflow with the same context and subcontexts
        case Some(n @ SubFlowNode(name, nodes, getNextNode)) =>
          logger.info("Executing: "+n)
          val subFlow = new Flow(name, nodes)
          val subFlowRunner = new SerialFlowRunner()

          val subFlowResults = subFlowRunner.execute(subFlow, context, subContexts)

          val exitPort = path.last.exitPort
          logger.info("Got exit port: "+exitPort)
          val nextNodeName = getNextNode(exitPort)
          logger.info("Next node: "+nextNodeName)
          val nextNode = 
            nextNodeName match {
              case Some(name) => flow.nodeMap.get(name)
              case _ => None
            }
          execNode(nextNode, NodeResult(n, context, exitPort) :: subFlowResults.reverse ::: path)

        //ParSubFlowNodes executing creates a new instance of this runner
        // and runs the subflow for each subcontext
        //
        // The original subcontext is forwarded to each par subflow
        // so it should be able to execute sub sub par subflows
        // That seems like a bad idea tho...
        case Some(n @ ParSubFlowNode(name, nodes, nextNodeName)) =>
          logger.info("Executing: "+n)
          val subFlow: Flow = new Flow(name, nodes)
          val subFlowRunner = new SerialFlowRunner()

          val subFlowResults = 
            subContexts.flatMap{ subFlowRunner.execute(subFlow, _, subContexts) }
         
          val nextNode = flow.nodeMap.get(nextNodeName)
          execNode(nextNode, NodeResult(n, context, ParSubFlowExit()) :: subFlowResults.reverse ::: path)
      }

    }
    
    logger.info("Starting flow: "+flow.name)
    execNode(flow.nodes.headOption, Nil)
  }

}
