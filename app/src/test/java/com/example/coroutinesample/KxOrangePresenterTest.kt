package com.example.coroutinesample

import co.metalab.coroutinesample.KxOrangePresenter
import co.metalab.coroutinesample.KxOrangeView
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import kotlinx.coroutines.experimental.Unconfined
import org.junit.Test

class KxOrangePresenterTest {
    private val kxOrangeView = mock<KxOrangeView>()

    @Test
    fun orangeTaskIsDone() {
        val kxOrangePresenter = KxOrangePresenter(kxOrangeView,
            coroutineDispatcher = Unconfined)

        kxOrangePresenter.startLongRunningOrangeTask()

        verify(kxOrangeView).setOrangeButtonText("Orange task in progress...")
        verify(kxOrangeView).setOrangeButtonText("Orange task is done")
    }
}