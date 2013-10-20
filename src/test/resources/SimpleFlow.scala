import com.wilb0t.flow.api._
import com.wilb0t.flow.impl.SimplFlowActions._

val passAction = new PassAction()
val failAction = new FailAction()

Flow("TestFlow", List[Node](

  ActionNode("HeadNode", passAction, {
    case PassExit() => Some("CompFlow")
    case _ => Some("EndNode")
  }),

  SubFlowNode("CompFlow", List[Node](
      EndNode("CompFlowEnd", passAction)
    ), {
      case PassExit() => Some("MidNode")
      case _ => Some("EndNode")
  }),

  ActionNode("MidNode", failAction, {
    case PassExit() => Some("SubFlow")
    case FailExit() => Some("SubFlow")
    case _ => Some("EndNode")
  }),

  ParSubFlowNode("SubFlow", List[Node](
      ActionNode("SubFlowHead", passAction, {
        case _ => Some("SubFlowEndNode")
      }),
      EndNode("SubFlowEndNode", failAction)
      ),
      "EndNode"
  ),

  EndNode("EndNode", passAction)
  )
)

