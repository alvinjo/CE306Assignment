package Assignment1;

import edu.stanford.nlp.process.Morphology;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.StringTokenizer;


public class WebSearch {

    private ArrayList<Integer> urlList = new ArrayList<>();
    private String url;
    //tfMap will act as the term frequency list
    private HashMap<String, Integer> tfMap = new HashMap<>();
    //path points to the directory containing the tagger file for pos tagging
    private String path = System.getProperty("user.dir") + "\\src\\Assignment1";

    public static void main(String[] args) {
        WebSearch web = new WebSearch();
    }


    /**
     * Constructor for the class. Allows the user to input a site to be indexed.
     * Runs the processing pipeline in a loop to allow multiple sites to be indexed.
     */
    public WebSearch(){
        Scanner input = new Scanner(System.in);
        while(true){
            System.out.println("Enter URL: ");
            url = input.nextLine();
            urlList.add(urlToName(url));
            run();
        }
    }


    /**
     * This method makes calls to each step in the processing pipeline and writes the output
     * to terminal and a text file
     */
    private void run(){
        //HTML input
        String docText = "";
        try{
            Document doc = Jsoup.connect(url).get();
            docText = getText(doc);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Tokenize
        String tokenizedText = tokenize(docText);
        writeToLog(tokenizedText, "tokenized");
        System.out.println(tokenizedText);

        //POS and selecting keywords
        String posTagged = posTagging(tokenizedText);
        writeToLog(posTagged, "tagged");
        System.out.println(posTagged);

        String niceTags = grabTagged(posTagged);
        System.out.println(niceTags);

        niceTags = removeTags(niceTags);
        writeToLog(niceTags, "selected");
        System.out.println(niceTags);

        //Remove stopwords
        String cleanText = removeStopWords(niceTags);
        writeToLog(cleanText, "stopwordremoval");
        System.out.println(cleanText);

        //Stemming
        String stemmedText = stemming(cleanText);
        writeToLog(stemmedText, "stemmed");
        System.out.println(stemmedText);

        //Create TF index
        calculateTF(stemmedText);
        System.out.println(tfMap.entrySet());

        //HTML output
        writeTF();
        writeInvertedIndex();
    }


    /**
     * Grabs the plaintext of the document including the title and the meta tags.
     *
     * @param doc The website being scraped.
     * @return the plaintext of the document converted to lowercase.
     */
    private String getText(Document doc){
        String title = doc.title();
        String text = doc.body().text();

        StringBuilder metaTags = new StringBuilder();
        Elements metaTagElements = doc.getElementsByTag("meta");
        for (Element e: metaTagElements) {
            metaTags.append(e.attr("content")).append(" ");
        }
        String finalText = title + " " + text + " " + metaTags;
        return finalText.toLowerCase();
    }


    /**
     * Splits the plaintext up into tokens using the specified delimiter. If the token is one character long it checks
     * whether it is a valid word, otherwise it appends the token to a stringbuilder.
     *
     * @param docText the plaintext to be tokenized.
     * @return a single string containing the tokenized words of the plaintext.
     */
    private String tokenize(String docText){
        StringTokenizer tokenizer = new StringTokenizer(docText, " \t\n\r\f\",.:;?![]'/");
        StringBuilder sb = new StringBuilder();

        while(tokenizer.hasMoreTokens()){
            String currentToken = tokenizer.nextToken();
            currentToken = currentToken.replace("(", "").replace(")", "");

            if(currentToken.length() == 1){
                if(checkASCII(currentToken.charAt(0))){
                    sb.append(currentToken).append(" ");
                }
            }else{
                sb.append(currentToken).append(" ");
            }
        }
        return sb.toString();
    }


    /**
     * Used to remove special characters and only keep numbers and valid words.
     *
     * @param c character to check.
     * @return true if the character is 'a', 'i' or a number.
     */
    private boolean checkASCII(char c) {
        return (c == 97 || c == 105 || c>= 48 && c<=57);
    }


    /**
     * Tags each term using the tagger file.
     *
     * @param text the text to be tagged.
     * @return a string of words, each tagged with their pos identifiers.
     */
    private String posTagging(String text){
        MaxentTagger tagger = new MaxentTagger( path + "\\english-left3words-distsim.tagger");
        return tagger.tagString(text);
    }


    /**
     * Grabs certain POS tagged words based on their tags. The tags being kept correspond to nouns, conjunctions,
     * numbers, adverbs and verbs.
     *
     * @param text the tagged text from which valuable words will be taken from.
     * @return a string of tagged valuable words.
     */
    private String grabTagged(String text){
        String[] textArray = text.split(" ");
        StringBuilder sb = new StringBuilder();

        for (String word : textArray) {
            String tag = word.substring(word.lastIndexOf("_")+1, word.length());
            if(tag.equals("NN") || tag.equals("JJ") || tag.equals("CD") || tag.equals("RB") || tag.equals("VBZ")){
                sb.append(word).append(" ");
            }
        }
        return sb.toString();
    }


    /**
     * Removes the tags from tagged words.
     *
     * @param text text that has gone through POS tagging.
     * @return a string of words with their tags removed.
     */
    private static String removeTags(String text){
        String[] textArray = text.split(" ");
        StringBuilder sb = new StringBuilder();

        for (String word : textArray) {
            sb.append(word.substring(0,word.lastIndexOf("_"))).append(" ");
        }
        return sb.toString();
    }


    /**
     * Loops through a stoplist and checks if each word of the input text is included in the stoplist.
     * If a word matches a term in the stoplist, the word is replaced with an empty value.
     * Each word is then added to a stringbuilder and converted to a single string.
     *
     * @param text the text where stop words must be removed from.
     * @return a single string with all stopwords removed.
     */
    private String removeStopWords(String text){
        String[] textArray = text.split(" ");

        try {
            BufferedReader br = new BufferedReader(new FileReader(path + "\\stoplist.txt"));
            String currentStopWord;

            while((currentStopWord = br.readLine()) != null){
                for (int i = 0; i < textArray.length; i++) {
                    if(textArray[i].equals(currentStopWord)){
                        textArray[i] = "";
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        StringBuilder sb = new StringBuilder();
        for (String st: textArray) {
            if(!st.equals("")){
                sb.append(st).append(" ");
            }
        }
        return sb.toString();
    }


    /**
     *  If possible, will reduce inflected words to their word stem.
     *
     * @param text the text to be simplified.
     * @return a single string that may include stemmed words.
     */
    private static String stemming(String text){
        String[] textArray = text.split(" ");
        StringBuilder sb = new StringBuilder();
        Morphology morph = new Morphology();

        for(String word : textArray){
            sb.append(morph.stem(word)).append(" ");
        }
        return sb.toString();
    }


    /**
     * Creates a term frequency list using a hash map. If a term does not exist in the map, it is added and if a term
     * already exists then its value is incremented.
     *
     * @param text the final string containing unique terms which will be used to create the TF list
     */
    private void calculateTF(String text){
        String[] textArray = text.split(" ");
        for (String word : textArray){
            if(tfMap.containsKey(word)){
                tfMap.put(word, tfMap.get(word)+1);
            }else{
                tfMap.put(word, 1);
            }
        }
    }


    /**
     * Outputs the TF list in the terminal in a readable format.
     * Also makes a call to the writeToLog method to write the TF list to a text file.
     */
    private void writeTF(){
        StringBuilder sb = new StringBuilder();
        for (String word:tfMap.keySet()) {
            sb.append(String.format("%15s%5d%15s", word, tfMap.get(word), urlToName(url)));
            sb.append("\r\n");
        }
        writeToLog(sb.toString(), "TF");
    }


    /**
     * This method creates an index file using the first TF created.
     * It then loops through the index checking its terms with the terms in the other TF file.
     * If a match is found, the document ID is appended to the end of the line.
     *
     * @return true if successful.
     */
    private boolean writeInvertedIndex(){
        try {
            File file = new File("index.txt");
            if (!file.exists()){
                Files.copy(new File(urlList.get(0)+"TF.txt").toPath(), file.toPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        if(urlList.size() == 1){return false;}
        try {
            BufferedReader reader1 = new BufferedReader(new FileReader("index.txt"));
            BufferedReader reader2 = new BufferedReader(new FileReader(urlList.get(1) + "TF.txt"));
            Writer writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream("index1.txt", true), "UTF-8"));
            String checkLine = reader2.readLine();
            String currentLine = reader1.readLine();

            while(currentLine != null && checkLine != null){
                String term = currentLine.substring(0, currentLine.indexOf(" "));
                if (term.equals(checkLine.substring(0, checkLine.indexOf(" ")))){
                    writer.append(currentLine).append(" ").append(urlList.get(1)+"").append("\r\n");
                }else{
                    writer.append(currentLine).append("\r\n");
                }
                currentLine = reader1.readLine();
                checkLine = reader2.readLine();
            }


            if(currentLine==null && checkLine !=null){
                while(checkLine!=null){
                    writer.append(checkLine).append("\r\n");
                    checkLine = reader2.readLine();
                }
            }else if (currentLine!=null && checkLine == null){
                while(currentLine!=null){
                    writer.append(currentLine).append("\r\n");
                    currentLine = reader1.readLine();
                }
            }

            writer.close();
            reader1.close();
            reader2.close();

            File old = new File("index.txt");
            old.delete();
            File newFile = new File("index1.txt");
            newFile.renameTo(old);

        } catch (IOException e) {
            e.printStackTrace();
        }

        urlList.remove(1);
        return true;
    }


    /**
     * Writes the inputted text to a text file with a specified identifier string appended to the name.
     *
     * @param text the text being written.
     * @param log the tag for the file.
     */
    private void writeToLog(String text, String log){
        try {
            PrintWriter writer = new PrintWriter( urlToName(url) + log + ".txt", "UTF-8");
            writer.write(text);
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }


    /**
     * Creates a document ID for a URL by tokenizing the URL then adding up the ascii values of each character.
     *
     * @param url the URL to be converted.
     * @return the docID
     */
    private int urlToName(String url){
        String sName = tokenize(url);
        int nName = 0;
        for (int i = 0; i < sName.length(); i++) {
            nName += sName.charAt(i);
        }
        return nName;
    }
}