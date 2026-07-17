package io.github.txtchapterdetector.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure offline TXT chapter detector; no Android or network dependency. */
public final class ChapterDetector {
    private static final Pattern NUMBERED = Pattern.compile("^(\\d{1,4})(.*)$");
    private static final Pattern EXPLICIT = Pattern.compile(
            "^第[0-9一二三四五六七八九十百千万零〇两]{1,8}[章节回卷部篇].{0,40}$");
    private static final Pattern METADATA = Pattern.compile(
            "(?:打赏|咸鱼|天前|https?://|www\\.|收藏|评论|作者|字数|更新时间)");
    private static final Pattern QUESTION = Pattern.compile("^\\d{1,4}[：:].*");
    private static final Pattern SENTENCE_END = Pattern.compile("[。！？!?；;]$");

    private ChapterDetector() { }

    public static List<Chapter> detect(String text) {
        if (text == null || text.isEmpty()) return Collections.emptyList();
        List<Line> lines = splitLines(text);
        List<Chapter> result = new ArrayList<>();
        List<NumberCandidate> numbered = new ArrayList<>();
        for (Line line : lines) {
            String value = line.value.trim();
            if (value.isEmpty() || value.length() > 48) continue;
            if (isExplicit(value)) {
                result.add(new Chapter(line.offset, line.number, value));
            } else {
                NumberCandidate candidate = numberCandidate(line, value);
                if (candidate != null) numbered.add(candidate);
            }
        }
        result.addAll(selectNumberedSequence(numbered));
        result.sort(Comparator.comparingInt(chapter -> chapter.offset));
        return deduplicate(result);
    }

    private static boolean isExplicit(String value) {
        if (EXPLICIT.matcher(value).matches() && value.length() <= 32
                && value.indexOf('，') < 0 && value.indexOf('。') < 0) return true;
        if (value.equals("序章") || value.equals("楔子") || value.equals("引子")
                || value.equals("后记") || value.equals("完结感言")) return true;
        return value.matches("^番外(?:篇)?[：:].{1,40}$");
    }

    private static NumberCandidate numberCandidate(Line line, String value) {
        Matcher matcher = NUMBERED.matcher(value);
        if (!matcher.matches() || QUESTION.matcher(value).matches() || METADATA.matcher(value).find()) return null;
        int number;
        try { number = Integer.parseInt(matcher.group(1)); }
        catch (NumberFormatException ignored) { return null; }
        String tail = matcher.group(2).trim();
        if (number < 1 || number > 999 || tail.length() > 28 || SENTENCE_END.matcher(value).find()) return null;
        if (!tail.isEmpty() && !tail.matches("^[\\p{IsHan}A-Za-z（）()【】\\[\\]《》·.\\-—、！!？?]+$")) return null;
        return new NumberCandidate(line, value, number, tail);
    }

    private static List<Chapter> selectNumberedSequence(List<NumberCandidate> candidates) {
        List<Chapter> result = new ArrayList<>();
        int expected = 1;
        int last = 0;
        for (NumberCandidate candidate : candidates) {
            boolean next = candidate.number == expected;
            boolean split = candidate.number == last && isPart(candidate.tail);
            if (!next && !split) continue;
            result.add(new Chapter(candidate.line.offset, candidate.line.number, candidate.raw));
            if (next) { last = candidate.number; expected = candidate.number + 1; }
        }
        return result;
    }

    private static boolean isPart(String tail) {
        String value = tail.toLowerCase(Locale.ROOT);
        return value.contains("上") || value.contains("下") || value.contains("篇") || value.contains("part");
    }

    private static List<Chapter> deduplicate(List<Chapter> input) {
        List<Chapter> result = new ArrayList<>();
        int lastOffset = -1;
        for (Chapter chapter : input) {
            if (chapter.offset != lastOffset) result.add(chapter);
            lastOffset = chapter.offset;
        }
        return result;
    }

    private static List<Line> splitLines(String text) {
        List<Line> result = new ArrayList<>();
        int start = 0;
        int number = 1;
        for (int i = 0; i <= text.length(); i++) {
            if (i == text.length() || text.charAt(i) == '\n') {
                int end = i;
                if (end > start && text.charAt(end - 1) == '\r') end--;
                result.add(new Line(start, number++, text.substring(start, end)));
                start = i + 1;
            }
        }
        return result;
    }

    private static final class Line {
        final int offset; final int number; final String value;
        Line(int offset, int number, String value) { this.offset = offset; this.number = number; this.value = value; }
    }

    private static final class NumberCandidate {
        final Line line; final String raw; final int number; final String tail;
        NumberCandidate(Line line, String raw, int number, String tail) {
            this.line = line; this.raw = raw; this.number = number; this.tail = tail;
        }
    }

    public static final class Chapter {
        public final int offset; public final int line; public final String title;
        Chapter(int offset, int line, String title) { this.offset = offset; this.line = line; this.title = title; }
    }
}
