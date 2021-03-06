package com.waz.zclient.legalhold

import android.os.Bundle
import android.view.{LayoutInflater, View, ViewGroup}
import androidx.recyclerview.widget.{LinearLayoutManager, RecyclerView}
import com.waz.model.UserId
import com.waz.utils.returning
import com.waz.zclient.{FragmentHelper, R}
import com.waz.zclient.common.controllers.ThemeController
import com.waz.zclient.common.views.PickableElement
import com.waz.zclient.conversation.ConversationController
import com.waz.zclient.usersearch.views.{PickerSpannableEditText, SearchEditText}
import com.wire.signals.Signal

class AllLegalHoldSubjectsFragment extends FragmentHelper {

  private lazy val legalHoldController    = inject[LegalHoldController]
  private lazy val conversationController = inject[ConversationController]

  private lazy val users: Signal[Seq[UserId]] =
    for {
      convId <- conversationController.currentConvId
      users  <- legalHoldController.legalHoldUsers(convId)
    } yield users

  private lazy val adapter = returning(new LegalHoldUsersAdapter(users.map(_.toSet))) {
    _.onClick.pipeTo(legalHoldController.onLegalHoldSubjectClick)
  }

  private lazy val searchBox = view[SearchEditText](R.id.search_box)

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle) =
    inflater.inflate(R.layout.all_participants_fragment, container, false)

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    setUpRecyclerView()
    setUpSearchBox()
  }

  private def setUpRecyclerView(): Unit =
    returning(findById[RecyclerView](R.id.recycler_view)) { recyclerView =>
      recyclerView.setLayoutManager(new LinearLayoutManager(getContext))
      recyclerView.setAdapter(adapter)
    }

  private def setUpSearchBox(): Unit =
    searchBox.foreach { sb =>
      sb.applyDarkTheme(inject[ThemeController].isDarkTheme)
      sb.setCallback(new PickerSpannableEditText.Callback {
        override def onRemovedTokenSpan(element: PickableElement): Unit = {}

        override def afterTextChanged(s: String): Unit = {
          adapter.filter ! s
        }
      })
    }
}

object AllLegalHoldSubjectsFragment {

  val Tag = "AllLegalHoldSubjectsFragment"

  def newInstance() = new AllLegalHoldSubjectsFragment
}
