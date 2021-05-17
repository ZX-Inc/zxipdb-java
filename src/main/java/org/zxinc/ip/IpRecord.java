package org.zxinc.ip;

public class IpRecord {
	IpAddress ipAddress;
	IpRange ipRange;
	String country;
	String local;
	String display;
/*
	String cc;
	String l1;
	String l2;
	String l3;
	String l4;
	String operator;
	String org;
	String desc;
*/
	@Override
	public String toString() {
		return "Addr: " + ipAddress + ", Range: " + ipRange + ", Display: " + display;
	}
}
