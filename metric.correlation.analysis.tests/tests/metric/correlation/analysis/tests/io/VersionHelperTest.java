package metric.correlation.analysis.tests.io;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import metric.correlation.analysis.io.VersionHelper;

public class VersionHelperTest {
	
	@Test
	public void testNumbers1() {
		String v1 = "1";
		String v2 = "2";
		assertEquals(1, VersionHelper.compare(v2, v1));
		assertEquals(-1, VersionHelper.compare(v1, v2));
	}
	
	@Test
	public void testNumbers2() {
		String v1 = "1.0";
		String v2 = "2.0";
		assertEquals(1, VersionHelper.compare(v2, v1));
		assertEquals(-1, VersionHelper.compare(v1, v2));
	}
	
	@Test
	public void testNumbers3() {
		String v1 = "1.0.1";
		String v2 = "1.0.2";
		assertEquals(1, VersionHelper.compare(v2, v1));
		assertEquals(-1, VersionHelper.compare(v1, v2));
	}
	
	@Test
	public void testMixed1() {
		String v1 = "v1";
		String v2 = "v2";
		assertEquals(1, VersionHelper.compare(v2, v1));
		assertEquals(-1, VersionHelper.compare(v1, v2));
	}
	
	@Test
	public void testMixed2() {
		String v1 = "v1.0abc";
		String v2 = "v1.1abc";
		assertEquals(1, VersionHelper.compare(v2, v1));
		assertEquals(-1, VersionHelper.compare(v1, v2));
	}
	
	@Test
	public void testText() {
		String v1 = "abc";
		String v2 = "def";
		assertEquals(0, VersionHelper.compare(v2, v1));
		assertEquals(0, VersionHelper.compare(v1, v2));
	}
}
