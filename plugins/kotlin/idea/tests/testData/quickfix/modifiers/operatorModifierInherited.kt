// "Add 'operator' modifier" "true"
open class Foo {
    fun get(idx: Int): Any = 5
}

object Bar : Foo()

fun test(): Any {
    return Bar<caret>[5]
}

// FUS_QUICKFIX_NAME: org.jetbrains.kotlin.idea.inspections.AddModifierFixFactory$createAction$1
// FUS_K2_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.AddModifierFix