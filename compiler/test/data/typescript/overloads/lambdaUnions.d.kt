// [test] lambdaUnions.kt
@file:Suppress("INTERFACE_WITH_SUPERCLASS", "OVERRIDING_FINAL_MEMBER", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "CONFLICTING_OVERLOADS")

import kotlin.js.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import org.w3c.dom.parsing.*
import org.w3c.dom.svg.*
import org.w3c.dom.url.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

typealias HookMethod<O, R> = (options: O) -> dynamic

typealias BeforeHook<O> = (options: O) -> Unit

typealias ErrorHook<O, E> = (error: E, options: O) -> Unit

typealias AfterHook<O, R> = (result: R, options: O) -> Unit

typealias WrapHook<O, R> = (hookMethod: HookMethod<O, R>, options: O) -> dynamic

external interface HookCollection {
    fun remove(name: String, hook: BeforeHook<Any>)
    fun remove(name: String, hook: ErrorHook<Any, Any>)
    fun remove(name: String, hook: WrapHook<Any, Any>)
}