import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Reads a TU Wien course catalogue plus curricula with SAX and writes an XML
 * document with aggregate and statistical information.
 *
 * Usage: java CourseStats ex6.xml ex6-out.xml
 */
public final class CourseStats {

    // ---------------------------------------------------------------- model

    static final class Lecturer {
        final String id;
        String name = "";
        String email = "";
        Lecturer(String id) { this.id = id; }
    }

    static final class Assessment {
        final String type;
        final double weight;
        Assessment(String type, double weight) {
            this.type = type;
            this.weight = weight;
        }
    }

    static final class Course {
        final String number;
        final String language;
        String title = "";
        String kind = "";
        String deliveryMode = "";
        double weeklyHours;
        double ects;
        final List<Lecturer> lecturers = new ArrayList<>();
        final List<Assessment> assessments = new ArrayList<>();
        final Set<String> usedInCurricula = new LinkedHashSet<>();

        Course(String number, String language) {
            this.number = number;
            this.language = language;
        }
    }

    static final class Curriculum {
        final String studyCode;
        final String degree;
        String title = "";
        String durationSemesters = "";
        double totalEcts;
        int modules;
        double moduleEcts;
        int courseRefs;
        final Set<String> courseNumbers = new LinkedHashSet<>();

        Curriculum(String studyCode, String degree) {
            this.studyCode = studyCode;
            this.degree = degree;
        }
    }

    // ---------------------------------------------------------------- main

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: java CourseStats <input.xml> <output.xml>");
            System.exit(2);
        }

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(false);
        SAXParser parser = factory.newSAXParser();

        StatsHandler handler = new StatsHandler();
        parser.parse(new File(args[0]), handler);

        // link courses and curricula in both directions
        for (Curriculum c : handler.curricula.values()) {
            for (String number : c.courseNumbers) {
                Course course = handler.courses.get(number);
                if (course != null) {
                    course.usedInCurricula.add(c.studyCode);
                }
            }
        }

        String xml = handler.buildOutput();
        try (Writer out = new OutputStreamWriter(
                new FileOutputStream(args[1]), StandardCharsets.UTF_8)) {
            out.write(xml);
        }
        System.out.println("Wrote " + args[1]);
    }

    // -------------------------------------------------------------- handler

    static final class StatsHandler extends DefaultHandler {

        final Map<String, Course> courses = new LinkedHashMap<>();
        final Map<String, Curriculum> curricula = new LinkedHashMap<>();
        String semester = "";

        private final List<String> path = new ArrayList<>();
        private final StringBuilder text = new StringBuilder();
        private Course currentCourse;
        private Curriculum currentCurriculum;
        private Lecturer currentLecturer;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) {
            text.setLength(0);
            path.add(qName);

            switch (qName) {
                case "courseCatalogue" -> semester = attrs.getValue("semester");
                case "course" -> {
                    currentCourse = new Course(
                            attrs.getValue("number"), attrs.getValue("language"));
                    courses.put(currentCourse.number, currentCourse);
                }
                case "lecturer" -> {
                    currentLecturer = new Lecturer(attrs.getValue("id"));
                    if (currentCourse != null) {
                        currentCourse.lecturers.add(currentLecturer);
                    }
                }
                case "assessment" -> {
                    if (currentCourse != null) {
                        currentCourse.assessments.add(new Assessment(
                                attrs.getValue("type"),
                                parseDouble(attrs.getValue("weight"))));
                    }
                }
                case "curriculum" -> {
                    currentCurriculum = new Curriculum(
                            attrs.getValue("studyCode"), attrs.getValue("degree"));
                    curricula.put(currentCurriculum.studyCode, currentCurriculum);
                }
                case "courseRef" -> {
                    if (currentCurriculum != null) {
                        currentCurriculum.courseRefs++;
                        currentCurriculum.courseNumbers.add(attrs.getValue("number"));
                    }
                }
                default -> { /* title, ects, ... handled on endElement */ }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            text.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            String value = text.toString().trim();
            text.setLength(0);
            String parent = path.size() >= 2 ? path.get(path.size() - 2) : "";
            path.remove(path.size() - 1);

            if (parent.equals("course") && currentCourse != null) {
                switch (qName) {
                    case "title" -> currentCourse.title = value;
                    case "kind" -> currentCourse.kind = value;
                    case "weeklyHours" -> currentCourse.weeklyHours = parseDouble(value);
                    case "ects" -> currentCourse.ects = parseDouble(value);
                    case "deliveryMode" -> currentCourse.deliveryMode = value;
                    default -> { }
                }
            } else if (parent.equals("lecturer") && currentLecturer != null) {
                switch (qName) {
                    case "name" -> currentLecturer.name = value;
                    case "email" -> currentLecturer.email = value;
                    default -> { }
                }
            } else if (parent.equals("curriculum") && currentCurriculum != null) {
                switch (qName) {
                    case "title" -> currentCurriculum.title = value;
                    case "durationSemesters" -> currentCurriculum.durationSemesters = value;
                    case "totalEcts" -> currentCurriculum.totalEcts = parseDouble(value);
                    default -> { }
                }
            } else if (parent.equals("module") && currentCurriculum != null) {
                if (qName.equals("ects")) {
                    currentCurriculum.modules++;
                    currentCurriculum.moduleEcts += parseDouble(value);
                }
            }

            switch (qName) {
                case "course" -> currentCourse = null;
                case "lecturer" -> currentLecturer = null;
                case "curriculum" -> currentCurriculum = null;
                default -> { }
            }
        }

        // ------------------------------------------------------------ output

        String buildOutput() {
            double totalWeekly = 0;
            double totalEcts = 0;
            Map<String, double[]> byKind = new TreeMap<>();
            Map<String, Integer> byLanguage = new TreeMap<>();
            Map<String, Integer> byDeliveryMode = new TreeMap<>();
            Map<String, double[]> byLecturer = new TreeMap<>();
            Map<String, String> lecturerNames = new TreeMap<>();
            Map<String, double[]> byAssessment = new TreeMap<>();
            Map<String, Integer> coursesPerLecturer = new TreeMap<>();

            for (Course c : courses.values()) {
                totalWeekly += c.weeklyHours;
                totalEcts += c.ects;

                accumulate(byKind, c.kind, c.weeklyHours, c.ects);
                byLanguage.merge(c.language, 1, Integer::sum);
                byDeliveryMode.merge(c.deliveryMode, 1, Integer::sum);

                for (Lecturer l : c.lecturers) {
                    accumulate(byLecturer, l.id, 0, c.ects);
                    lecturerNames.putIfAbsent(l.id, l.name);
                    coursesPerLecturer.merge(l.id, 1, Integer::sum);
                }
                for (Assessment a : c.assessments) {
                    accumulate(byAssessment, a.type, 0, a.weight);
                }
            }

            int n = courses.size();
            StringBuilder b = new StringBuilder();
            b.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            b.append("<course-statistics semester=\"").append(esc(semester)).append("\">\n");

            // totals
            b.append("  <totals");
            attr(b, "courses", n);
            attr(b, "curricula", curricula.size());
            attr(b, "weeklyHours", fmt(totalWeekly));
            attr(b, "ects", fmt(totalEcts));
            attr(b, "averageEctsPerCourse", fmt(n == 0 ? 0 : totalEcts / n));
            attr(b, "averageWeeklyHoursPerCourse", fmt(n == 0 ? 0 : totalWeekly / n));
            b.append("/>\n");

            // courses by kind
            b.append("  <by-kind>\n");
            for (var e : byKind.entrySet()) {
                b.append("    <kind");
                attr(b, "name", e.getKey());
                attr(b, "courses", (int) e.getValue()[2]);
                attr(b, "weeklyHours", fmt(e.getValue()[0]));
                attr(b, "ects", fmt(e.getValue()[1]));
                b.append("/>\n");
            }
            b.append("  </by-kind>\n");

            // languages
            b.append("  <by-language>\n");
            for (var e : byLanguage.entrySet()) {
                b.append("    <language");
                attr(b, "code", e.getKey());
                attr(b, "courses", e.getValue());
                b.append("/>\n");
            }
            b.append("  </by-language>\n");

            // delivery modes
            b.append("  <by-delivery-mode>\n");
            for (var e : byDeliveryMode.entrySet()) {
                b.append("    <delivery-mode");
                attr(b, "name", e.getKey());
                attr(b, "courses", e.getValue());
                b.append("/>\n");
            }
            b.append("  </by-delivery-mode>\n");

            // lecturers
            b.append("  <lecturers>\n");
            for (var e : byLecturer.entrySet()) {
                b.append("    <lecturer");
                attr(b, "id", e.getKey());
                attr(b, "name", lecturerNames.get(e.getKey()));
                attr(b, "courses", coursesPerLecturer.get(e.getKey()));
                attr(b, "ects", fmt(e.getValue()[1]));
                b.append("/>\n");
            }
            b.append("  </lecturers>\n");

            // assessment types
            b.append("  <assessment-types>\n");
            for (var e : byAssessment.entrySet()) {
                b.append("    <assessment");
                attr(b, "type", e.getKey());
                attr(b, "count", (int) e.getValue()[2]);
                attr(b, "averageWeight", fmt(e.getValue()[1] / e.getValue()[2]));
                b.append("/>\n");
            }
            b.append("  </assessment-types>\n");

            // curricula
            b.append("  <curricula>\n");
            for (Curriculum c : curricula.values()) {
                b.append("    <curriculum");
                attr(b, "studyCode", c.studyCode);
                attr(b, "degree", c.degree);
                attr(b, "title", c.title);
                attr(b, "durationSemesters", c.durationSemesters);
                attr(b, "totalEcts", fmt(c.totalEcts));
                attr(b, "modules", c.modules);
                attr(b, "moduleEcts", fmt(c.moduleEcts));
                attr(b, "courseRefs", c.courseRefs);
                attr(b, "distinctCourses", c.courseNumbers.size());
                double coverage = c.totalEcts == 0 ? 0 : c.moduleEcts / c.totalEcts * 100;
                attr(b, "moduleEctsShare", fmt(coverage) + "%");
                b.append("/>\n");
            }
            b.append("  </curricula>\n");

            // per course view
            b.append("  <courses>\n");
            for (Course c : courses.values()) {
                double weightSum = c.assessments.stream().mapToDouble(a -> a.weight).sum();
                b.append("    <course");
                attr(b, "number", c.number);
                attr(b, "title", c.title);
                attr(b, "kind", c.kind);
                attr(b, "language", c.language);
                attr(b, "deliveryMode", c.deliveryMode);
                attr(b, "weeklyHours", fmt(c.weeklyHours));
                attr(b, "ects", fmt(c.ects));
                attr(b, "lecturers", c.lecturers.size());
                attr(b, "assessmentWeightSum", fmt(weightSum));
                attr(b, "curricula", c.usedInCurricula.size());
                b.append(">\n");
                for (String code : c.usedInCurricula) {
                    b.append("      <used-in studyCode=\"").append(esc(code)).append("\"/>\n");
                }
                b.append("    </course>\n");
            }
            b.append("  </courses>\n");

            b.append("</course-statistics>\n");
            return b.toString();
        }

        private static void accumulate(Map<String, double[]> map, String key,
                                       double a, double c) {
            double[] v = map.computeIfAbsent(key, k -> new double[3]);
            v[0] += a;
            v[1] += c;
            v[2] += 1; // count of occurrences
        }

        private static void attr(StringBuilder b, String name, Object value) {
            b.append(' ').append(name).append("=\"").append(esc(String.valueOf(value)))
             .append('"');
        }

        private static String esc(String s) {
            StringBuilder b = new StringBuilder(s.length() + 16);
            for (int i = 0; i < s.length(); i++) {
                char ch = s.charAt(i);
                switch (ch) {
                    case '&' -> b.append("&amp;");
                    case '<' -> b.append("&lt;");
                    case '>' -> b.append("&gt;");
                    case '"' -> b.append("&quot;");
                    default -> b.append(ch);
                }
            }
            return b.toString();
        }
    }

    static double parseDouble(String s) {
        if (s == null || s.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static String fmt(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
