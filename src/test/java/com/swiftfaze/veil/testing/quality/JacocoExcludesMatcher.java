package com.swiftfaze.veil.testing.quality;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class JacocoExcludesMatcher {
    private final List<PathMatcher> matchers = new ArrayList<>();
    private boolean valid = false;

    JacocoExcludesMatcher(Path projectRoot) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        dbf.setXIncludeAware(false);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(projectRoot.resolve("pom.xml").toFile());

        NodeList executions = doc.getElementsByTagName("execution");
        for (int i = 0; i < executions.getLength(); i++) {
            Element exec = (Element) executions.item(i);
            NodeList ids = exec.getElementsByTagName("id");
            if (ids.getLength() > 0 && "jacoco-check".equals(ids.item(0).getTextContent().trim())) {
                NodeList configs = exec.getElementsByTagName("configuration");
                if (configs.getLength() > 0) {
                    NodeList excludesLists = ((Element) configs.item(0)).getElementsByTagName("excludes");
                    if (excludesLists.getLength() > 0) {
                        NodeList excludes = ((Element) excludesLists.item(0)).getElementsByTagName("exclude");
                        for (int e = 0; e < excludes.getLength(); e++) {
                            String glob = excludes.item(e).getTextContent().trim();
                            if (!glob.isEmpty()) {
                                matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + glob));
                            }
                        }
                        valid = true;
                        return;
                    }
                }
            }
        }
    }

    boolean isValid() {
        return valid;
    }

    boolean matches(String classFilePath) {
        Path path = FileSystems.getDefault().getPath(classFilePath);
        for (PathMatcher matcher : matchers) {
            if (matcher.matches(path)) return true;
        }
        return false;
    }
}
