package org.omgcobra

import io.ktor.http.cio.websocket.*
import kotlinext.js.js
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.html.InputType
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.omgcobra.hooks.useStateWithStorage
import org.omgcobra.hooks.useWebSocket
import org.w3c.dom.Element
import react.*
import react.dom.*
import styled.*

external interface ChatProps : PropsWithChildren {
  var name: String
}

@ExperimentalSerializationApi
val Chat: FunctionComponent<ChatProps> = functionComponent(::Chat.name) { props ->
  val endOfChat = useRef<Element>(null)

  fun scrollToBottom() {
    endOfChat.current?.scrollIntoView(js { behavior = "smooth" })
  }

  val (messages, updateMessages) = useReducer({ list: List<ChatMessage>, param: ChatMessage -> list + param }, listOf())
  var text by useState("")
  val author by useStateWithStorage("name", props.name)
  var participants by useState(setOf<String>())
  val websocket = useWebSocket("chat", author) { data ->
    when (val message = Json.decodeFromString<ChatEvent>(data)) {
      is ChatMessage -> {
        val isAtEnd = endOfChat.current?.parentElement?.let { it.scrollTop + it.clientHeight + 1 >= it.scrollHeight } ?: false
        updateMessages(message)
        if (isAtEnd) scrollToBottom()
      }
      is JoinChat -> {
        message.previousMessages.forEach(updateMessages)
        scrollToBottom()
      }
      is UpdateParticipants -> {
        participants = message.participants
      }
    }
  }

  fun send(text: String) {
    MainScope().launch {
      websocket.current?.send(Json.encodeToString(ChatMessage(text, author)))
    }
  }

  fun handleSubmit() {
    send(text)
    text = ""
  }

  fun RBuilder.renderMessages() {
    messages.forEachIndexed { index, message ->
      val mine = message.author == author
      val prev = if (index < 1 || messages.size <= index) null else messages[index - 1]
      val showAuthor = prev?.author != message.author

      div {
        if (showAuthor) {
          styledDiv {
            css {
              fontWeight = FontWeight.bold
              if (mine) textAlign = TextAlign.end
            }
            +message.author
          }
        }
        styledDiv {
          css {
            display = Display.flex
            if (mine) flexDirection = FlexDirection.rowReverse
          }
          styledDiv {
            css {
              width = LinearDimension.fitContent
            }
            +message.text
          }
          styledDiv {
            css {
              flex(1.0, flexBasis = 4.rem)
            }
          }
        }
      }
    }
    div { ref = endOfChat }
  }

  fun RBuilder.renderParticipants() {
    styledDiv {
      css {
        flexDirection = FlexDirection.row
      }
      h4 { +"Users" }
      small { +"${participants.size} total"}
    }
    participants.forEach { user ->
      div { +user }
    }
  }

  styledDiv {
    css {
      display = Display.flex
      flexDirection = FlexDirection.column
      height = 100.pct
    }
    div {
      renderParticipants()
    }
    styledMain {
      css {
        flex(1.0)
        overflow = Overflow.auto
        width = 80.vw
        alignSelf = Align.center
      }
      renderMessages()
    }
    div {
      input(type = InputType.text) {
        attrs {
          value = text
          autoFocus = true
          onChange = {
            text = it.target.asDynamic().value as String
          }
          onKeyDown = {
            if (it.key.equals("enter", ignoreCase = true) && text != "") {
              handleSubmit()
              it.preventDefault()
            }
          }
        }
      }
      input(type = InputType.button) {
        attrs {
          onClick = { handleSubmit() }
          value = "Send"
        }
      }
    }
  }
}
