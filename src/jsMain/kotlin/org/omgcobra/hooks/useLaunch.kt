package org.omgcobra.hooks

import kotlinx.coroutines.*
import react.useEffect

fun useLaunch(vararg dependencies: dynamic, message: String = "Cleanup", block: suspend CoroutineScope.() -> Unit) {
  useEffect(*dependencies) {
    val scope = MainScope()
    scope.launch(block = block)
    cleanup {
      console.log("Cleaning up the launch")
      scope.cancel(message)
    }
  }
}