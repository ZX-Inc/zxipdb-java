package org.zxinc.ip;

import java.math.BigInteger;
import java.util.Arrays;

public class IpAddress {
	private byte[] m_ip = null;
	private boolean m_mapped = false;

	private IpAddress() {
	}

	public IpAddress(IpAddress ip) {
		m_ip = ip.m_ip;
		m_mapped = ip.m_mapped;
	}

	public IpAddress(String ip) {
		m_ip = textToNumericFormatV6(ip);
		if (m_ip == null) m_ip = textToNumericFormatV4(ip);
		if (m_ip == null)
			throw new IllegalArgumentException("非法IP地址");
	}

	public static IpAddress fromBytes(byte[] bytes) {
		if (bytes.length == INADDR4SZ) {
			IpAddress ip = new IpAddress();
			ip.m_ip = bytes;
			return ip;
		} else if (bytes.length == INADDR16SZ) {
			IpAddress ip = new IpAddress();
			ip.m_ip = bytes;
			ip.convertFromIPv4MappedAddress();
			return ip;
		} else {
			throw new IllegalArgumentException("非法IP地址");
		}
	}

	public static IpAddress fromBytesV4(byte[] b) {
		if (b.length == INADDR4SZ) {
			IpAddress ip = new IpAddress();
			ip.m_ip = b;
			return ip;
		} else {
			throw new IllegalArgumentException("非法IP地址");
		}
	}

	public static IpAddress fromBytesV6(byte[] b) {
		if (b.length == INADDR16SZ) {
			IpAddress ip = new IpAddress();
			ip.m_ip = b;
			return ip;
		} else if (b.length < INADDR16SZ) {
			IpAddress ip = new IpAddress();
			ip.m_ip = new byte[INADDR16SZ];
			System.arraycopy(b, 0, ip.m_ip, 0, b.length);
			ip.convertFromIPv4MappedAddress();
			return ip;
		} else {
			throw new IllegalArgumentException("非法IP地址");
		}
	}

	public static IpAddress fromBytesLE(byte[] b) {
		byte[] be = bswap(b);
		return fromBytes(be);
	}

	public static IpAddress fromBytesV4LE(byte[] b) {
		byte[] be = bswap(b);
		return fromBytesV4(be);
	}

	public static IpAddress fromBytesV6LE(byte[] b) {
		byte[] be = bswap(b);
		return fromBytesV6(be);
	}

	public byte[] getBytes() {
		return m_ip;
	}

	public byte[] getBytesLE() {
		return bswap(m_ip);
	}

	public BigInteger getBigInt() {
		byte[] bi = new byte[m_ip.length + 1];
		bi[0] = 0;
		System.arraycopy(m_ip, 0, bi, 1, m_ip.length);
		return new BigInteger(bi);
	}

	public boolean isValid() {
		return m_ip != null;
	}

	public boolean isIpv4() {
		return m_ip != null && m_ip.length == INADDR4SZ;
	}

	public boolean isIpv6() {
		return m_ip != null && m_ip.length == INADDR16SZ;
	}

	public boolean isSameFamily(IpAddress anIp) {
		if (m_ip == null || anIp == null) return false;
		if (this == anIp) return true;
		return m_ip.length == anIp.getBytes().length;
	}

	public boolean wasIPv4MappedAddress() {
		return m_mapped;
	}

	@Override
	public String toString() {
		if (m_ip == null)
			return "";
		else if (m_ip.length == INADDR4SZ)
			return numericToTextFormatV4(m_ip);
		else if (m_ip.length == INADDR16SZ)
			return numericToTextFormatV6(m_ip);
		else
			throw new RuntimeException("INTERNAL ERROR");
	}

	public boolean equals(IpAddress anIp) {
		if (m_ip == null || anIp == null) return false;
		if (this == anIp) return true;
		return Arrays.equals(m_ip, anIp.getBytes());
	}

	public int compareTo(IpAddress anIp) {
		return Arrays.compareUnsigned(m_ip, anIp.getBytes());
	}

	public static byte[] bswap(byte[] b) {
		byte[] rev = new byte[b.length];
		for (int i = 0, j = b.length - 1; j >= 0; i++, j--) {
			rev[j] = b[i];
		}
		return rev;
	}

	public void subOne() {
		BigInteger bi = getBigInt();
		bi = bi.subtract(BigInteger.ONE);
		byte[] b = bi.toByteArray();

		if (b.length >= m_ip.length) {
			System.arraycopy(b, 0, m_ip, 0, m_ip.length);
		} else {
			boolean minus = ((b[0] & 0x80) != 0);
			b[0] &= 0x7F;
			if (minus) {
				Arrays.fill(m_ip, (byte)0xFF);
			} else {
				Arrays.fill(m_ip, (byte)0);
			}
			System.arraycopy(b, 0, m_ip, m_ip.length - b.length, b.length);
		}
	}

	////////////////////////////////////////////////////////////////////////////
	// sun.net.util.IPAddressUtil
	////////////////////////////////////////////////////////////////////////////

	private static final int INADDR4SZ = 4;
	private static final int INADDR16SZ = 16;
	private static final int INT16SZ = 2;

	/*
	 * Converts IPv4 address in its textual presentation form
	 * into its numeric binary form.
	 *
	 * @param src a String representing an IPv4 address in standard format
	 * @return a byte array representing the IPv4 numeric address
	 */
	private static byte[] textToNumericFormatV4(String src)
	{
		byte[] res = new byte[INADDR4SZ];

		long tmpValue = 0;
		int currByte = 0;
		boolean newOctet = true;

		int len = src.length();
		if (len == 0 || len > 15) {
			return null;
		}
		/*
		 * When only one part is given, the value is stored directly in
		 * the network address without any byte rearrangement.
		 *
		 * When a two part address is supplied, the last part is
		 * interpreted as a 24-bit quantity and placed in the right
		 * most three bytes of the network address. This makes the
		 * two part address format convenient for specifying Class A
		 * network addresses as net.host.
		 *
		 * When a three part address is specified, the last part is
		 * interpreted as a 16-bit quantity and placed in the right
		 * most two bytes of the network address. This makes the
		 * three part address format convenient for specifying
		 * Class B net- work addresses as 128.net.host.
		 *
		 * When four parts are specified, each is interpreted as a
		 * byte of data and assigned, from left to right, to the
		 * four bytes of an IPv4 address.
		 *
		 * We determine and parse the leading parts, if any, as single
		 * byte values in one pass directly into the resulting byte[],
		 * then the remainder is treated as a 8-to-32-bit entity and
		 * translated into the remaining bytes in the array.
		 */
		for (int i = 0; i < len; i++) {
			char c = src.charAt(i);
			if (c == '.') {
				if (newOctet || tmpValue < 0 || tmpValue > 0xff || currByte == 3) {
					return null;
				}
				res[currByte++] = (byte) (tmpValue & 0xff);
				tmpValue = 0;
				newOctet = true;
			} else {
				int digit = Character.digit(c, 10);
				if (digit < 0) {
					return null;
				}
				tmpValue *= 10;
				tmpValue += digit;
				newOctet = false;
			}
		}
		if (newOctet || tmpValue < 0 || tmpValue >= (1L << ((4 - currByte) * 8))) {
			return null;
		}
		switch (currByte) {
			case 0:
				res[0] = (byte) ((tmpValue >> 24) & 0xff);
			case 1:
				res[1] = (byte) ((tmpValue >> 16) & 0xff);
			case 2:
				res[2] = (byte) ((tmpValue >>  8) & 0xff);
			case 3:
				res[3] = (byte) ((tmpValue >>  0) & 0xff);
		}
		return res;
	}

	/*
	 * Convert IPv6 presentation level address to network order binary form.
	 * credit:
	 *  Converted from C code from Solaris 8 (inet_pton)
	 *
	 * Any component of the string following a per-cent % is ignored.
	 *
	 * @param src a String representing an IPv6 address in textual format
	 * @return a byte array representing the IPv6 numeric address
	 */
	private byte[] textToNumericFormatV6(String src) {
		// Shortest valid string is "::", hence at least 2 chars
		if (src.length() < 2) {
			return null;
		}

		int colonp;
		char ch;
		boolean saw_xdigit;
		int val;
		char[] srcb = src.toCharArray();
		byte[] dst = new byte[INADDR16SZ];

		int srcb_length = srcb.length;
		int pc = src.indexOf ('%');
		if (pc == srcb_length -1) {
			return null;
		}

		if (pc != -1) {
			srcb_length = pc;
		}

		colonp = -1;
		int i = 0, j = 0;
		/* Leading :: requires some special handling. */
		if (srcb[i] == ':')
			if (srcb[++i] != ':')
				return null;
		int curtok = i;
		saw_xdigit = false;
		val = 0;
		while (i < srcb_length) {
			ch = srcb[i++];
			int chval = Character.digit(ch, 16);
			if (chval != -1) {
				val <<= 4;
				val |= chval;
				if (val > 0xffff)
					return null;
				saw_xdigit = true;
				continue;
			}
			if (ch == ':') {
				curtok = i;
				if (!saw_xdigit) {
					if (colonp != -1)
						return null;
					colonp = j;
					continue;
				} else if (i == srcb_length) {
					return null;
				}
				if (j + INT16SZ > INADDR16SZ)
					return null;
				dst[j++] = (byte) ((val >> 8) & 0xff);
				dst[j++] = (byte) (val & 0xff);
				saw_xdigit = false;
				val = 0;
				continue;
			}
			if (ch == '.' && ((j + INADDR4SZ) <= INADDR16SZ)) {
				String ia4 = src.substring(curtok, srcb_length);
				/* check this IPv4 address has 3 dots, ie. A.B.C.D */
				int dot_count = 0, index=0;
				while ((index = ia4.indexOf ('.', index)) != -1) {
					dot_count ++;
					index ++;
				}
				if (dot_count != 3) {
					return null;
				}
				byte[] v4addr = textToNumericFormatV4(ia4);
				if (v4addr == null) {
					return null;
				}
				for (int k = 0; k < INADDR4SZ; k++) {
					dst[j++] = v4addr[k];
				}
				saw_xdigit = false;
				break;  /* '\0' was seen by inet_pton4(). */
			}
			return null;
		}
		if (saw_xdigit) {
			if (j + INT16SZ > INADDR16SZ)
				return null;
			dst[j++] = (byte) ((val >> 8) & 0xff);
			dst[j++] = (byte) (val & 0xff);
		}

		if (colonp != -1) {
			int n = j - colonp;

			if (j == INADDR16SZ)
				return null;
			for (i = 1; i <= n; i++) {
				dst[INADDR16SZ - i] = dst[colonp + n - i];
				dst[colonp + n - i] = 0;
			}
			j = INADDR16SZ;
		}
		if (j != INADDR16SZ)
			return null;
		m_ip = dst;
		convertFromIPv4MappedAddress();
		return m_ip;
	}

	///**
	// * @param src a String representing an IPv4 address in textual format
	// * @return a boolean indicating whether src is an IPv4 literal address
	// */
	//private static boolean isIPv4LiteralAddress(String src) {
	//	return textToNumericFormatV4(src) != null;
	//}

	///**
	// * @param src a String representing an IPv6 address in textual format
	// * @return a boolean indicating whether src is an IPv6 literal address
	// */
	//private boolean isIPv6LiteralAddress(String src) {
	//	return textToNumericFormatV6(src) != null;
	//}

	/*
	 * Convert IPv4-Mapped address to IPv4 address. Both input and
	 * returned value are in network order binary form.
	 *
	 * @param src a String representing an IPv4-Mapped address in textual format
	 * @return a byte array representing the IPv4 numeric address
	 */
	private void convertFromIPv4MappedAddress() {
		if (isIPv4MappedAddress(m_ip)) {
			m_mapped = true;
			byte[] newAddr = new byte[INADDR4SZ];
			System.arraycopy(m_ip, 12, newAddr, 0, INADDR4SZ);
			m_ip = newAddr;
		} else {
			m_mapped = false;
		}
	}

	/**
	 * Utility routine to check if the InetAddress is an
	 * IPv4 mapped IPv6 address.
	 *
	 * @return a <code>boolean</code> indicating if the InetAddress is
	 * an IPv4 mapped IPv6 address; or false if address is IPv4 address.
	 */
	private static boolean isIPv4MappedAddress(byte[] addr) {
		if (addr.length < INADDR16SZ) {
			return false;
		}
		if ((addr[0] == 0x00) && (addr[1] == 0x00) &&
			(addr[2] == 0x00) && (addr[3] == 0x00) &&
			(addr[4] == 0x00) && (addr[5] == 0x00) &&
			(addr[6] == 0x00) && (addr[7] == 0x00) &&
			(addr[8] == 0x00) && (addr[9] == 0x00) &&
			(addr[10] == (byte)0xff) &&
			(addr[11] == (byte)0xff))  {
			return true;
		}
		return false;
	}

	////////////////////////////////////////////////////////////////////////////
	// java.net.Inet4Address
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Converts IPv4 binary address into a string suitable for presentation.
	 *
	 * @param src a byte array representing an IPv4 numeric address
	 * @return a String representing the IPv4 address in
	 *		 textual representation format
	 */
	private static String numericToTextFormatV4(byte[] src)	{
		return (src[0] & 0xff) + "." + (src[1] & 0xff) + "." + (src[2] & 0xff) + "." + (src[3] & 0xff);
	}

	////////////////////////////////////////////////////////////////////////////
	// java.net.Inet6Address
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Convert IPv6 binary address into presentation (printable) format.
	 *
	 * @param src a byte array representing the IPv6 numeric address
	 * @return a String representing an IPv6 address in
	 *		 textual representation format
	 */
	private static String numericToTextFormatV6(byte[] src) {
		StringBuilder sb = new StringBuilder(39);
		for (int i = 0; i < (INADDR16SZ / INT16SZ); i++) {
			sb.append(Integer.toHexString(((src[i<<1]<<8) & 0xff00) | (src[(i<<1)+1] & 0xff)));
			if (i < (INADDR16SZ / INT16SZ) -1 ) {
			   sb.append(":");
			}
		}
		return sb.toString();
	}

}
