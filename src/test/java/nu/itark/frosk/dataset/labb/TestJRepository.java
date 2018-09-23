package nu.itark.frosk.dataset.labb;

import java.util.logging.Logger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestJRepository {
	Logger logger = Logger.getLogger(TestJRepository.class.getName());

	@Autowired
	PostRepository postRepository;

	@Autowired
	TagRepository tagRepository;

	@Test
	public final void testSaveDataSet() {

		//Kolla https://www.baeldung.com/hibernate-many-to-many
		
		
		// Cleanup the tables
		postRepository.deleteAllInBatch();
		tagRepository.deleteAllInBatch();

		// =======================================

		// Create a Post
		Post post = new Post("Hibernate Many to Many Example with Spring Boot",
				"Learn how to map a many to many relationship using hibernate", "Entire Post content with Sample code");

		// Create two tags
		Tag tag1 = new Tag("Spring Boot");
		Tag tag2 = new Tag("Hibernate");

		// Add tag references in the post
		post.getTags().add(tag1);
		post.getTags().add(tag2);

		// Add post reference in the tags
		tag1.getPosts().add(post);
		tag2.getPosts().add(post);

		postRepository.save(post);
		
		postRepository.findByTitle("Hibernate Many to Many Example with Spring Boot");
		
		

	}

}
