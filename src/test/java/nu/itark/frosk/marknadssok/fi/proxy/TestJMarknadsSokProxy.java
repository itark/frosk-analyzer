package nu.itark.frosk.marknadssok.fi.proxy;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.assertj.core.util.DateUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
public class TestJMarknadsSokProxy {

	@Autowired
	MarknadsSokProxy proxy;	
	
	@Test
	public void testGet() {
		
		List<InsynsTransaktion> list = proxy.get(DateUtil.yesterday(), DateUtil.now());
		assertNotNull(list);
		
	}

	@Test
	public void testGetInsynshandel() {
		List<InsynsTransaktion> list;
		try {
			list= proxy.getInsynshandel(DateUtil.yesterday(), DateUtil.now());
			
			System.out.println("list.size"+list.size());
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	@Test
	public void testDownloadFile() {
		
		proxy.downloadFile(DateUtil.yesterday(), DateUtil.now());
		
	}	
	

}
