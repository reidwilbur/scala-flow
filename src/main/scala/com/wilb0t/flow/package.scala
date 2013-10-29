package com.wilb0t.flow

/** Provides classes for creating arbitrary flows of actions.
  *
  * Provides flow runners to execute flows.
  *
  * Defines a DSL and loader for instantiating flows from strings or files.
  *
  * ==Overview==
  * 
  * The main class representing flows is [[com.wilb0t.flow.api.Flow]] which 
  * is made up of a list of nodes [[com.wilb0t.flow.api.Node]].
  *
  * Flows can be created programmatically using the classes in [[com.wilb0t.flow.api]]
  *
  * Flows can also be created by parsing a string or text file using 
  * [[com.wilb0t.flow.loader.Loader]]
  *
  * Instantiated flows can be executed with a FlowRunner which are defined in 
  * [[com.wilb0t.flow.runner]]
  *
  */
package object api {
  /** Actions define the action to be executed for a given flow step 
    * 
    * From an api standpoint, this is just a function of [[com.wilb0t.flow.api.FlowContext]]
    * to [[com.wilb0t.flow.api.ExitPort]]
    *
    */
  type Action = (FlowContext) => ExitPort

  /** NextNode indicates the name of the node to execute next given an 
    * [[com.wilb0t.flow.api.ExitPort]]
    */
  type NextNode = (ExitPort) => Option[String]
}
