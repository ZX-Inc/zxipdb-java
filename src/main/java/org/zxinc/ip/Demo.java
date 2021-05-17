package org.zxinc.ip;

import java.io.IOException;

public class Demo {
	public static void out(Object... a) {
		System.out.print(a[0]);
		for (int i=1; i<a.length; i++) {
			System.out.print(",");
			System.out.print(a[i]);
		}
		System.out.println();
	}

	public static void main() throws IOException {
		Ipdbv4 db4 = new Ipdbv4("F:\\winshell\\qqwry.db");
		out(db4.getVersion());
		Ipdbv6 db6 = new Ipdbv6("F:\\winshell\\ipv6wry.db", db4);
		out(db6.getVersion());

		out(db4.query("1.2.3.4"));
		out(db4.query("255.255.255.255"));
		out(db6.query("::ffff:1111:2222"));
		out(db6.query("2001:db8::ffff:1111:2222"));
		out(db6.query("::1"));
		out(db6.query("2406:840:20::1"));
	}
}

