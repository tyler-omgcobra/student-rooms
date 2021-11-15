package org.omgcobra

import org.omgcobra.hooks.useStateWithStorage
import kotlinx.browser.sessionStorage
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.attrs
import react.dom.br
import styled.css
import styled.styledDiv
import styled.styledInput

external interface WelcomeProps : PropsWithChildren {
  var name: String
}

class Obj(
    val name: String,
    val count: Int
)

val Welcome: FunctionComponent<WelcomeProps> = functionComponent(::Welcome.name) { props ->
  var state by useStateWithStorage("name", props.name)
  var session by useStateWithStorage<String?>("favorite", null, sessionStorage)
  var obj by useStateWithStorage("obj", Obj("ooh", 0), sessionStorage)

  styledDiv {
    css {
      +WelcomeStyles.textContainer
    }
    +"Hello, $state."
    br {}
    +when (session) {
      null -> "I don't know your favorite yet."
      else -> "I remember your favorite is $session"
    }
  }
  styledDiv {
    +"${obj.name} is ${obj.count}"
  }
  styledInput {
    css {
      +WelcomeStyles.textInput
    }
    attrs {
      type = InputType.text
      value = state
      onChangeFunction = { event ->
        state = (event.target as HTMLInputElement).value
      }
    }
  }
  br {}
  styledInput {
    css {
      +WelcomeStyles.textInput
    }
    attrs {
      type = InputType.text
      value = session ?: ""
      onChangeFunction = { event ->
        session = (event.target as HTMLInputElement).value.takeUnless { it.isBlank() }
      }
    }
  }
  styledInput {
    attrs {
      type = InputType.button
      onClickFunction = {
        obj = Obj(obj.name, obj.count + 1)
      }
    }
  }
}