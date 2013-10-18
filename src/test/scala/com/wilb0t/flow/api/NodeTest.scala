package com.wilb0t.flow.api

import org.scalatest.FunSuite
import com.weiglewilczek.slf4s.Logging

case class PassExit() extends ExitPort {
  val description = "Node Passed"
}

case class FailExit() extends ExitPort {
  val description = "Node Failed"
}

class PassAction extends Action with Logging {
  override def execute(context: FlowContext): ExitPort = {
    logger.info("Emitting PassExit")

    PassExit()
  }
}

class FailAction extends Action with Logging {
  override def execute(context: FlowContext): ExitPort = {
    logger.info("Emitting FailExit")

    FailExit()
  }
}

class FlowTest extends FunSuite with Logging {

  test("simple flow") {

    val flow = 
      new Flow("TestFlow", List[Node](
            ActionNode("HeadNode", new PassAction(), Map[ExitPort, String](
             ( PassExit() -> "CompFlow" ),
             ( FailExit() -> "EndNode" )
            )),
            SubFlowNode("CompFlow", List[Node](
                EndNode("CompFlowEnd", new PassAction())
              ), 
              Map[ExitPort, String](
               ( PassExit() -> "MidNode" ),
               ( FailExit() -> "EndNode" )
            )),
            ActionNode("MidNode", new FailAction(), Map[ExitPort, String](
             ( PassExit() -> "SubFlow" ),
             ( FailExit() -> "SubFlow" )
            )),
            ParSubFlowNode("SubFlow", List[Node](
                ActionNode("SubFlowHead", new PassAction(), Map[ExitPort, String](
                 ( PassExit() -> "SubFlowEndNode" ),
                 ( FailExit() -> "SubFlowEndNode" )
                )),
                EndNode("SubFlowEndNode", new FailAction())
                ),
                "EndNode"
            ),
            EndNode("EndNode", new PassAction())
            )
          )

    val flowRunner = new FlowRunner(flow)
    val path = flowRunner.execute(FlowContext("Main"), List(FlowContext("Ctx1"), FlowContext("Ctx2")))

    logger.info("Got path: "+path)

    //assert(path.size == 3)
    //assert(path(0)._1.name == "HeadNode")
    //assert(path(0)._2.exists( { case (context, exitport) => context.name == "Main" && exitport == PassExit() } ))

    //assert(path(1)._1.name == "MidNode")
    //assert(path(1)._2.exists( { case (context, exitport) => context.name == "Main" && exitport == FailExit() } ))

    //assert(path(2)._1.name == "EndNode")
    //assert(path(2)._2.exists( { case (context, exitport) => context.name == "Main" && exitport == PassExit() } ))
  }
}

