package harlequinmettle.gaero;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;

public class TwoNumbers implements Serializable{
public float low;
public float high;

public TwoNumbers(float low,float high){
		this.low =  new BigDecimal(low).round(new MathContext(3)).floatValue();
		this.high =  new BigDecimal(high).round(new MathContext(3)).floatValue();
	}

@Override
public String toString() {
	return "[("+low+")-("+high+")]";
}
	
}
