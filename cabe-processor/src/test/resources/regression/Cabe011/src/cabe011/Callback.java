package cabe11;

public interface Callback<T,U> {
    U call(T arg);
}
