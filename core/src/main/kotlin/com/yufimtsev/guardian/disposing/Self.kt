package com.yufimtsev.guardian.disposing

import com.badlogic.gdx.utils.Disposable

class Self : Disposing {
    private val disposables = mutableListOf<Disposable>()
    override fun <T : Disposable> remember(block: () -> T): Lazy<T> =
        lazy {
            val disposable = block()
            disposables += disposable
            disposable
        }

    override fun <T : Disposable> T.autoDisposing(): T =
        this.also { disposables += it }

    override fun dispose() {
        disposables.forEach { it.dispose() }
    }
}
