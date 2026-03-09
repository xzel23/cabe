package cabe011;

public interface Callback<T,U> {
    U call(T arg);
}
