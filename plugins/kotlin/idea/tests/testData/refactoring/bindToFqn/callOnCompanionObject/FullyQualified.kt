// BIND_TO test.B
// BIND_RESULT B
package test

class A {
    companion object {
        fun foo() { }
    }
}

class B {
    companion object {
        fun foo() { }
    }
}

fun foo() {
    test.<caret>A.foo()
}