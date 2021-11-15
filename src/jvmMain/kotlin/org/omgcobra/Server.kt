package org.omgcobra

import kotlinx.html.*

fun HTML.index() {
  head {
    title("Simple Queue")
  }
  body {
    div {
      id = "root"
    }
    script(src = "/static/student-queue.js") {}
  }
}