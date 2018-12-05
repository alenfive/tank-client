package com.source3g.tankclient.action;

public abstract class AbstractActiion<T,V> {
    abstract NodeType process(T params, V actions);
}
