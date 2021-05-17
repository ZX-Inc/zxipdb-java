package org.zxinc.ip;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class IpRange {
	private IpAddress ipStart;
	private IpAddress ipEnd;

	public IpRange(String ip) {
		int slash = ip.indexOf('/');
		if (slash != -1) {
			String ipstart = ip.substring(0, slash).strip();
			int prefixSize = Integer.parseInt(ip.substring(slash + 1));
			ipStart = new IpAddress(ipstart);
			if (ipStart.wasIPv4MappedAddress()) {
				prefixSize -= 96;
				if (prefixSize < 0) prefixSize = 0;
			}
			int maxPrefixSize = ipStart.getBytes().length * 8;
			BigInteger b_ipstart = new BigInteger(1, ipStart.getBytes());
			BigInteger b_hostmask = BigInteger.ONE.shiftLeft(maxPrefixSize).subtract(BigInteger.ONE)
				.shiftRight(prefixSize);
			b_ipstart = b_ipstart.andNot(b_hostmask);
			BigInteger b_ipend = b_ipstart.add(b_hostmask);
			byte[] bb_ipstart = toBytes(b_ipstart.toByteArray(), maxPrefixSize / 8);
			byte[] bb_ipend = toBytes(b_ipend.toByteArray(), maxPrefixSize / 8);
			ipStart = IpAddress.fromBytes(bb_ipstart);
			ipEnd = IpAddress.fromBytes(bb_ipend);
		} else {
			int dash = ip.indexOf('-');
			if (dash != -1) {
				String ipstart = ip.substring(0, dash).strip();
				String ipend = ip.substring(dash + 1).strip();
				ipStart = new IpAddress(ipstart);
				ipEnd = new IpAddress(ipend);
			} else {
				ipStart = ipEnd = new IpAddress(ip);
			}
		}
		checkValid();
	}

	public IpRange(String strStart, String strEnd) {
		ipStart = new IpAddress(strStart);
		ipEnd = new IpAddress(strEnd);
		checkValid();
	}

	public IpRange(IpAddress start, IpAddress end) {
		ipStart = start;
		ipEnd = end;
		checkValid();
	}

	private void checkValid() {
		if (ipStart.isValid() && ipEnd.isValid() && ipStart.isSameFamily(ipEnd)) {
			//OK
		} else {
			throw new IllegalArgumentException("输入IP范围非法");
		}
	}

	public boolean contains(String ip) {
		return contains(new IpAddress(ip));
	}

	public boolean contains(IpAddress ip) {
		if (!ip.isValid()) return false;
		if (!ip.isSameFamily(ipStart)) return false;
		if (ip.compareTo(ipStart) * ip.compareTo(ipEnd) <= 0)
			return true;
		else
			return false;
	}

	public BigInteger size() {
		return size(-1);
	}

	public BigInteger size(int prefixSize) {
		int maxPrefixSize = ipStart.getBytes().length * 8;
		if (prefixSize < 0) prefixSize = maxPrefixSize;
		if (prefixSize > maxPrefixSize)
			throw new IllegalArgumentException("输入前缀长度非法");
		BigInteger bistart = new BigInteger(1, ipStart.getBytes());
		BigInteger biend = new BigInteger(1, ipEnd.getBytes());
		BigInteger nums = biend.subtract(bistart).add(BigInteger.ONE);
		nums = nums.divide(BigInteger.TWO.pow((maxPrefixSize - prefixSize)));
		return nums;
	}

	@Override
	public String toString() {
		return ipStart + " - " + ipEnd;
	}

	private byte[] toBytes(byte[] array, int targetSize) {
		int counter = 0;
		List<Byte> newArr = new ArrayList<>();
		while (counter < targetSize && (array.length - 1 - counter >= 0)) {
			newArr.add(0, array[array.length - 1 - counter]);
			counter++;
		}
		int size = newArr.size();
		for (int i = 0; i < (targetSize - size); i++) {
			newArr.add(0, (byte) 0);
		}
		byte[] ret = new byte[newArr.size()];
		for (int i = 0; i < newArr.size(); i++) {
			ret[i] = newArr.get(i);
		}
		return ret;
	}

	public IpAddress getIpStart() {
		return ipStart;
	}

	public IpAddress getIpEnd() {
		return ipEnd;
	}

}
