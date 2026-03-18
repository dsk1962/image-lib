package com.dkgeneric.pdf.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Rectangle {
	private float left;
	private float right;
	private float top;
	private float bottom;
	
	public float getX()
	{
		return left;
	}
	public float getY()
	{
		return top;
	}
	public float getWidth()
	{
		return Math.abs(left-right);
	}
	public float getHeight()
	{
		return Math.abs(top-bottom);
	}

}
