// PROBLEM: Redundant lambda arrow
// WITH_STDLIB

fun main() {
    registerHandler(handlers = *arrayOf(
        { _ -> },
        { it<caret> -> }
    ))
}

fun registerHandler(vararg handlers: (String) -> Unit) {
    handlers.forEach { it.invoke("hello") }
}
// KT-76580
// IGNORE_K2