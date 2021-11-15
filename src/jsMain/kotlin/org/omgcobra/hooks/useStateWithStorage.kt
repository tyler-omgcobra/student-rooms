package org.omgcobra.hooks

import kotlinx.browser.localStorage
import org.w3c.dom.*
import react.useState
import kotlin.reflect.KProperty

class StorageDelegate<T>(
    private val storage: Storage,
    private val key: String,
    private val default: T,
    private val encoder: (T) -> String,
    private val decoder: (String?) -> T?,
    private val updater: (T) -> Unit
) {
  operator fun component1() = decoder(storage[key]) ?: default
  operator fun component2(): (T) -> Unit = {
    storage[key] = encoder(it)
    updater(it)
  }

  operator fun getValue(thisRef: Any?, property: KProperty<*>) = component1()
  operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    component2()(value)
  }
}

fun <T> useStateWithStorage(
    key: String,
    default: T,
    storage: Storage = localStorage,
    encoder: (T) -> String = { JSON.stringify(it) },
    decoder: (String?) -> T? = { it?.let { it1 -> JSON.parse<T>(it1) } }
): StorageDelegate<T> {
  var state by useState(default)

  return StorageDelegate(storage, key, state, encoder, decoder) {
    state = it
  }
}