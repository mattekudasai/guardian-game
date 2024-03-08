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

    override fun dispose() {
        System.out.println("Disposing $this")
        disposables.forEach { it.dispose() }
    }
}
