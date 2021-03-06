/**
 * Wire
 * Copyright (C) 2018 Wire Swiss GmbH
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
package com.waz.zclient.messages.parts

import android.content.Context
import android.util.AttributeSet
import com.waz.log.BasicLogging.LogTag.DerivedLogTag
import com.waz.model.{Name, UserId}
import com.waz.threading.Threading
import com.wire.signals.Signal
import com.waz.zclient.common.controllers.global.{AccentColorController, ClientsController}
import com.waz.zclient.common.controllers.{BrowserController, ScreenController}
import com.waz.zclient.log.LogUI._
import com.waz.zclient.messages.UsersController.DisplayName.{Me, Other}
import com.waz.zclient.messages.{MessageViewPart, MsgPart, SystemMessageView, UsersController}
import com.waz.zclient.participants.ParticipantsController
import com.waz.zclient.participants.fragments.SingleParticipantFragment
import com.waz.zclient.utils.ContextUtils._
import com.waz.zclient.{R, SpinnerController, ViewHelper}
import com.waz.threading.Threading._
import com.waz.zclient.legalhold.LegalHoldController
import com.waz.zclient.utils._

class OtrMsgPartView(context: Context, attrs: AttributeSet, style: Int)
  extends SystemMessageView(context, attrs, style)
    with MessageViewPart
    with ViewHelper
    with DerivedLogTag {

  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null, 0)

  import com.waz.api.Message.Type._

  override val tpe = MsgPart.OtrMessage

  private lazy val screenController       = inject[ScreenController]
  private lazy val participantsController = inject[ParticipantsController]
  private lazy val browserController      = inject[BrowserController]
  private lazy val selfUserId             = inject[Signal[UserId]]
  private lazy val clientsController      = inject[ClientsController]
  private lazy val spinnerController      = inject[SpinnerController]
  private lazy val legalHoldController    = inject[LegalHoldController]
  private lazy val memberIsJustSelf       = users.memberIsJustSelf(message)
  private lazy val affectedUserName       = message.map(_.userId).flatMap(users.displayName)

  private val accentColor = inject[AccentColorController]
  private val users       = inject[UsersController]
  private val msgType     = message.map(_.msgType)

  private lazy val memberNames = for {
    selfUserId <- selfUserId
    msg        <- message
    names      <- users.getMemberNamesSplit(msg.members, selfUserId)
    mainString =  users.membersNamesString(names.main, separateLast = names.others.isEmpty && !names.andYou)
  } yield (mainString, names.others.size, names.andYou)

  (msgType.map {
    case OTR_ERROR | OTR_IDENTITY_CHANGED | HISTORY_LOST | RESTRICTED_FILE => Some(R.drawable.red_alert)
    case OTR_ERROR_FIXED                                                   => Some(R.drawable.ic_check)
    case OTR_VERIFIED                                                      => Some(R.drawable.shield_full)
    case OTR_UNVERIFIED | OTR_DEVICE_ADDED | OTR_MEMBER_ADDED              => Some(R.drawable.shield_half)
    case SESSION_RESET                                                     => Some(R.drawable.ic_iconographyemail)
    case LEGALHOLD_ENABLED | LEGALHOLD_DISABLED                            => Some(R.drawable.ic_legal_hold_active)
    case STARTED_USING_DEVICE                                              => None
    case _                                                                 => None
  }).onUi {
    case None       => setIcon(null)
    case Some(icon) => setIcon(icon)
  }

  private lazy val errorCodeAndFingerprint = Signal.zip(message.map(_.userId), message.map(_.error)).flatMap {
    case (sender, Some(error)) =>
      clientsController.client(sender, error.clientId).map(c => (error.code.toString, c.fold("")(_.displayId)))
    case _ => Signal.const(("", ""))
  }

  private val msgString = msgType.flatMap {
    case HISTORY_LOST =>
      Signal.const(getString(R.string.content__otr__lost_history))
    case STARTED_USING_DEVICE =>
      Signal.const(getString(R.string.content__otr__start_this_device__message))
    case OTR_VERIFIED  =>
      Signal.const(getString(R.string.content__otr__all_fingerprints_verified))
    case OTR_ERROR =>
      affectedUserName.map {
        case Me          => getString(R.string.content__otr__message_error_you)
        case Other(name) => getString(R.string.content__otr__message_error, name)
      }
    case OTR_ERROR_FIXED =>
      affectedUserName.flatMap {
        case Me =>
          Signal.const(getString(R.string.content__otr__message_error_you))
        case Other(name) =>
          errorCodeAndFingerprint.map {
            case (code, fprint) => getString(R.string.content__otr__message_error_fixed, name, name, code, fprint)
          }
      }
    case OTR_IDENTITY_CHANGED =>
      affectedUserName.map {
        case Me          => getString(R.string.content__otr__identity_changed_error_you)
        case Other(name) => getString(R.string.content__otr__identity_changed_error, name.toUpperCase)
      }
    case SESSION_RESET =>
      affectedUserName.map {
        case Other(name) => getString(R.string.content__session_reset, name)
        case _ => ""
      }
    case OTR_UNVERIFIED =>
      memberIsJustSelf.flatMap {
        case true  => Signal.const(getString(R.string.content__otr__your_unverified_device__message))
        case false => memberNames.map { case (main, _, _) => getString(R.string.content__otr__unverified_device__message, main) }
      }
    case OTR_DEVICE_ADDED =>
      memberNames.map {
        case (main, 0, true)       => getString(R.string.content__otr__someone_and_you_added_new_device__message, main)
        case (main, 0, false)      => getString(R.string.content__otr__someone_added_new_device__message, main)
        case (main, others, true)  => getString(R.string.content__otr__someone_others_and_you_added_new_device__message, main, others.toString)
        case (main, others, false) => getString(R.string.content__otr__someone_and_others_added_new_device__message, main, others.toString)
      }
    case OTR_MEMBER_ADDED =>
      Signal.const(getString(R.string.content__otr__new_member__message))
    case RESTRICTED_FILE =>
      Signal.zip(affectedUserName, message.map(_.name)).map {
        case (Other(name), _)     => getString(R.string.file_restrictions__receiver_error, name)
        case (_, Some(Name(ext))) => getString(R.string.file_restrictions__sender_error, ext)
        case _                    => getString(R.string.file_restrictions__sender_error, "")
      }
    case LEGALHOLD_ENABLED =>
      Signal.const(getString(R.string.content__otr__legalhold_enabled__message))
    case LEGALHOLD_DISABLED =>
      Signal.const(getString(R.string.content__otr__legalhold_disabled__message))
    case _ =>
      Signal.const("")
  }

  Signal.zip(message, msgString, accentColor.accentColor, memberIsJustSelf).onUi { case (msg, text, color, isMe) =>
    setTextWithLink(text, color.color) {
      (msg.msgType, isMe, msg.error) match {
        case (OTR_UNVERIFIED | OTR_DEVICE_ADDED | OTR_MEMBER_ADDED, true, _)  =>
          screenController.openOtrDevicePreferences()
        case (OTR_UNVERIFIED | OTR_DEVICE_ADDED | OTR_MEMBER_ADDED, false, _) =>
          participantsController.onShowParticipants ! Some(SingleParticipantFragment.DevicesTab.str)
        case (STARTED_USING_DEVICE, _, _) =>
          screenController.openOtrDevicePreferences()
        case (OTR_ERROR, false, Some(error)) =>
          spinnerController.showDimmedSpinner(show = true)
          clientsController
            .resetSession(msg.userId, error.clientId, Some(msg.convId))
            .foreach(_ => spinnerController.hideSpinner())(Threading.Ui)
        case (OTR_IDENTITY_CHANGED, _, _) =>
          browserController.openDecryptionError2()
        case (LEGALHOLD_ENABLED, _, _) =>
          legalHoldController.onShowConversationLegalHoldInfo ! (())
        case _ =>
          info(l"unhandled help link click for $msg")
      }
    }
  }

}
