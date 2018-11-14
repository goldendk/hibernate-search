package org.goldenworkshop;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.hibernate.CacheMode;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;

public class AppMain {
    private static EntityManagerFactory emf;

    private static EntityManager em;

    private static Logger log = LoggerFactory.getLogger(IndexAndSearchTest.class);
    private static List<Book> books;

    public static void main(String... args) throws ParseException {
        System.setProperty("java.net.preferIPv4Stack", "true");
        emf = Persistence.createEntityManagerFactory("hibernate-search-example");
        em = emf.createEntityManager();

        // search by title
        books = search("hibernate");
//        assertEquals( "Should find one book", 1, books.size() );
//        assertEquals( "Wrong title", "Java Persistence with Hibernate", books.get( 0 ).getTitle() );

        Scanner scanner = new Scanner(System.in);
        System.out.println("Waiting for next input....");
        String scannerValue = null;
        while (true) {
            scannerValue = scanner.nextLine();
            try {

                if ("exit".equalsIgnoreCase(scannerValue)) {
                    break;
                }

                if (scannerValue.startsWith("book")) {
                    Book book = new Book();
                    EntityTransaction transaction = em.getTransaction();
                    transaction.begin();

                    book.setId((int) ((Math.random() * 10_000_000) + 300_000));
                    book.setTitle(scannerValue);

                    em.persist(book);
                    transaction.commit();
                } else if (scannerValue.startsWith("search")) {
                    String search_ = scannerValue.replace("search ", "");
                    System.out.println("Searching: " + search_);
                    search(search_);
                } else if (scannerValue.startsWith("reindex")) {
                    index();
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        em.close();
        emf.close();
    }

    private static void index() {
        FullTextEntityManager ftEm = org.hibernate.search.jpa.Search.getFullTextEntityManager(em);


        try {

            ftEm.createIndexer(Book.class)
                    .batchSizeToLoadObjects(1000)
                    .cacheMode(CacheMode.IGNORE)
                    .threadsToLoadObjects(2)
                    .idFetchSize(500)
                    .transactionTimeout(1800)
                    .startAndWait();
        } catch (InterruptedException e) {
            log.error("Was interrupted during indexing", e);
        }
    }

    private static List<Book> search(String searchQuery) throws ParseException {
        Query query = searchQuery(searchQuery);

        List<Book> books = query.getResultList();

        for (Book b : books) {
            log.info("Title: " + b.getTitle());
        }
        return books;
    }

    private static Query searchQuery(String searchQuery) throws ParseException {

        String[] bookFields = {"title", "subtitle", "authors.name"};

        //lucene part
        Map<String, Float> boostPerField = new HashMap<String, Float>(4);
        boostPerField.put(bookFields[0], (float) 4);
        boostPerField.put(bookFields[1], (float) 3);
        boostPerField.put(bookFields[2], (float) 4);

        FullTextEntityManager ftEm = org.hibernate.search.jpa.Search.getFullTextEntityManager(em);

        Analyzer customAnalyzer = ftEm.getSearchFactory().getAnalyzer("customanalyzer");

        QueryParser parser = new MultiFieldQueryParser(bookFields, customAnalyzer, boostPerField);

        org.apache.lucene.search.Query luceneQuery;
        luceneQuery = parser.parse(searchQuery);

        final FullTextQuery query = ftEm.createFullTextQuery(luceneQuery, Book.class);

        return query;
    }

}
