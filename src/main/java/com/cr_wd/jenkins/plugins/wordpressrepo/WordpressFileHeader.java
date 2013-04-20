package com.cr_wd.jenkins.plugins.wordpressrepo;

import hudson.FilePath;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordpressFileHeader {

    private Map<String, String> headers;

    private WordpressFileHeader(Map<String, String> headers) {
        this.headers = headers;
    }

    protected static Map<String, Pattern> getDefaultHeaders() {
        Map<String, Pattern> headers = new HashMap<String, Pattern>();
        headers.put("Description", makePattern("Description"));
        headers.put("Author", makePattern("Author"));
        headers.put("AuthorURI", makePattern("Author URI"));
        headers.put("Version", makePattern("Version"));
        headers.put("TextDomain", makePattern("Text Domain"));
        headers.put("DomainPath", makePattern("Domain Path"));
        return headers;
    }

    public String getName() {
        return getHeaders().get("Name");
    }

    public String getVersion() {
        return getHeaders().get("Version");
    }

    public String getDescription() {
        return getHeaders().get("Description");
    }

    public String getAuthor() {
        return getHeaders().get("Author");
    }

    public String getAuthorURI() {
        return getHeaders().get("AuthorURI");
    }

    public String getTextDomain() {
        return getHeaders().get("TextDomain");
    }

    public String getDomainPath() {
        return getHeaders().get("DomainPath");
    }


    protected Map<String, String> getHeaders() {
        return headers;
    }

    protected static Pattern makePattern(String name) {
        return Pattern.compile("^[ \\t\\/*#@]*" + Pattern.quote(name) + ":(.*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    }

    protected static String cleanHeader(String header) {
        return header.replaceAll("/\\s*(?:\\*\\/|\\?>).*/", "").trim();
    }

    public static class WordpressThemeFileHeader extends WordpressFileHeader {
        private WordpressThemeFileHeader(Map<String, String> headers) {
            super(headers);
        }

        protected static Map<String, Pattern> getDefaultHeaders() {
            Map<String, Pattern> headers = WordpressFileHeader.getDefaultHeaders();
            headers.put("Name", makePattern("Theme Name"));
            headers.put("URI", makePattern("Theme URI"));
            headers.put("Template", makePattern("Template"));
            headers.put("Status", makePattern("Status"));
            headers.put("Tags", makePattern("Tags"));
            return headers;
        }

        public String getStatus() {
            return getHeaders().get("status");
        }

        public String getTags() {
            return getHeaders().get("tags");
        }

        public String getTemplate() {
            return getHeaders().get("template");
        }

        public static WordpressThemeFileHeader parse(FilePath workspace) {
            try {
                byte[] header = new byte[8192];
                InputStream is = workspace.child("style.css").read();
                is.read(header);

                Map<String, String> headers = parse(header, getDefaultHeaders());
                if (headers.containsKey("Name")) {
                    return new WordpressThemeFileHeader(headers);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public static class WordpressPluginFileHeader extends WordpressFileHeader {
        private WordpressPluginFileHeader(Map<String, String> headers) {
            super(headers);
        }

        protected static Map<String, Pattern> getDefaultHeaders() {
            Map<String, Pattern> headers = WordpressFileHeader.getDefaultHeaders();
            headers.put("Name", makePattern("Plugin Name"));
            headers.put("URI", makePattern("Plugin URI"));
            headers.put("Network", makePattern("Network"));
            return headers;
        }

        public String getNetwork() {
            return getHeaders().get("Network");
        }

        public static WordpressPluginFileHeader parse(FilePath workspace) {
            try {
                List<FilePath> phpFiles = workspace.list(new ExtentionFileFilter(new String[]{".php"}));
                for (FilePath file : phpFiles) {
                    byte[] header = new byte[8192];
                    InputStream is = file.read();
                    is.read(header);

                    Map<String, String> headers = parse(header, getDefaultHeaders());
                    if (headers.containsKey("Name")) {
                        return new WordpressPluginFileHeader(headers);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    protected static Map<String, String> parse(byte[] headerBytes, Map<String, Pattern> patternMap) {
        final Map<String, String> headers = new HashMap<String, String>();
        final String header = new String(headerBytes);
        final Set<Entry<String, Pattern>> patternSet = patternMap.entrySet();
        for (Entry<String, Pattern> entry : patternSet) {
            final Matcher matcher = entry.getValue().matcher(header);
            if (matcher.find()) {
                headers.put(entry.getKey(), cleanHeader(matcher.group(1)));
            }
        }
        return headers;
    }

    protected static class ExtentionFileFilter implements FileFilter {

        private final String[] extentions;

        public ExtentionFileFilter(String[] extentions) {
            this.extentions = extentions;
        }

        public boolean accept(File file) {
            String name = file.getName().toLowerCase();
            for (String ext : extentions) {
                if (name.endsWith(ext)) {
                    return true;
                }
            }
            return false;
        }
    }

}
