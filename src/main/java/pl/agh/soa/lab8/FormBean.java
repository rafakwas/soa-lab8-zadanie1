package pl.agh.soa.lab8;

import org.hibernate.HibernateException;
import org.hibernate.Session;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Stateless;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;


@Stateless
@RequestScoped
@Named("formbean")
public class FormBean implements Serializable {

    private final static Logger LOGGER = Logger.getLogger(FormBean.class.toString());

    private EntityManagerFactory emf;
    private EntityManager em;


    public FormBean() {
    }

    private Integer id;
    private String title;
    private Integer year;
    private String author;
    private String isbn;
    private Integer price;
    private boolean modified;

    @PostConstruct
    public void init() {
        emf = Persistence.createEntityManagerFactory( "hibernate-search-example" );
        em = emf.createEntityManager();
        LOGGER.info(()-> "Initialization completed");

    }

    @PreDestroy
    public void destroy() {
        em.close();
        emf.close();
        LOGGER.info(()-> "Destruction completed");
    }

    public List<Book> getBooks() {
        String hql = "from Book";
        Query query = em.createQuery(hql);
        return query.getResultList();
    }

    public void delete(Integer id) {
        Session session = em.unwrap(Session.class);
        String hql = "delete " + Book.class.getName() + " where id = :id";
        org.hibernate.Query q = (org.hibernate.Query)session.createQuery(hql).setParameter("id",id);
        q.executeUpdate();
    }

    public void renderModification(Integer id) {
        modified = true;
        this.id = id;
        Session session = em.unwrap(Session.class);
        Book book = (Book)session.get(Book.class,1);
    }

    public void modify(Integer id) {
        Session session = em.unwrap(Session.class);
        Book book = (Book)session.get(Book.class,id);
        LOGGER.info(""+book);

        book.setAuthor(author);
        book.setTitle(title);
        book.setYear(year);
        book.setPrice(price);
        book.setIsbn(isbn);

        try {
            session.beginTransaction();
            session.update(book);
            session.getTransaction().commit();
        } catch (HibernateException e) {
            e.printStackTrace();
            session.getTransaction().rollback();
        }
        book = (Book)session.get(Book.class,id);
        LOGGER.info(""+book);
        modified = false;
        this.id = 0;
    }


    public void submit() {
        Book book = new Book();
        book.setAuthor(author);
        book.setIsbn(isbn);
        book.setPrice(price);
        book.setTitle(title);
        book.setYear(year);
        em.getTransaction().begin();
        if(!em.contains(book)) {
            em.persist(book);
            em.flush();
        }
        em.getTransaction().commit();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

}
