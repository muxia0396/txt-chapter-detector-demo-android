package io.github.txtchapterdetector.demo;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ChapterDetectorTest {
    @Test public void detectsSequentialHeadingsAndSplitParts() {
        String text = lines(
                "序章", "正文", "1Start", "正文", "2Second", "正文", "3", "正文",
                "4Part上", "正文", "4Part下", "正文", "番外：Later", "正文", "完结感言");

        List<ChapterDetector.Chapter> result = ChapterDetector.detect(text);

        assertEquals(8, result.size());
        assertEquals("序章", result.get(0).title);
        assertEquals("1Start", result.get(1).title);
        assertEquals("3", result.get(3).title);
        assertEquals("4Part上", result.get(4).title);
        assertEquals("4Part下", result.get(5).title);
        assertEquals("番外：Later", result.get(6).title);
        assertEquals("完结感言", result.get(7).title);
    }

    @Test public void rejectsCommonMetadataAndProseNoise() {
        String text = lines(
                "1Start", "正文", "2Second", "正文", "2天前 user 打赏了 5 咸鱼",
                "1：问卷", "https://example.test", "第二部分的叙述，仍然是正文。", "3Third", "正文");

        List<ChapterDetector.Chapter> result = ChapterDetector.detect(text);

        assertEquals(3, result.size());
        assertEquals("1Start", result.get(0).title);
        assertEquals("2Second", result.get(1).title);
        assertEquals("3Third", result.get(2).title);
    }

    private static String lines(String... values) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) text.append('\n');
            text.append(values[i]);
        }
        return text.toString();
    }
}
