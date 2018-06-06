package nu.itark.frosk.marknadssok.fi.proxy;

import static org.junit.Assert.*;

import java.util.List;

import org.assertj.core.util.DateUtil;
import org.junit.Before;
import org.junit.Test;

public class TestJMarknadsSokProxy {

	MarknadsSokProxy proxy = null;
	
	@Before
	public void setUp() throws Exception {
		proxy = new MarknadsSokProxy();
		
	}
	
	@Test
	public void testGet() {
		
		List<InsynsTransaktion> list = proxy.get(DateUtil.yesterday(), DateUtil.now());
		assertNotNull(list);
		
	}
	
	@Test
	public void testDownloadFile() {
		
		proxy.downloadFile(DateUtil.yesterday(), DateUtil.now());
		
	}	
	

}
