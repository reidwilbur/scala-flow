package com.wilb0t.flow

package object api {
  type Action = (FlowContext) => ExitPort
  type NextNode = (ExitPort) => Option[String]
}
