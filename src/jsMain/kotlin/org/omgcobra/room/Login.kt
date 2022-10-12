package org.omgcobra.room

import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.html.*
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import org.omgcobra.UserCredential
import org.omgcobra.api.doPost
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.*
import styled.styledInput

external interface LoginProps : Props {
  var loginHandler: (String, String?) -> Unit
}

val Login: FunctionComponent<LoginProps> = functionComponent(::Login.name) { props ->
  var password by useState("")
  val (username, token) = useContext(AuthenticationContext)

  if (token == null) {
    p {
      +"Username:"
      styledInput {
        attrs {
          type = InputType.text
          value = username
          onChangeFunction = { event ->
            props.loginHandler((event.target as HTMLInputElement).value, token)
          }
        }
      }
    }
    if (username == "spruitt1") {
      p {
        +"Password"
        styledInput {
          attrs {
            type = InputType.password
            value = password
            onChangeFunction = { event ->
              password = (event.target as HTMLInputElement).value
            }
          }
        }
      }
    }
    button {
      attrs {
        onClickFunction = {
          MainScope().launch {
            try {
              val newToken = doPost<Map<String, String>>(UserCredential(username, password), "login")["token"]
              props.loginHandler(username, newToken)
            } catch (e: Exception) {
              window.alert(e.message.toString())
            }
          }
        }
      }
      +"Login"
    }
  } else {
    +username
    +" - "
    button {
      attrs {
        onClickFunction = {
          props.loginHandler(username, null)
        }
      }
      +"Logout"
    }
  }
}