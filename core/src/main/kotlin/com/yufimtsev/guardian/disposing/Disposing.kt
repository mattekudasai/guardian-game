package com.yufimtsev.guardian.disposing

import com.badlogic.gdx.utils.Disposable

interface Disposing : Disposable {
    fun <T : Disposable> remember(block: () -> T): Lazy<T>
}
