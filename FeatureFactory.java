import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.*;

import org.json.JSONException;
import org.json.JSONObject;

public class FeatureFactory {

    // Common titles that often precede person names
    private Set<String> titleSet;

    // Common name prefixes and suffixes
    private Set<String> namePrefixes;

    // Common person name gazetteers
    private Set<String> commonFirstNames;
    private Set<String> commonLastNames;

    // Common reporting verbs that often follow person names
    private Set<String> reportingVerbs;

    // Common words that are unlikely to be person names
    private Set<String> nonNameWords;

    // Cache for previously seen words and their features
    private Map<String, Boolean> nameCache;

    // Resampling weights for balancing classes
    private double personWeight = 5.0; // Weight for PERSON class to boost recall

    /** Add any necessary initialization steps for your features here.
     *  Using this constructor is optional. Depending on your
     *  features, you may not need to intialize anything.
     */
    public FeatureFactory() {
        // Initialize title set - expanded with more titles
        titleSet = new HashSet<String>(Arrays.asList(
                "mr", "mrs", "ms", "miss", "dr", "prof", "professor", "sir", "madam",
                "president", "prime", "minister", "king", "queen", "prince", "princess",
                "senator", "governor", "mayor", "chief", "director", "secretary", "general",
                "colonel", "captain", "lieutenant", "officer", "judge", "justice", "attorney",
                "prosecutor", "ambassador", "chancellor", "commissioner", "superintendent",
                "coach", "manager", "commander", "sheikh", "imam", "rabbi", "father", "brother",
                "sister", "reverend", "pope", "cardinal", "bishop", "archbishop", "monsignor",
                "chairman", "chairwoman", "chairperson", "ceo", "cfo", "cto", "vp", "vice",
                "deputy", "assistant", "associate", "senior", "junior", "rep", "representative",
                "spokesman", "spokeswoman", "spokesperson"
        ));

        // Initialize name prefixes
        namePrefixes = new HashSet<String>(Arrays.asList(
                "van", "von", "der", "de", "la", "le", "du", "di", "da", "al", "el", "bin",
                "ibn", "mc", "mac", "o", "san", "st", "saint"
        ));

        // Initialize common first names based on dataset analysis
        commonFirstNames = new HashSet<String>(Arrays.asList(
                "john", "david", "michael", "paul", "mark", "peter", "robert", "thomas",
                "james", "william", "richard", "joseph", "charles", "george", "daniel",
                "matthew", "donald", "anthony", "steven", "andrew", "edward", "brian",
                "kevin", "ronald", "timothy", "jason", "jeffrey", "gary", "ryan", "jacob",
                "nicholas", "eric", "stephen", "jonathan", "larry", "justin", "scott",
                "brandon", "benjamin", "samuel", "gregory", "alexander", "patrick", "frank",
                "raymond", "jack", "dennis", "jerry", "tyler", "aaron", "jose", "adam",
                "henry", "douglas", "nathan", "peter", "zachary", "kyle", "walter", "harold",
                "jeremy", "ethan", "carl", "keith", "roger", "gerald", "christian", "terry",
                "sean", "arthur", "austin", "noah", "lawrence", "jesse", "joe", "bryan",
                "billy", "jordan", "albert", "dylan", "bruce", "willie", "gabriel", "alan",
                "juan", "logan", "wayne", "ralph", "roy", "eugene", "randy", "vincent",
                "russell", "louis", "philip", "bobby", "johnny", "bradley", "mary", "patricia",
                "jennifer", "linda", "elizabeth", "barbara", "susan", "jessica", "sarah",
                "karen", "nancy", "lisa", "margaret", "betty", "sandra", "ashley", "dorothy",
                "kimberly", "emily", "donna", "michelle", "carol", "amanda", "melissa",
                "deborah", "stephanie", "rebecca", "laura", "sharon", "cynthia", "kathleen",
                "amy", "shirley", "angela", "helen", "anna", "brenda", "pamela", "nicole",
                "ruth", "katherine", "samantha", "christine", "emma", "catherine", "debra",
                "virginia", "rachel", "carolyn", "janet", "maria", "heather", "diane", "julie",
                "joyce", "victoria", "kelly", "christina", "joan", "evelyn", "lauren", "judith",
                "olivia", "frances", "martha", "cheryl", "megan", "andrea", "hannah", "jacqueline",
                "ann", "jean", "alice", "kathryn", "gloria", "teresa", "doris", "sara", "janice",
                "julia", "marie", "madison", "grace", "judy", "abigail", "clinton", "yeltsin",
                "arafat", "lebed", "dutroux", "wasim", "akram", "ahmed", "lien", "tang", "shen",
                "costas", "simitis", "skandalidis", "dimitris", "colleen", "itamar", "rabinovich",
                "eliahu", "hafez", "assad", "levy", "boris", "bill", "hillary", "barack", "joe",
                "kamala", "donald", "mike", "george", "dick", "colin", "condoleezza", "john",
                "vladimir", "dmitry", "angela", "emmanuel", "nicolas", "francois", "jacques",
                "tony", "gordon", "theresa", "boris", "justin", "stephen", "paul", "jean",
                "shinzo", "narendra", "manmohan", "indira", "benjamin", "ariel", "ehud",
                "mahmoud", "yasser", "hosni", "muammar", "saddam", "bashar", "recep", "kim",
                "xi", "hu", "jiang", "deng", "mao", "fidel", "hugo", "nicolas", "evo",
                "luiz", "dilma", "michel", "jair", "alberto", "cristina", "nestor", "felipe",
                "sebastian", "michelle", "nelson", "thabo", "jacob", "cyril", "robert",
                "emmerson", "uhuru", "yoweri", "paul", "felix", "joseph", "julius", "john"
        ));

        // Initialize common last names based on dataset analysis
        commonLastNames = new HashSet<String>(Arrays.asList(
                "smith", "johnson", "williams", "jones", "brown", "davis", "miller", "wilson",
                "moore", "taylor", "anderson", "thomas", "jackson", "white", "harris", "martin",
                "thompson", "garcia", "martinez", "robinson", "clark", "rodriguez", "lewis",
                "lee", "walker", "hall", "allen", "young", "hernandez", "king", "wright",
                "lopez", "hill", "scott", "green", "adams", "baker", "gonzalez", "nelson",
                "carter", "mitchell", "perez", "roberts", "turner", "phillips", "campbell",
                "parker", "evans", "edwards", "collins", "stewart", "sanchez", "morris",
                "rogers", "reed", "cook", "morgan", "bell", "murphy", "bailey", "rivera",
                "cooper", "richardson", "cox", "howard", "ward", "torres", "peterson", "gray",
                "ramirez", "james", "watson", "brooks", "kelly", "sanders", "price", "bennett",
                "wood", "barnes", "ross", "henderson", "coleman", "jenkins", "perry", "powell",
                "long", "patterson", "hughes", "flores", "washington", "butler", "simmons",
                "foster", "gonzales", "bryant", "alexander", "russell", "griffin", "diaz",
                "hayes", "myers", "ford", "hamilton", "graham", "sullivan", "wallace", "woods",
                "cole", "west", "jordan", "owens", "reynolds", "fisher", "ellis", "harrison",
                "gibson", "mcdonald", "cruz", "marshall", "ortiz", "gomez", "murray", "freeman",
                "wells", "webb", "simpson", "stevens", "tucker", "porter", "hunter", "hicks",
                "crawford", "henry", "boyd", "mason", "morales", "kennedy", "warren", "dixon",
                "ramos", "reyes", "burns", "gordon", "shaw", "holmes", "rice", "robertson",
                "hunt", "black", "daniels", "palmer", "mills", "nichols", "grant", "knight",
                "ferguson", "rose", "stone", "hawkins", "dunn", "perkins", "hudson", "spencer",
                "gardner", "stephens", "payne", "pierce", "berry", "matthews", "arnold",
                "wagner", "willis", "ray", "watkins", "olson", "carroll", "duncan", "snyder",
                "hart", "cunningham", "bradley", "lane", "andrews", "ruiz", "harper", "fox",
                "riley", "armstrong", "carpenter", "weaver", "greene", "lawrence", "elliott",
                "chavez", "sims", "austin", "peters", "kelley", "franklin", "lawson", "blackburn",
                "zwingmann", "pas", "fischler", "palacio", "lloyd", "jones", "hendrix",
                "etchingham", "chan", "guofang", "shubei", "kontogiannis", "siegel", "ben-elissar",
                "clinton", "bush", "obama", "biden", "harris", "trump", "pence", "cheney",
                "powell", "rice", "kerry", "putin", "medvedev", "merkel", "macron", "sarkozy",
                "hollande", "chirac", "blair", "brown", "may", "johnson", "trudeau", "harper",
                "martin", "chretien", "abe", "modi", "singh", "gandhi", "netanyahu", "sharon",
                "barak", "abbas", "arafat", "mubarak", "gaddafi", "hussein", "assad", "erdogan",
                "jong-un", "jinping", "jintao", "zemin", "xiaoping", "zedong", "castro", "chavez",
                "maduro", "morales", "lula", "rousseff", "temer", "bolsonaro", "fernandez",
                "kirchner", "pinera", "bachelet", "mandela", "mbeki", "zuma", "ramaphosa", "mugabe",
                "mnangagwa", "kenyatta", "museveni", "kagame", "kabila", "nyerere", "magufuli",
                "blackburn"
        ));

        // Initialize reporting verbs that often follow person names
        reportingVerbs = new HashSet<String>(Arrays.asList(
                "said", "says", "told", "announced", "stated", "reported", "claimed", "added",
                "noted", "commented", "explained", "argued", "suggested", "mentioned", "indicated",
                "emphasized", "insisted", "declared", "acknowledged", "confirmed", "denied",
                "admitted", "asserted", "observed", "remarked", "replied", "responded", "testified",
                "wrote", "tweeted", "posted"
        ));

        // Initialize common words that are unlikely to be person names
        nonNameWords = new HashSet<String>(Arrays.asList(
                "the", "a", "an", "and", "or", "but", "if", "because", "as", "what", "when",
                "where", "how", "which", "who", "whom", "this", "that", "these", "those",
                "am", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had",
                "do", "does", "did", "will", "would", "shall", "should", "may", "might",
                "must", "can", "could", "to", "for", "from", "in", "on", "at", "by", "with",
                "about", "against", "between", "into", "through", "during", "before", "after",
                "above", "below", "under", "over", "again", "further", "then", "once", "here",
                "there", "all", "any", "both", "each", "few", "more", "most", "other", "some",
                "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very",
                "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
                "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
                "january", "february", "march", "april", "may", "june", "july", "august",
                "september", "october", "november", "december"
        ));

        // Initialize name cache
        nameCache = new HashMap<String, Boolean>();
    }

    /**
     * Words is a list of the words in the entire corpus, previousLabel is the label
     * for position-1 (or O if it's the start of a new sentence), and position
     * is the word you are adding features for. PreviousLabel must be the
     * only label that is visible to this method.
     */
    private List<String> computeFeatures(List<String> words,
                                         String previousLabel, int position) {

        List<String> features = new ArrayList<String>();

        String currentWord = words.get(position);

        // Baseline Features
        features.add("word=" + currentWord);
        features.add("prevLabel=" + previousLabel);
        features.add("word=" + currentWord + ", prevLabel=" + previousLabel);

        /** Warning: If you encounter "line search failure" error when
         *  running the program, considering putting the baseline features
         *  back. It occurs when the features are too sparse. Once you have
         *  added enough features, take out the features that you don't need.
         */

        // 1. Orthographic Features

        // 1.1 Capitalization features
        boolean isCapitalized = currentWord.length() > 0 && Character.isUpperCase(currentWord.charAt(0));
        features.add("isCapitalized=" + isCapitalized);

        boolean isAllCaps = currentWord.equals(currentWord.toUpperCase()) && currentWord.length() > 1;
        features.add("isAllCaps=" + isAllCaps);

        boolean isAllLower = currentWord.equals(currentWord.toLowerCase()) && currentWord.length() > 1;
        features.add("isAllLower=" + isAllLower);

        // 1.2 Word shape features
        String wordShape = wordShape(currentWord);
        features.add("wordShape=" + wordShape);

        String compressedWordShape = compressedWordShape(currentWord);
        features.add("compressedWordShape=" + compressedWordShape);

        // 1.3 Contains special characters
        boolean containsHyphen = currentWord.contains("-");
        features.add("containsHyphen=" + containsHyphen);

        boolean containsApostrophe = currentWord.contains("'");
        features.add("containsApostrophe=" + containsApostrophe);

        // 2. Contextual Features

        // 2.1 Window features (previous and next words)
        String prevWord = position > 0 ? words.get(position - 1) : "START";
        String prevWordLower = prevWord.toLowerCase();
        features.add("prevWord=" + prevWordLower);

        String nextWord = position < words.size() - 1 ? words.get(position + 1) : "END";
        String nextWordLower = nextWord.toLowerCase();
        features.add("nextWord=" + nextWordLower);

        // 2.2 Extended window features (2 words before and after)
        if (position > 1) {
            String prevPrevWord = words.get(position - 2).toLowerCase();
            features.add("prevPrevWord=" + prevPrevWord);
        }

        if (position < words.size() - 2) {
            String nextNextWord = words.get(position + 2).toLowerCase();
            features.add("nextNextWord=" + nextNextWord);
        }

        // 2.3 Bigram and trigram features
        if (position > 0) {
            features.add("prevBigram=" + prevWordLower + "_" + currentWord.toLowerCase());
        }

        if (position < words.size() - 1) {
            features.add("nextBigram=" + currentWord.toLowerCase() + "_" + nextWordLower);
        }

        if (position > 0 && position < words.size() - 1) {
            features.add("surrounding=" + prevWordLower + "_" + currentWord.toLowerCase() + "_" + nextWordLower);
        }

        // 2.4 Position features
        boolean isFirstWord = (position == 0);
        features.add("isFirstWord=" + isFirstWord);

        boolean isLastWord = (position == words.size() - 1);
        features.add("isLastWord=" + isLastWord);

        // 3. Lexical Features

        // 3.1 Prefix and suffix features
        if (currentWord.length() >= 1) {
            String prefix1 = currentWord.substring(0, 1);
            features.add("prefix1=" + prefix1);

            String suffix1 = currentWord.substring(currentWord.length() - 1);
            features.add("suffix1=" + suffix1);
        }

        if (currentWord.length() >= 2) {
            String prefix2 = currentWord.substring(0, Math.min(2, currentWord.length()));
            features.add("prefix2=" + prefix2);

            String suffix2 = currentWord.substring(Math.max(0, currentWord.length() - 2));
            features.add("suffix2=" + suffix2);
        }

        if (currentWord.length() >= 3) {
            String prefix3 = currentWord.substring(0, Math.min(3, currentWord.length()));
            features.add("prefix3=" + prefix3);

            String suffix3 = currentWord.substring(Math.max(0, currentWord.length() - 3));
            features.add("suffix3=" + suffix3);
        }

        // 3.2 Word length features
        int wordLength = currentWord.length();
        features.add("wordLength=" + wordLength);

        boolean isShortWord = (wordLength <= 2);
        features.add("isShortWord=" + isShortWord);

        boolean isLongWord = (wordLength >= 10);
        features.add("isLongWord=" + isLongWord);

        // 4. Gazetteer Features

        // 4.1 Title detection
        String lowerCaseWord = currentWord.toLowerCase();
        boolean isPrecededByTitle = false;

        if (position > 0) {
            isPrecededByTitle = titleSet.contains(prevWordLower);
            features.add("isPrecededByTitle=" + isPrecededByTitle);
        }

        // 4.2 Name prefix detection
        boolean isNamePrefix = namePrefixes.contains(lowerCaseWord);
        features.add("isNamePrefix=" + isNamePrefix);

        // 4.3 Common name detection
        boolean isCommonFirstName = commonFirstNames.contains(lowerCaseWord);
        features.add("isCommonFirstName=" + isCommonFirstName);

        boolean isCommonLastName = commonLastNames.contains(lowerCaseWord);
        features.add("isCommonLastName=" + isCommonLastName);

        // 4.4 Reporting verb detection
        boolean isFollowedByReportingVerb = false;
        if (position < words.size() - 1) {
            isFollowedByReportingVerb = reportingVerbs.contains(nextWordLower);
            features.add("isFollowedByReportingVerb=" + isFollowedByReportingVerb);
        }

        // 4.5 Non-name word detection (negative feature)
        boolean isNonNameWord = nonNameWords.contains(lowerCaseWord);
        features.add("isNonNameWord=" + isNonNameWord);

        // 5. Transition Features

        // 5.1 Previous label with current word features
        features.add("prevLabel_word=" + previousLabel + "_" + currentWord);

        // 5.2 Previous label with orthographic features
        features.add("prevLabel_isCapitalized=" + previousLabel + "_" + isCapitalized);

        // 5.3 Previous label with gazetteer features
        features.add("prevLabel_isCommonFirstName=" + previousLabel + "_" + isCommonFirstName);
        features.add("prevLabel_isCommonLastName=" + previousLabel + "_" + isCommonLastName);

        // 6. Compound Features

        // 6.1 Capitalized word not at the start of sentence
        boolean isCapitalizedNotAtStart = isCapitalized && position > 0;
        features.add("isCapitalizedNotAtStart=" + isCapitalizedNotAtStart);

        // 6.2 Current and next word are capitalized (likely part of a multi-token name)
        boolean currentAndNextCapitalized = false;
        if (position < words.size() - 1) {
            currentAndNextCapitalized = isCapitalized && nextWord.length() > 0 && Character.isUpperCase(nextWord.charAt(0));
            features.add("currentAndNextCapitalized=" + currentAndNextCapitalized);
        }

        // 6.3 Previous word was PERSON and current word is capitalized
        boolean prevPersonCurrentCap = previousLabel.equals("PERSON") && isCapitalized;
        features.add("prevPersonCurrentCap=" + prevPersonCurrentCap);

        // 6.4 Previous word is a title and current word is capitalized
        boolean titleFollowedByCap = isPrecededByTitle && isCapitalized;
        features.add("titleFollowedByCap=" + titleFollowedByCap);

        // 6.5 Current word is capitalized and followed by a reporting verb
        boolean capFollowedByReportingVerb = isCapitalized && isFollowedByReportingVerb;
        features.add("capFollowedByReportingVerb=" + capFollowedByReportingVerb);

        // 6.6 Previous word is capitalized and current word is capitalized
        boolean prevCapCurrentCap = false;
        if (position > 0) {
            prevCapCurrentCap = prevWord.length() > 0 && Character.isUpperCase(prevWord.charAt(0)) && isCapitalized;
            features.add("prevCapCurrentCap=" + prevCapCurrentCap);
        }

        // 7. Pattern-based Features

        // 7.1 Initials pattern (e.g., "J." or "J.K.")
        boolean isInitials = isInitialsPattern(currentWord);
        features.add("isInitials=" + isInitials);

        // 7.2 Camel case pattern (e.g., "McDowell")
        boolean isCamelCase = isCamelCasePattern(currentWord);
        features.add("isCamelCase=" + isCamelCase);

        // 7.3 Hyphenated name pattern (e.g., "Jean-Claude")
        boolean isHyphenatedName = isHyphenatedNamePattern(currentWord);
        features.add("isHyphenatedName=" + isHyphenatedName);

        // 8. Recall-boosting Features (Class Imbalance Handling)

        // 8.1 Any capitalized word (more aggressive approach)
        if (isCapitalized) {
            // Add multiple instances of this feature to increase its weight
            for (int i = 0; i < 3; i++) {
                features.add("anyCapitalized=true");
            }
        }

        // 8.2 Capitalized word with common name characteristics
        boolean likelyName = isCapitalized &&
                (isCommonFirstName || isCommonLastName ||
                        isPrecededByTitle || isFollowedByReportingVerb ||
                        prevCapCurrentCap || currentAndNextCapitalized ||
                        previousLabel.equals("PERSON"));

        // Add multiple instances of this feature to increase its weight
        if (likelyName) {
            for (int i = 0; i < 5; i++) {
                features.add("likelyName=true");
            }
        }

        // 8.3 Strong name indicators (very high confidence)
        boolean strongNameIndicator = titleFollowedByCap ||
                (previousLabel.equals("PERSON") && isCapitalized) ||
                (isCapitalized && isCommonFirstName) ||
                (isCapitalized && isFollowedByReportingVerb);

        // Add multiple instances of this feature to increase its weight
        if (strongNameIndicator) {
            for (int i = 0; i < 7; i++) {
                features.add("strongNameIndicator=true");
            }
        }

        // 8.4 Capitalized word in the middle of sentence (not at start)
        if (isCapitalizedNotAtStart) {
            // Add multiple instances of this feature to increase its weight
            for (int i = 0; i < 3; i++) {
                features.add("midSentenceCapitalized=true");
            }
        }

        // 8.5 Sequence of capitalized words
        if (prevCapCurrentCap) {
            // Add multiple instances of this feature to increase its weight
            for (int i = 0; i < 4; i++) {
                features.add("capitalizationSequence=true");
            }
        }

        // 8.6 Weighted feature for common names (to boost recall)
        if (isCommonFirstName || isCommonLastName) {
            // Add multiple instances of this feature to increase its weight
            for (int i = 0; i < 5; i++) {
                features.add("isInNameGazetteer=true");
            }
        }

        // 8.7 Context-based name likelihood
        boolean nameContext = isPrecededByTitle ||
                isFollowedByReportingVerb ||
                previousLabel.equals("PERSON");

        // Add multiple instances of this feature to increase its weight
        if (nameContext) {
            for (int i = 0; i < 4; i++) {
                features.add("nameContext=true");
            }
        }

        // 9. Resampling-inspired Features (to address class imbalance)

        // 9.1 Cache-based features (memory of previously seen names)
        String cacheKey = currentWord.toLowerCase();
        if (nameCache.containsKey(cacheKey)) {
            boolean isPreviouslyName = nameCache.get(cacheKey);
            if (isPreviouslyName) {
                // Add multiple instances of this feature to increase its weight
                for (int i = 0; i < 5; i++) {
                    features.add("previouslySeenName=true");
                }
            }
        }

        // Update cache if this is likely a name
        if (likelyName || strongNameIndicator || previousLabel.equals("PERSON")) {
            nameCache.put(cacheKey, true);
        }

        // 9.2 Focal loss inspired features (focus more on hard-to-classify examples)
        // If the word has characteristics that make it ambiguous, add more weight
        boolean isAmbiguous = isCapitalized && !isCommonFirstName && !isCommonLastName &&
                !isPrecededByTitle && !isFollowedByReportingVerb &&
                !previousLabel.equals("PERSON");

        if (isAmbiguous && isCapitalizedNotAtStart) {
            // Add multiple instances of this feature to increase its weight
            for (int i = 0; i < 4; i++) {
                features.add("ambiguousCapitalizedName=true");
            }
        }

        // 9.3 Data augmentation inspired features
        // Add variations of the current word to improve generalization
        if (isCapitalized && currentWord.length() > 2) {
            features.add("nameVariation=" + currentWord.substring(0, 2));
            if (currentWord.length() > 3) {
                features.add("nameVariation=" + currentWord.substring(0, 3));
            }
        }

        // 10. Ensemble-inspired Features (combining multiple signals)

        // 10.1 Weighted ensemble of name indicators
        int nameScore = 0;
        if (isCapitalized) nameScore += 1;
        if (isCapitalizedNotAtStart) nameScore += 2;
        if (isCommonFirstName) nameScore += 3;
        if (isCommonLastName) nameScore += 3;
        if (isPrecededByTitle) nameScore += 4;
        if (isFollowedByReportingVerb) nameScore += 3;
        if (previousLabel.equals("PERSON")) nameScore += 4;
        if (prevCapCurrentCap) nameScore += 2;
        if (currentAndNextCapitalized) nameScore += 2;
        if (isInitials) nameScore += 2;
        if (isCamelCase) nameScore += 2;
        if (isHyphenatedName) nameScore += 2;

        // Subtract score for negative indicators
        if (isNonNameWord) nameScore -= 3;
        if (isAllLower && !isNamePrefix) nameScore -= 2;
        if (isShortWord && !isInitials) nameScore -= 1;

        features.add("nameScore=" + nameScore);

        // Add strong bias for high scores to boost recall
        if (nameScore >= 3) {
            features.add("nameScoreHigh=true");
        }
        if (nameScore >= 5) {
            // Add multiple instances of this feature to increase its weight
            for (int i = 0; i < 3; i++) {
                features.add("nameScoreVeryHigh=true");
            }
        }
        if (nameScore >= 7) {
            // Add multiple instances of this feature to increase its weight
            for (int i = 0; i < 5; i++) {
                features.add("nameScoreExtreme=true");
            }
        }

        // 11. Special Case Features

        // 11.1 Special handling for single-token sentences (often headlines with names)
        if (words.size() == 1 && isCapitalized) {
            // Add multiple instances of this feature to increase its weight
            for (int i = 0; i < 3; i++) {
                features.add("singleTokenSentence=true");
            }
        }

        // 11.2 Special handling for words after punctuation (often start of new sentence)
        if (position > 0 && prevWord.equals(".") && isCapitalized) {
            features.add("afterPunctuation=true");
        }

        // 11.3 Special handling for words in quotes (often names)
        boolean inQuotes = false;
        if (position > 0 && (prevWord.equals("\"") || prevWord.equals("'"))) {
            inQuotes = true;
        }
        if (inQuotes && isCapitalized) {
            // Add multiple instances of this feature to increase its weight
            for (int i = 0; i < 3; i++) {
                features.add("quotedName=true");
            }
        }

        return features;
    }

    /**
     * Helper method to check if a string contains any digit
     */
    private boolean containsDigit(String str) {
        for (char c : str.toCharArray()) {
            if (Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper method to generate word shape
     * Maps uppercase letters to 'X', lowercase to 'x', digits to 'd', and retains other characters
     */
    private String wordShape(String word) {
        StringBuilder shape = new StringBuilder();
        for (char c : word.toCharArray()) {
            if (Character.isUpperCase(c)) {
                shape.append('X');
            } else if (Character.isLowerCase(c)) {
                shape.append('x');
            } else if (Character.isDigit(c)) {
                shape.append('d');
            } else {
                shape.append(c);
            }
        }
        return shape.toString();
    }

    /**
     * Helper method to generate compressed word shape
     * Similar to wordShape but compresses consecutive identical characters
     */
    private String compressedWordShape(String word) {
        String shape = wordShape(word);
        StringBuilder compressed = new StringBuilder();
        char prev = 0;

        for (char c : shape.toCharArray()) {
            if (c != prev) {
                compressed.append(c);
                prev = c;
            }
        }
        return compressed.toString();
    }

    /**
     * Helper method to check if a word matches the initials pattern
     */
    private boolean isInitialsPattern(String word) {
        return Pattern.matches("[A-Z](\\.[A-Z])*\\.*", word);
    }

    /**
     * Helper method to check if a word matches a camel case pattern typical in names
     */
    private boolean isCamelCasePattern(String word) {
        return Pattern.matches("[A-Z][a-z]+([A-Z][a-z]+)+", word) ||
                Pattern.matches("(Mc|Mac)[A-Z][a-z]+", word);
    }

    /**
     * Helper method to check if a word matches a hyphenated name pattern
     */
    private boolean isHyphenatedNamePattern(String word) {
        return Pattern.matches("[A-Z][a-z]+-[A-Z][a-z]+", word);
    }

    /** Do not modify this method **/
    public List<Datum> readData(String filename) throws IOException {

        List<Datum> data = new ArrayList<Datum>();
        BufferedReader in = new BufferedReader(new FileReader(filename));

        for (String line = in.readLine(); line != null; line = in.readLine()) {
            if (line.trim().length() == 0) {
                continue;
            }
            String[] bits = line.split("\\s+");
            String word = bits[0];
            String label = bits[1];

            Datum datum = new Datum(word, label);
            data.add(datum);
        }

        return data;
    }

    /** Do not modify this method **/
    public List<Datum> readTestData(String ch_aux) throws IOException {

        List<Datum> data = new ArrayList<Datum>();

        for (String line : ch_aux.split("\n")) {
            if (line.trim().length() == 0) {
                continue;
            }
            String[] bits = line.split("\\s+");
            String word = bits[0];
            String label = bits[1];

            Datum datum = new Datum(word, label);
            data.add(datum);
        }

        return data;
    }

    /** Do not modify this method **/
    public List<Datum> setFeaturesTrain(List<Datum> data) {
        // this is so that the feature factory code doesn't accidentally use the
        // true label info
        List<Datum> newData = new ArrayList<Datum>();
        List<String> words = new ArrayList<String>();

        for (Datum datum : data) {
            words.add(datum.word);
        }

        String previousLabel = "O";
        for (int i = 0; i < data.size(); i++) {
            Datum datum = data.get(i);

            Datum newDatum = new Datum(datum.word, datum.label);
            newDatum.features = computeFeatures(words, previousLabel, i);
            newDatum.previousLabel = previousLabel;
            newData.add(newDatum);

            previousLabel = datum.label;
        }

        return newData;
    }

    /** Do not modify this method **/
    public List<Datum> setFeaturesTest(List<Datum> data) {
        // this is so that the feature factory code doesn't accidentally use the
        // true label info
        List<Datum> newData = new ArrayList<Datum>();
        List<String> words = new ArrayList<String>();
        List<String> labels = new ArrayList<String>();
        Map<String, Integer> labelIndex = new HashMap<String, Integer>();

        for (Datum datum : data) {
            words.add(datum.word);
            if (labelIndex.containsKey(datum.label) == false) {
                labelIndex.put(datum.label, labels.size());
                labels.add(datum.label);
            }
        }

        // compute features for all possible previous labels in advance for
        // Viterbi algorithm
        for (int i = 0; i < data.size(); i++) {
            Datum datum = data.get(i);

            if (i == 0) {
                String previousLabel = "O";
                datum.features = computeFeatures(words, previousLabel, i);

                Datum newDatum = new Datum(datum.word, datum.label);
                newDatum.features = computeFeatures(words, previousLabel, i);
                newDatum.previousLabel = previousLabel;
                newData.add(newDatum);

            } else {
                for (String previousLabel : labels) {
                    datum.features = computeFeatures(words, previousLabel, i);

                    Datum newDatum = new Datum(datum.word, datum.label);
                    newDatum.features = computeFeatures(words, previousLabel, i);
                    newDatum.previousLabel = previousLabel;
                    newData.add(newDatum);
                }
            }

        }

        return newData;
    }

    /** Do not modify this method **/
    public void writeData(List<Datum> data, String filename)
            throws IOException {


        FileWriter file = new FileWriter(filename + ".json", false);


        for (int i = 0; i < data.size(); i++) {
            try {
                JSONObject obj = new JSONObject();
                Datum datum = data.get(i);
                obj.put("_label", datum.label);
                obj.put("_word", base64encode(datum.word));
                obj.put("_prevLabel", datum.previousLabel);

                JSONObject featureObj = new JSONObject();

                List<String> features = datum.features;
                for (int j = 0; j < features.size(); j++) {
                    String feature = features.get(j).toString();
                    featureObj.put("_" + feature, feature);
                }
                obj.put("_features", featureObj);
                obj.write(file);
                file.append("\n");
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        file.close();
    }

    /** Do not modify this method **/
    private String base64encode(String str) {
        Base64 base = new Base64();
        byte[] strBytes = str.getBytes();
        byte[] encBytes = base.encode(strBytes);
        String encoded = new String(encBytes);
        return encoded;
    }

}
