package org.zxinc.ip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Ipdbv4 {
	private RandomAccessFile file;
	private int total;
	private long index_start_offset;
	//private long index_end_offset;
	private int offlen;
	private int iplen;

	public Ipdbv4(String dbfile) throws IOException {
		init(dbfile);
	}

	private void init(String dbfile) throws IOException {
		Path path = Path.of(dbfile);
		if (Files.notExists(path)) {
			throw new IOException(dbfile + " does not exist, or is not readable");
		}
		file = new RandomAccessFile(dbfile, "r");
		index_start_offset = read8(16);
		offlen = read1(6);
		iplen = read1(7);
		total = (int)read8(8);
		//index_end_offset = index_start_offset + (iplen + offlen) * total;
	}

	public IpRecord query(String strIp) throws IOException {
		IpAddress ip = new IpAddress(strIp);
		return query(ip);
	}

	public IpRecord query(IpAddress ip) throws IOException {
		if (!ip.isValid()) {
			throw new IllegalArgumentException("错误或不完整的IP地址");
		} else if (ip.isIpv4()) {
			int ip_find = find(ip, 0, total);
			long ip_offset = index_start_offset + ip_find * (iplen + offlen);
			long ip_offset2 = ip_offset + iplen + offlen;
			byte[] b_ip_start = readRaw(ip_offset, iplen);
			IpAddress ip_start = IpAddress.fromBytesV4LE(b_ip_start);
			IpAddress ip_end;
			try {
				byte[] b_ip_end = readRaw(ip_offset2, iplen);
				ip_end = IpAddress.fromBytesV4LE(b_ip_end);
				ip_end.subOne();
			} catch (IOException e) {
				ip_end = new IpAddress("255.255.255.255");
			}
			long ip_record_offset = read8(ip_offset + iplen, offlen);
			String[] ip_addr = readRecord(ip_record_offset);
			String ip_addr_disp = ip_addr[0] + " " + ip_addr[1];

			IpRecord rec = new IpRecord();
			rec.ipAddress = ip;
			rec.ipRange = new IpRange(ip_start, ip_end);
			rec.country = ip_addr[0];
			rec.local = ip_addr[1];
			rec.display = ip_addr_disp;
			return rec;
		} else {
			throw new IllegalArgumentException("不支持的IP地址类型");
		}
	}

	public String getVersion() throws IOException {
		return query("255.255.255.255").display;
	}

	private int find(IpAddress ip, int L, int R) throws IOException {
		if (L + 1 >= R) {
			return L;
		}
		int M = (L + R) / 2;
		byte[] mip = readRaw(index_start_offset + M * (iplen + offlen), iplen);
		IpAddress aip = IpAddress.fromBytesV4LE(mip);
		if (ip.compareTo(aip) < 0) {
			return find(ip, L, M);
		} else {
			return find(ip, M, R);
		}

	}

	private String[] readRecord(long offset) throws IOException {
		String[] record = new String[2];
		byte[] rec0, rec1;
		int flag = read1(offset);
		if (flag == 1) {
			long location_offset = read8(offset + 1, offlen);
			return readRecord(location_offset);
		} else {
			rec0 = readLocation(offset);
			if (flag == 2) {
				rec1 = readLocation(offset + offlen + 1);
			} else {
				rec1 = readLocation(offset + rec0.length + 1);
			}
		}
		record[0] = new String(rec0, StandardCharsets.UTF_8);
		record[1] = new String(rec1, StandardCharsets.UTF_8);
		return record;
	}

	private byte[] readLocation(long offset) throws IOException {
		if (offset == 0) {
			return new byte[0];
		}
		int flag = read1(offset);
		// 出错
		if (flag == 0) {
			return new byte[0];
		}
		// 仍然为重定向
		if (flag == 2) {
			offset = read8(offset + 1, offlen);
			return readLocation(offset);
		}
		byte[] location = readStr(offset);
		return location;
	}

	private byte[] readRaw(byte[] b, long offset, int size) throws IOException {
		if (offset >= 0) {
			file.seek(offset);
		}
		file.read(b, 0, size);
		return b;
	}

	private byte[] readRaw(long offset, int size) throws IOException {
		byte[] b = new byte[size];
		return readRaw(b, offset, size);
	}

	private int read1(long offset) throws IOException {
		if (offset >= 0) {
			file.seek(offset);
		}
		return file.read();
	}

	//private long read4(long offset) throws IOException {
	//	byte[] b = readRaw(offset, 4);
	//	ByteBuffer buffer = ByteBuffer.wrap(b);
	//	buffer.order(ByteOrder.LITTLE_ENDIAN);
	//	return buffer.getInt() & 0xFFFFFFFFL;
	//}

	private long read8(long offset) throws IOException {
		return read8(offset, 8);
	}

	private long read8(long offset, int size) throws IOException {
		byte[] b = new byte[8];
		readRaw(b, offset, size);
		ByteBuffer buffer = ByteBuffer.wrap(b);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		return buffer.getLong();
	}

	private byte[] readStr(long offset) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int ch = read1(offset);
		while (ch != 0) {
			baos.write(ch);
			offset++;
			ch = read1(offset);
		}
		return baos.toByteArray();
	}

}
