package pl.agh.soa.lab8;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.util.Version;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example testcase for Hibernate Search
 */
public class IndexAndSearchTest {

	private EntityManagerFactory emf;

	private EntityManager em;

	private static Logger log = LoggerFactory.getLogger( IndexAndSearchTest.class );

	@Before
	public void setUp() {
		initHibernate();
	}

//	@After
//	public void tearDown() {
//		purge();
//	}

	@Test
	public void testIfBooksInitialized() throws Exception {
		String hql = "from Book";
		Query query = em.createQuery(hql);
		List<Book> books = query.getResultList();
		for (Book book : books) {
			log.info(""+book);
		}
	}

	@Test
	public void testPersist() throws Exception {
		Book book = new Book();
		book.setAuthor("Howęda Śródlesńy");
		book.setIsbn("54-4-3-4-5");
		book.setPrice(111);
		book.setTitle("Baronowa");
		book.setYear(2000);
		em.getTransaction().begin();
		if(!em.contains(book)) {
			em.persist(book);
			em.flush();
		}
		em.getTransaction().commit();
	}

	@Test
	public void testModification() throws Exception {
		Session session = em.unwrap(Session.class);
		Book book = (Book)session.get(Book.class,1);
		System.out.println("book = " + book);

		book.setTitle("Pan Tadeusz 2");

		try {
			session.beginTransaction();
			session.update(book);
			session.getTransaction().commit();
		} catch (HibernateException e) {
			e.printStackTrace();
			session.getTransaction().rollback();
		}
		book = (Book)session.get(Book.class,1);
		System.out.println("updated book = " + book);
	}

	@Test
	public void testRemove() throws Exception {
		Session session = em.unwrap(Session.class);
		String hql = "delete " + Book.class.getName() + " where id = :id";
		org.hibernate.Query q = (org.hibernate.Query)session.createQuery(hql).setParameter("id",3);
		q.executeUpdate();
	}

	private void initHibernate() {
		emf = Persistence.createEntityManagerFactory( "hibernate-search-example" );
		em = emf.createEntityManager();
	}

	private void index() {
		FullTextEntityManager ftEm = org.hibernate.search.jpa.Search.getFullTextEntityManager( em );
		try {
			ftEm.createIndexer().startAndWait();
		}
		catch ( InterruptedException e ) {
			log.error( "Was interrupted during indexing", e );
		}
	}

	private void purge() {
		FullTextEntityManager ftEm = org.hibernate.search.jpa.Search.getFullTextEntityManager( em );
		ftEm.purgeAll( Book.class );
		ftEm.flushToIndexes();
		ftEm.close();
		emf.close();
	}

	private List<Book> search(String searchQuery) throws ParseException {
		Query query = searchQuery( searchQuery );

		List<Book> books = query.getResultList();

		for ( Book b : books ) {
			log.info( "Title: " + b.getTitle() );
		}
		return books;
	}

	private Query searchQuery(String searchQuery) throws ParseException {

		String[] bookFields = { "title", "subtitle", "authors.name", "publicationDate" };

		//lucene part
		Map<String, Float> boostPerField = new HashMap<String, Float>( 4 );
		boostPerField.put( bookFields[0], (float) 4 );
		boostPerField.put( bookFields[1], (float) 3 );
		boostPerField.put( bookFields[2], (float) 4 );
		boostPerField.put( bookFields[3], (float) .5 );

		FullTextEntityManager ftEm = org.hibernate.search.jpa.Search.getFullTextEntityManager( em );
		Analyzer customAnalyzer = ftEm.getSearchFactory().getAnalyzer( "customanalyzer" );
		QueryParser parser = new MultiFieldQueryParser(
				Version.LUCENE_34, bookFields,
				customAnalyzer, boostPerField
		);

		org.apache.lucene.search.Query luceneQuery;
		luceneQuery = parser.parse( searchQuery );

		final FullTextQuery query = ftEm.createFullTextQuery( luceneQuery, Book.class );

		return query;
	}
}
