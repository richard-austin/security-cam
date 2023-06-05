package common;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthorizationHeaderParser {


    /* ****************************************
     * OP Mechanism
     * **************************************** */

    private static final String SEPARATORS = "()<>@,;:\\\\\"/\\[\\]?={} \t";

    private static final Pattern TOKEN_PATTERN = Pattern
            .compile("[[\\p{ASCII}]&&[^" + SEPARATORS + "]&&[^\\p{Cntrl}]]+");
    private static final Pattern EQ_PATTERN = Pattern.compile("=");
    private static final Pattern TOKEN_QUOTED_PATTERN = Pattern
            .compile("\"([^\"]|\\\\\\p{ASCII})*\"");
    private static final Pattern COMMA_PATTERN = Pattern.compile(",");
    private static final Pattern LWS_PATTERN = Pattern
            .compile("(\r?\n)?[ \t]+");

    private static class Tokenizer {
        private String remaining;

        public Tokenizer(String input) {
            remaining = input;
        }

        private void skipSpaces() {
            Matcher m = LWS_PATTERN.matcher(remaining);
            if (!m.lookingAt()) {
                return;
            }
            String match = m.group();
            remaining = remaining.substring(match.length());
        }

        public String match(Pattern p) {
            skipSpaces();
            Matcher m = p.matcher(remaining);
            if (!m.lookingAt()) {
                return null;
            }
            String match = m.group();
            remaining = remaining.substring(match.length());
            return match;
        }

        public String mustMatch(Pattern p) {
            String match = match(p);
            if (match == null) {
                throw new NoSuchElementException();
            }
            return match;
        }

        public boolean hasMore() {
            skipSpaces();
            return remaining.length() > 0;
        }

    }

    public static Map<String, String> parse(String input) {
        Tokenizer t = new Tokenizer(input);
        Map<String, String> map = new HashMap<String, String>();

        String authScheme = t.match(TOKEN_PATTERN);
        map.put(":auth-scheme", authScheme);

        while (true) {
            while (t.match(COMMA_PATTERN) != null) {
                // Skip null list elements
            }

            if (!t.hasMore()) {
                break;
            }

            String key = t.mustMatch(TOKEN_PATTERN);
            t.mustMatch(EQ_PATTERN);
            String value = t.match(TOKEN_PATTERN);
            if (value == null) {
                value = t.mustMatch(TOKEN_QUOTED_PATTERN);
                // trim quotes
                value = value.substring(1, value.length() - 1);
            }

            map.put(key, value);

            if (t.hasMore()) {
                t.mustMatch(COMMA_PATTERN);
            }

        }
        return map;
    }

    /* ****************************************
     * State Machine Mechanism
     * **************************************** */

    private static enum ParseState{
        PROLOGSPACE,
        PROLOGWORD,
        KEY,
        KEYVALGAP,
        VALUE,
        QUOTEDVALUE,
        SEPARATOR,
        COMPLETE;
    }

    private static final String WHITESPACE = new String(" \t\r\n");

    public static Map<String,String> parseSM(String value) {
        Map<String,String> result = new HashMap<>();

        ParseState currentstate = ParseState.PROLOGSPACE;
        char[] valchars = value.toCharArray();
        // add a null character at the end.
        valchars = Arrays.copyOf(valchars, valchars.length + 1);
        int mark = 0;
        String key = null;
        for (int i = 0; i < valchars.length; i++) {
            final char ch = valchars[i];
            switch (currentstate) {
                case PROLOGSPACE: {
                    // we are in any whitespace before the 'Digest' :auth-scheme
                    if (WHITESPACE.indexOf(ch) < 0) {
                        // no longer in white-space, mark the spot, and move on.
                        mark = i;
                        currentstate = ParseState.PROLOGWORD;
                    }
                    break;
                }
                case PROLOGWORD: {
                    // we are in the 'Digest' :auth-scheme
                    if (WHITESPACE.indexOf(ch) >= 0) {
                        // no longer on the word, handle it....
                        result.put(":auth-scheme", new String(valchars, mark, i - mark));
                        currentstate = ParseState.SEPARATOR;
                    }
                    break;
                }
                case SEPARATOR: {
                    // processing the gap before/between key=value pairs.
                    if (ch == 0) {
                        currentstate = ParseState.COMPLETE;
                    } else if (ch != ',' && WHITESPACE.indexOf(ch) < 0) {
                        mark = i;
                        currentstate = ParseState.KEY;
                    }
                    break;
                }
                case KEY: {
                    // processing a key=value key.
                    if (ch == '=' /* || WHITESPACE.indexOf(ch) >= 0 */ ) {
                        // no longer in key
                        key = new String(valchars, mark, i-mark);
                        currentstate = ParseState.KEYVALGAP;
                    }
                    break;
                }
                case KEYVALGAP: {
                    if (ch != '=' /* && WHITESPACE.indexOf(ch) < 0 */) {
                        mark = 0;
                        if (ch == '"') {
                            currentstate = ParseState.QUOTEDVALUE;
                            mark = i + 1;
                        } else {
                            currentstate = ParseState.VALUE;
                            mark = i;
                        }
                    }
                    break;
                }
                case VALUE: {
                    if (ch == ',' || ch == 0 || WHITESPACE.indexOf(ch) >= 0) {
                        result.put(key, new String(valchars, mark, i - mark));
                        currentstate = ParseState.SEPARATOR;
                    }
                    break;
                }
                case QUOTEDVALUE: {
                    if (ch == '"') {
                        result.put(key, new String(valchars, mark, i - mark));
                        currentstate = ParseState.SEPARATOR;
                    }
                    break;
                }
                case COMPLETE: {
                    throw new IllegalStateException("There should be no characters after COMPLETE");
                }

            }
        }
        if (currentstate != ParseState.COMPLETE) {
            throw new IllegalStateException("Unexpected parse path ended before completion (ended at " + currentstate + ").");
        }
        return result;
    }

    /* ****************************************
     * Scanner Mechanism
     * **************************************** */

    private static final Pattern SCANWHITESPACE = Pattern.compile("\\s+");
    private static final Pattern SCANEQUALS = Pattern.compile("=");
    private static final Pattern SCANONECHAR = Pattern.compile("\\s*");
    private static final Pattern SCANCOMMA = Pattern.compile("\\s*,\\s*");
    private static final Pattern SCANQUOTEEND = Pattern.compile("\"");

    public static Map<String,String> parseScanner(String value) {
        Map<String,String> result = new HashMap<>();
        try (Scanner scanner = new Scanner(value)) {
            scanner.useDelimiter(SCANWHITESPACE);
            if (scanner.hasNext(SCANWHITESPACE)) {
                scanner.skip(SCANWHITESPACE);
            }
            result.put(":auth-scheme", scanner.next());
            while (scanner.hasNext()) {
                scanner.skip(scanner.delimiter());
                scanner.useDelimiter(SCANEQUALS);
                String key = scanner.next();
                scanner.skip(scanner.delimiter());
                scanner.useDelimiter(SCANONECHAR);
                if (scanner.hasNext()) {
                    String firstchar = scanner.next();
                    if ("\"".equals(firstchar)) {
                        scanner.useDelimiter(SCANQUOTEEND);
                        String val = scanner.next();
                        result.put(key, val);
                        scanner.skip(scanner.delimiter());
                        scanner.useDelimiter(SCANCOMMA);
                    } else {
                        scanner.useDelimiter(SCANCOMMA);
                        result.put(key, firstchar + scanner.next());
                    }
                }
            }
        }

        return result;
    }

    public static void main(String args[]) {
        String test1 = "Digest\n"
                + "                 realm=\"testrealm@host.com\",\n"
                + "                 qop=\"auth,auth-int\",\n"
                + "                 nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\",\n"
                + "                 opaque=\"5ccc069c403ebaf9f0171e9517f40e41\"";
        String test2 = "Digest username=\"Mufasa\",\n"
                + "                 realm=\"testrealm@host.com\",\n"
                + "                 nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\",\n"
                + "                 uri=\"/dir/index.html\",\n"
                + "                 qop=auth,\n"
                + "                 nc=00000001,\n"
                + "                 cnonce=\"0a4f113b\",\n"
                + "                 response=\"6629fae49393a05397450978507c4ef1\",\n"
                + "                 opaque=\"5ccc069c403ebaf9f0171e9517f40e41\"";

        System.out.println(parse(test1));
        System.out.println(parseSM(test1));
        System.out.println(parseScanner(test1));
        System.out.println(parse(test2));
        System.out.println(parseSM(test2));
        System.out.println(parseScanner(test2));
    }
}
