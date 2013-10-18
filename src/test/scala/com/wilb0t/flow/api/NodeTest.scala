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
    val results = flowRunner.execute(FlowContext("Main"), List(FlowContext("Ctx1"), FlowContext("Ctx2")))

    logger.info("Got Results: "+results)

    assert(results.size == 10)

    assert(results(0).node.name == "HeadNode")
    assert(results(0).context == FlowContext("Main"))
    assert(results(0).exitPort == PassExit())

    assert(results(1).node.name == "CompFlowEnd")
    assert(results(1).context == FlowContext("Main"))
    assert(results(1).exitPort == PassExit())

    assert(results(2).node.name == "CompFlow")
    assert(results(2).context == FlowContext("Main"))
    assert(results(2).exitPort == PassExit())

    assert(results(3).node.name == "MidNode")
    assert(results(3).context == FlowContext("Main"))
    assert(results(3).exitPort == FailExit())

    assert(
      results.exists( 
        result => 
          result.node.name == "SubFlowHead" 
          && result.context == FlowContext("Ctx1") 
          && result.exitPort == PassExit()
      )
    )

    assert(
      results.exists( 
        result => 
          result.node.name == "SubFlowEndNode" 
          && result.context == FlowContext("Ctx1") 
          && result.exitPort == FailExit()
      )
    )

    assert(
      results.exists( 
        result => 
          result.node.name == "SubFlowHead" 
          && result.context == FlowContext("Ctx2") 
          && result.exitPort == PassExit()
      )
    )

    assert(
      results.exists( 
        result => 
          result.node.name == "SubFlowEndNode" 
          && result.context == FlowContext("Ctx2") 
          && result.exitPort == FailExit()
      )
    )

    assert(
      results.exists( 
        result => 
          result.node.name == "SubFlow" 
          && result.context == FlowContext("Main") 
          && result.exitPort == ParSubFlowExit()
      )
    )

    assert(results.last.node.name == "EndNode")
    assert(results.last.context == FlowContext("Main"))
    assert(results.last.exitPort == PassExit())

  }
}

