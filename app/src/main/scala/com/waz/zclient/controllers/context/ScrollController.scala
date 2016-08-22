/**
  * Wire
  * Copyright (C) 2016 Wire Swiss GmbH
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */
package com.waz.zclient.controllers.context

import com.waz.ZLog.{LogTag, debug, logTagFor}
import com.waz.model.ConvId
import com.waz.service.ZMessaging
import com.waz.utils.events.Signal
import com.waz.zclient.controllers.global.KeyboardController
import com.waz.zclient.{Injectable, Injector, WireContext}

class ScrollController(implicit cxt: WireContext, inj: Injector) extends Injectable {

  implicit val eventContext = cxt.eventContext

  import ScrollController._

  val zms = inject[Signal[ZMessaging]]
  val keyboardVisibility = inject[KeyboardController].keyboardVisibility

  private var currentSelectedConv = Option.empty[ConvId]
  val selectedConv = zms.flatMap(_.convsStats.selectedConversationId).collect { case Some(convId) => convId }

  val msgCursor = Signal(zms, selectedConv).flatMap {
    case (z, convId) => z.messagesStorage.getEntries(convId)
  }

  val lastReadIndex = msgCursor.map(_.lastReadIndex)

  val lastMessageIndex = msgCursor.map(_.size - 1)

  val lastVisibleItemIndex = Signal(-1)

  val scrolledToBottom = lastVisibleItemIndex.zip(lastMessageIndex).map {
    case (lv, lm) if lv == lm => true
    case _ => false
  }

  private var currentScrollPos = -1
  val scrollPosition = for {
    sB <- scrolledToBottom
    lm <- lastMessageIndex
    kB <- keyboardVisibility
  } yield {
    debug(s"ScrolledToBottom?: $sB, total count: $lm, keyboardVisible?: $kB")
    if (sB) lm
    else currentScrollPos // not scrolled to bottom
  }
  scrollPosition (currentScrollPos = _)
}

object ScrollController {
  implicit val logTag: LogTag = logTagFor[ScrollController]
}