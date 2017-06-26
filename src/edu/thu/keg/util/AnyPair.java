package edu.thu.keg.util;

public class AnyPair<T1, T2> {
	T1 _o1 = null;
	T2 _o2 = null;
	
	public AnyPair(T1 o1, T2 o2){
		_o1 = o1;
		_o2 = o2;
	}

	public T1 get_o1() {
		return _o1;
	}

	public T2 get_o2() {
		return _o2;
	}
	
}
