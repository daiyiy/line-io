package com.github.wolray.line.io.test;

import com.github.wolray.line.io.CsvWriter;
import com.github.wolray.line.io.LineCache;
import com.github.wolray.line.io.LineReader;
import lombok.ToString;
import org.junit.Test;

import java.util.List;

/**
 * @author wolray
 */
public class IoTest {
    @Test
    public void test() {
        LineReader.Excel<Person> excel = LineReader.byExcel(Person.class);
        List<Person> seq = excel
            .read(getClass(), "/line.xlsx")
            .columns("姓名", "体重", "年龄")
            .skipLines(1)
//            .stream()
//            .cacheCsv("src/test/resources/cache", Person.class)
//            .toList();
            .toSeq()
            .cacheBy(LineCache.byCsv("src/test/resources/cache.csv", Person.class))
            .toList();
        System.out.println(seq);
        CsvWriter.of(",", Person.class)
            .write("src/test/resources/line.csv")
            .autoHeader()
            .with(seq);
    }

    @ToString
    public static class Person {
        public String name;
        public Double weight;
        public Integer age;
    }
}