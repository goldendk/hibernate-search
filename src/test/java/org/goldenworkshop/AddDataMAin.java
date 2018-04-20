package org.goldenworkshop;

import org.apache.lucene.util.IOUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class AddDataMAin {
    public static void main(String... args) throws IOException {


        FileWriter fileWriter = new FileWriter(new File("./src/test/resources/import.sql"), true);


        for (int i = 0 ; i< 200000; i ++){
            int id = 10_070 + i;
            String book = "INSERT INTO BOOK VALUES (" +( id ) + " , '2004-01-31', '', 'book " + i + "');";
            String auther = "INSERT INTO BOOK_AUTHOR VALUES ("+id+", 111);";

            fileWriter.write(book  + "\n");
        }

        fileWriter.close();


    }
}
