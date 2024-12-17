package hello;

import org.jspecify.annotations.NonNull;

public class Hello {
    public static void main(String[] args) {
        sayHello(null);
    }

    public static void sayHello(@NonNull String name) {
        System.out.println("hello.Hello, " + name + "!");
    }
}
