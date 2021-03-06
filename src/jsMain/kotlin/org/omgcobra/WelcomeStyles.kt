package org.omgcobra

import kotlinx.css.*
import styled.StyleSheet

val globalStyles: RuleSet = {
  universal {
    boxSizing = BoxSizing.borderBox
    wordBreak = WordBreak.breakWord
  }
  body {
    margin(0.px)
  }
  "#root" {
    height = 100.vh
    width = 100.vw
  }
  "a.active" {
    textTransform = TextTransform.uppercase
  }
}

object WelcomeStyles : StyleSheet("WelcomeStyles", isStatic = true) {
  val textContainer by css {
    padding(5.px)

    backgroundColor = rgb(8, 97, 22)
    color = rgb(56, 246, 137)
  }

  val textInput by css {
    margin(vertical = 5.px)

    fontSize = 14.px
  }
} 
