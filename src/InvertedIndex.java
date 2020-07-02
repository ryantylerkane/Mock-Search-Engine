import java.io.File; 
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;     
import java.util.LinkedList;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import static java.lang.Math.log10;
import java.util.Collections;
import java.util.List;

//Apache open source library for reading docx files.
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
//Porter's Stemming algorithm from the Lucene Snowball library.
import org.tartarus.snowball.ext.PorterStemmer;

public class InvertedIndex {
  
    /*The inverted index can be thought of as a hash table where the key is a term found in the corpus/query. The key links to a linkedlist that will hold document
    ID's and the locations of the key (term) in that document. Note that we choose to use LinkedLists to hold these pairs and to hold the list of locations. The reason for
    this is that we are only going to add to the list, and then check of all its values (its term frequency counts). We have no reason to randomly access items in the 
    list, providing we add to the beginning of the list each time. This means we will not have to traverse the list at all to update counts of terms.
    Since we are only adding nodes and viewing the entirety of the list, LinkedLists will allow us to implement a much quicker and dynamic memory space-efficient
    implementation. We will simply have to add nodes to the front of the list as opposed to deleting and resizing the array if an ArrayLit was used isntead.
    */
     private HashMap<String, LinkedList<Postings>> invertedIndex; 
     /*Create an array list that will hold the list of all file names so that they do not have to be added to the postings list.
     The index of a particular file name is the docID that it is assigned.*/
     public ArrayList<Document> docIDs; 
     private ArrayList<String> stopWords; //Array that will hold all of the stopwords. 
     
     public InvertedIndex() //Default class constructor - Creates an empty inverted index.
     {
         //Declare empty hash table and array for the inverted index.
         invertedIndex = new HashMap<String, LinkedList<Postings>>();
         docIDs = new ArrayList<Document>(); 
         stopWords = new ArrayList<String>();  
     }
     
     public void buildIndex(String directory) throws IOException //Function extracts each file from a corpus directory and then uses InvertedIndex member functions to add each document's contents to the inverted index.
    {
        loadStopWords(directory);
        /*Note that this function relies on the directory structure of the corpus not changing from what was provided in the zip. The function requires
        a directory with no files, only sub-directories. Those sub-directories must contain only files in order for this function to work.
        */
        File[] corpus = new File(directory).listFiles(); //Generates a list of all sub-directories and files in provided directory.
        int id=0; //Assign each document an ID to prevent storing the file name each time.
        for(int i=0; i < corpus.length;i++)
        {
                File[] subDirectory = corpus[i].listFiles(); //Obtain the files for that sub-directory.
                for(int j = 0; j < subDirectory.length; j++) //Add each file in a sub-directory to the index.
                {
                    String parent[] = subDirectory[j].getParent().split("\\\\");
                    docIDs.add(new Document(subDirectory[j].toString(), parent[parent.length-1])); //Assign a docID  and query type to the document.
                    addDoc(id, subDirectory[j]); //Place the documents terms in the arrayList.
                    id++;
                }
                
        }
    }
     
     public void getResults(String query, BufferedWriter out) throws IOException
     {
        try
        {
        
        ArrayList<String> terms = new ArrayList<String>();
        terms = processQuery(query); //List of significant terms from the query.
         ArrayList<Score> scores = scoreQuery(terms); //Process the query and obtain the scores for each document.
        
           /*Since many of the corus documents from different queries are based off of similar content (i.e. football and hockey), taking only the top
             n (where n = 20) documents may result in not returning some results that are still highly similar to the query. We must balance this with the notion
             that scores are generated for any value that contains even a single instance of any keyword. To achieve some sort of balance, we can include documents
             that are within a certain score distance from the 20th ranked document. This may not be the best approach for every query, but considering the similar
             content found across the corpus it is a worthy trade off.*/
           
        double threshold = scores.get(19).getScore() * 0.90; 
        
        ArrayList<Score> finalScores = new ArrayList<Score>();
        
        for(int h = 0; h < scores.size(); h++) //Remove scores that are below the threshold.
        {
            finalScores.add(scores.get(h));
            if(scores.get(h).getScore() < threshold) //Since the list is sorted, once we find a score lower than the threshold, all subsequent scores will be beneath it as well.
            {
                break;
            }  
        }
        
         
            out.append("QUERY: " + query);
            out.newLine();
             for (Score score: finalScores) //For each document that was scored...
             {
                 String[] fileName = docIDs.get(score.getDocID()).getFileName().split("\\\\"); //Split the directories from the string.
                 out.append("FILE NAME: " + fileName[fileName.length-1]); //Print out the file name only.
                 out.newLine();
                 //Print out document statistics.
                 out.append("TOTAL PARAGRAPH COUNT: " + docIDs.get(score.getDocID()).getParagraphCount());
                 out.newLine();
                 out.append("APPROXIMATE WORD COUNT: " + docIDs.get(score.getDocID()).getWordCount());
                 out.newLine();
                 //Print out snippet from document.
                 addSnippet(score, terms, out);
             }
             
             calculatePrecisionRecall(finalScores, terms,out);
        }
        catch(IOException e)
        {
            System.err.println(e);
            System.exit(1);
        }
     }
     
     private void calculatePrecisionRecall(ArrayList<Score> scores, ArrayList<String> terms, BufferedWriter out) throws IOException
     {
         //Search the keywords of the query to determine which documents are considered relevant.
         String queryType = "";
         if(terms.contains("lidstrom"))
         {
             queryType = "NikLidstrom";
         }
         else if(terms.contains("sec"))
         {
           queryType = "GeorgiaTech";          
         }
         else if(terms.contains("heisman"))
         {
             queryType = "Heisman";
         }
         else if(terms.contains("quebec"))
         {
             queryType = "QuebecNordiques";
         }
         else if(terms.contains("scout"))
         {
             queryType = "KCScouts";
         }
         else if(terms.contains("nhl"))
         {
             queryType = "NHL";
         }
         else if(terms.contains("nfl"))
         {
             queryType = "NFL";
         }
         else if(terms.contains("fb"))
         {
             queryType = "FBS";
         }
         else if(terms.contains("stanford"))
         {
             queryType="Stanford";
         }
         else if(terms.contains("iron"))
         {
             queryType = "IronBowl";
         }
         else //Indicate that a query will not have precision and recall calculated if it cannot be determined which documents in the corpus are relevant.
         {
             out.append("The provided query does not have underlying document stats collected.");
             out.newLine();
         }
         
         if(!queryType.equals(""))
         {
           int truePositives = 0;
           for(Score s: scores)
           {
               if(docIDs.get(s.getDocID()).getQuery().equals(queryType)) //Check if the document that was scored is from the group of documents related to the search.
                   truePositives++;
           }
           
           int totalRelevant = 0;
           for(Document d: docIDs)
           {
               if(d.getQuery().equals(queryType)) //Count the number of documents contained in the corpus that are matches to this query.
                   totalRelevant++;
           }
          //Calculate preciision and recall.
           double precision = (double)truePositives / (double) scores.size();
           double recall = (double) truePositives / (double) totalRelevant;
           
           out.append("PRECISION: " + String.format("%.2f",precision*100) + "%");
           out.newLine();
           out.append("RECALL: " + String.format("%.2f",recall*100) + "%");
           out.newLine();
           out.newLine();
         }
         else
         {
             out.append("Precision could not be calculated.");
             out.newLine();
             out.append("Recall could not be calculated.");
             out.newLine();
         }
     }
     private void addSnippet(Score s, ArrayList<String> queryTerms, BufferedWriter out)
     {
         ArrayList<Postings>sigPosts = new ArrayList<Postings>();
          for (String term : queryTerms)
         {
             for(int i=0; i < invertedIndex.get(term).size(); i++)
             {
                 if(invertedIndex.get(term).get(i).getDocId() == s.getDocID())
                 {
                     sigPosts.add(invertedIndex.get(term).get(i)); //Obtain all relevant postings for the document in question.
                     break;
                 }
                    
             }
         }
         
        //Attempt to locate paragraphs where the most relevant terms appear.  
        ArrayList<Integer> paragraphs = new ArrayList<Integer>();
        
        for(int j=0; j < sigPosts.size(); j++)
        {
            for(int k=0; k < sigPosts.get(j).getPara().size(); k++)
            {
                paragraphs.add(sigPosts.get(j).getPara().get(k));
            }
            
        }
        
        //Obtain the paragraph with the most relevant terms by sorting the ArrayList and finding the most abundant paragraph.
        Collections.sort(paragraphs);
        
        int [] otherRelevant = new int [docIDs.get(s.getDocID()).getParagraphCount()]; //Array that will hold any paragraph that has the same number of keywords as the paragraph with the greatest number of keywords.
        
        int maxCount = 1;
        int currentBest = paragraphs.get(0);
        int currentCount = 1;
        
        //Traverse the ArrayList once to determine which single paragraph contains the most keywords.
        for(int m = 1; m < paragraphs.size(); m++)
        {
            if(paragraphs.get(m) == paragraphs.get(m-1))
            {
                currentCount++;
            }
            else
            {
                if(currentCount > maxCount)
                {
                    maxCount = currentCount;
                    currentBest = paragraphs.get(m-1);
                }
                else
                {
                    if (currentCount==maxCount) //Catches the case where a paragraph has the same keyword count as the most relevant paragraph.
                    {
                        otherRelevant[paragraphs.get(m-1)] = currentCount;
                    }
                }
                currentCount=1;
            }
        }
        
        if(currentCount>maxCount)
        {
            maxCount = currentCount;
            currentBest =  paragraphs.get(paragraphs.size()-1);
        }
        else
        {
            if(currentCount==maxCount) //Store it in the highly relevant array so it can be returned.
                {
                    otherRelevant[paragraphs.get(paragraphs.size()-1)-1]= currentCount;
                }
        }
        
        if(otherRelevant.length > currentBest && otherRelevant[currentBest] != 0) //Alows us to remove the instance where paragraph #1 is featured in the snippet but was added to the other relevant paragraphs.
        {
            otherRelevant[currentBest] = 0;
        }
        
        //Find all words that appear in the paragraph with the most relevant terms.
        ArrayList<Integer> wordPos = new ArrayList<Integer>();
    
         for(int j=0; j < sigPosts.size(); j++)
         {
                for(int k=0; k < sigPosts.get(j).getPara().size(); k++)
                {
                    if(sigPosts.get(j).getPara().get(k)==currentBest);
                        wordPos.add(sigPosts.get(j).getLine().get(k));
                }

         }
         Collections.sort(wordPos); //Sort the words so we can obtain the range between the first relevant term and the last in the pargraph.
         
          try{
             FileInputStream fileIn = new FileInputStream(docIDs.get(s.getDocID()).getFileName());
             XWPFDocument snippet = new XWPFDocument(fileIn);
             List<XWPFParagraph> currentPar = snippet.getParagraphs();
             
             String words[] = currentPar.get(currentBest-1).getText().split(" ");
             out.write("SNIPPET: ");
             if(words.length <= 50) //If the entire paragraph is less than 50 words, simply print the paragraph as a preview.
             {
                out.append(currentPar.get(currentBest-1).getText() + " (PARAGRAPH #" + currentBest+")");
                out.newLine();
             }
             else //The length of the paragraph exceeds 50 words, we must decide where to stop printing the paragraph.
             {
                 int range = wordPos.get(wordPos.size()-1) - wordPos.get(0); //Number of words between the first and last relevant terms.
                 if(range >= 50) //Print from the first relevant word and then cite the paragraph so the user can find the rest.
                 {
                     for(int i = wordPos.get(0)-1; i < 50; i++)
                     {
                         out.write(words[i] + " ");
                     }
                     out.append("... (PARAGRAPH #" + currentBest+")");
                     out.newLine();
                 }
                 else //Range is less than fifty, so we can fit every significant term.
                 {
                     
                     boolean flag = false;
                     
                     for(int i = wordPos.get(0)-1; i < 50; i++)
                     {
                         out.write(words[i] + " ");
                         if(i > range && words[i].contains("[/./?!]")) //End of sentence found.
                         {
                             out.append(" (PARAGRAPH #" + currentBest+")");
                             out.newLine();
                             flag = true;
                         }
                     }
                     
                     if(!flag) //Check if end of sentence was reached and if not, print the paragraph location.
                     {
                         out.append(" (PARAGRAPH #" + currentBest+")");
                         out.newLine();
                     }
                 }
                 
             }
             if(otherRelevant.length > 0) //if there were paragrpahs with the same number of relevant terms found as the featured snippet...
             {
                 out.write("OTHER HIGHLY RELEVANT PARAGRAPHS: ");
                 for(int k = 0; k <otherRelevant.length; k++)
                 {
                     if(otherRelevant[k] ==maxCount)
                        out.write(k + "  ");
                 }
                     
                 out.newLine();
             }
             
             out.append("-----------");
             out.newLine();
             
          }
          catch(IOException e)
         {
             System.err.println(e.toString());
             System.exit(1);
         }
         
     }
     private ArrayList<Score> scoreQuery(ArrayList<String> queryTerms)
     {
         //ArrayList<String> queryTerms = new ArrayList<String>();
         //queryTerms = processQuery(query); //Obtain the significant terms from the query.
               
         ArrayList<Score> scores= new ArrayList<Score>(); //ArrayList that will hold all of the score for potential results.
         
         for(int i = 0; i < queryTerms.size(); i++) //For every significant query term...
         {
             if(invertedIndex.containsKey(queryTerms.get(i))) //Check if term was seen in the corpus.
             {
                 int postingsSize = invertedIndex.get(queryTerms.get(i)).size();
                 
                 for(int j=0; j < postingsSize; j++) //Read the entire postings list and generate their respective scores.
                {
                     int docID = invertedIndex.get(queryTerms.get(i)).get(j).getDocId();
                     double score = calculateTfIdf(invertedIndex.get(queryTerms.get(i)).get(j), postingsSize);
                     //Check to see if document has been considered in the score.
                     boolean found = false;
                     for(int k =0; k < scores.size(); k++)
                     {
                         if(docID == scores.get(k).getDocID())
                         {
                             found=true;
                             scores.get(k).updateScore(score);
                             break;
                         }
                     }
                     if(!found) //Document has not yet been considered in the score.
                     {
                        scores.add(new Score(docID, score));
                     }
                     
                }
             }
         }
         Collections.sort(scores);
         return scores; //Return the ArrayList containing the scores.
     }
     
     private ArrayList<String> processQuery(String query)
     {
       String lower = query.toLowerCase(); //Make all letters lowercase.
       String lowerNoPunc = lower.replaceAll("[^a-zA-Z0-9 ]+", ""); //Remove all punctuation from the query.
       String [] terms= lowerNoPunc.split(" "); //Split the string by whitespace so that each term is an index in an array.
       
       ArrayList<String> sigTerms = new ArrayList<String>(); //Holds significant terms from a query.
       PorterStemmer porters = new PorterStemmer(); //Object that will reduce a significant word to its root form. 
       
       for(int i = 0; i < terms.length; i++)
       {
           porters.setCurrent(terms[i]); //Feed the significant term into Porter's Stemming algorithm.
           porters.stem(); //Reduce the word to its root form.
           String root = porters.getCurrent();
           if(!stopWords.contains(terms[i]) && invertedIndex.containsKey(root)) //If the word is not a keyword...
           {   
               sigTerms.add(root); //Add the root form of the word to the significant terms array.
           }
       }
       return sigTerms; //Return the Array List containing the significant terms. 
     }
     
     private double calculateTfIdf(Postings p, int listSize)
     {
         double count = p.getCount();
         double totalWords = docIDs.get(p.getDocId()).getWordCount();
         double tf = count/totalWords; //Normalized term frequency.
         
         double docFreq = listSize; //Number of documents that contain the term.
         double corpusSize = docIDs.size();
         /*It was decided to take the log of the idf in order to balance the weight of the normalized term frequency and the inverse document frequency.
         In practice, where there are many more documents in a corpus, the log allows us to normalize values when the document frequency becomes very large
         (i.e. we can minimize the discrepancy between a doc frequency of 1 million and 2 million). However, in the case of this project, the IDF is comparatively
         much larger despite the small size of the corpus. Documents range from a few hundred to about a thousand words. Even a word that occurs in high frequency 
         will generate a comparatively smaller tf score relative to a document that is found in only 10% (20 documents) of the corpus. To treat these values as nearly equal,
         we will use the log of idf.
         */
         double idf = log10(corpusSize/docFreq);
         
         return tf*idf;
     }
     
     private void addDoc(int idNum, File doc) throws FileNotFoundException //Function will add all words in a document to the inverted index given its docID and its directory location.
     {
         int paraWordCount=1;
         int wordCount = 1;
         int paraCount = 1;
         PorterStemmer stemmer = new PorterStemmer();//Porter's Stemmer object used to convert a word to its root form.
         try{
             FileInputStream fileIn = new FileInputStream(doc);
             XWPFDocument corpra = new XWPFDocument(fileIn);
             List<XWPFParagraph> currentPar = corpra.getParagraphs();
             
             for(int i = 0; i < currentPar.size(); i++)
             {
               String words[] = currentPar.get(i).getText().split(" ");
               if(words.length>0 && !words[0].equals("")){ //Avoid processing paragraphs containing exclusively whitespace. Note that the corpus has been checked to fix all instances where a paragraph begins with the empty string and then contains non-whitespace characters.
               
               for(int j = 0; j < words.length; j++)
               {
                   String lower = words[j].toLowerCase(); //Convert all strings to lowercase to avoid capitalized words not matching to lowercase words.
                   String noPunc = lower.replaceAll("[^a-zA-Z0-9]+", ""); //Remove all punctuation to avoid having identical words with different punctuation being mismatched. Note that this concatenates words that that contain hyphens and /. Provide we are consistent and apply this regex statement to remove punctuation from queried terms the loss of hyphenated or slashed words can be minimalized on the results.
                   
                   if(!stopWords.contains(noPunc)) //If the word isn't a stop word it needs to be added to the index.
                   {
                       stemmer.setCurrent(noPunc); //Feed the non-punctuated string into Porter's Stemming Algorithm.
                       stemmer.stem(); //Reduce the word to its root form.
                       String rootWord = stemmer.getCurrent(); //Obtain the root word from Porter's Stemming algorithm.
                       //Check if in list, if so, add positions to existing list. If not, create a new postings list entry and push onto linkedlist.
                       if(!invertedIndex.containsKey(rootWord)) //Word doesn't exist in index.
                       {
                           LinkedList<Postings> newList = new LinkedList<Postings>();
                           invertedIndex.put(rootWord, newList); //Add the word to the index along with an empty postings list.
                       }
                      
                       if(!invertedIndex.get(rootWord).isEmpty() && invertedIndex.get(rootWord).getFirst().getDocId() ==idNum) //The word was already added, so we can increase the count by one by adding its statistics.
                       {
                          invertedIndex.get(rootWord).getFirst().addLocation(wordCount, paraWordCount, paraCount); //Add a new entry to the word's postings list.
                       }
                       else //Document has not yet been found in the postings list. Add a postings entry for that document.
                       {
                            ArrayList<Integer> word = new ArrayList<Integer>();
                            word.add(wordCount);
                            ArrayList<Integer> paraWord = new ArrayList<Integer>();
                            paraWord.add(paraWordCount);
                            ArrayList<Integer> paragraph = new ArrayList<Integer>();
                            paragraph.add(paraCount);
                            Postings post = new Postings(idNum, word, paraWord, paragraph);
                            invertedIndex.get(rootWord).addFirst(post);
                       }
                    }
                   docIDs.get(idNum).setStats(wordCount, paraCount); //Save the statistics gathered for the document.
                   paraWordCount++;   
                   wordCount++;
               }
             paraWordCount=1;
             paraCount++;
               } 
         }
            
     
             fileIn.close();
        }
          catch(IOException e)
         {
             System.err.println(e.toString());
             System.exit(1);
         }
     }
    
     
     private void loadStopWords(String directory) throws IOException//Function reads the list of stop words in the projects directory and loads them into the array.
     {
         /*Note that this function is contingent on stopWords.txt belonging to the same a directory as when it was zipped. 
         */
         String temp = directory.replace("Corpus","Stopwords");
         String fileName = temp + "\\stopWords.txt";
         BufferedReader input = new BufferedReader(new FileReader(fileName));
         try
         {
             String word = input.readLine(); //The file is formatted as one word per line, so we can read one line at a time.
             while(word != null)
             {
                 stopWords.add(word); //Add the word to the stopword list.
                 word = input.readLine(); //Go to the next word in the text file.
             }
             input.close(); //Closed the buffered reader.
         }
         catch(IOException e)
         {
             System.err.println(e.toString());
             System.exit(1);
         }
         
     }
    
     
     private class Postings {
        
        private int id;
        ArrayList<Integer> wordNum; //Holds the absolute position of the word (i.e. its position relative to the entire document).
        ArrayList<Integer> lineNum; //Holds the word number of word given some word given a paragraph.
        ArrayList<Integer> paraNum; //Stores the paragraph number where the term can be found.
        /*Chose to use an ArrayList here since once the keywords are mapped, we will need to quickly check locations which can be done using random lookup
        as opposed to a sequential lookup with linked lists.*/
        
        public Postings(int docID, ArrayList<Integer> wNum, ArrayList<Integer> lNum, ArrayList<Integer> par) {
            id = docID;
            wordNum = wNum;
            lineNum = lNum;
            paraNum = par;
        }
        
        public int getCount() //Function returns the number of locations that a particular word was found in a document. 
        {
            return wordNum.size();
        }
        
        public ArrayList<Integer> getPara()
        {
             return paraNum;       
        }
        
         public ArrayList<Integer> getWord()
        {
             return wordNum;       
        }
         
         public ArrayList<Integer> getLine()
        {
             return lineNum;       
        }
         
        public int[] getLocation(int pos)
        {
            int location[] = {wordNum.get(pos), lineNum.get(pos), paraNum.get(pos)};
            return location;
        }
        
        public int getDocId()
        {
            return id;
        }
        
        public void addLocation(int word, int lNum, int pNum) //Function adds a new location to the back of the ArrayList.
        {
            wordNum.add(word);
            lineNum.add(lNum);
            paraNum.add(pNum);
        }
    }
     
     private class Document{ //Class will hold document statistics associated with each docID.
         
         private int wordCount;
         private int paragraphCount;
         private String fileName;
         private String intendedQuery;
         
         public Document(String f, String q) //Parameterized constructor that will allow for generation of a document before inspecting its contents.
         {
             fileName = f;
             intendedQuery = q;
         }
         public int getWordCount()
         {
             return wordCount;
         }
         public int getParagraphCount()
         {
             return paragraphCount;
         }
         public String getFileName()
         {
             return fileName;
         }
         
         public String getQuery()
         {
             return intendedQuery;
         }
         public void setStats(int words, int para)
         {
             wordCount = words;
             paragraphCount = para;
         }
         
     }
     
     private class Score implements Comparable<Score>
     {
         private int docID;
         private double cumulativeScore;
         
         public Score(int id, double score)
         {
             docID = id;
             cumulativeScore = score;
         }
         
         @Override
         public int compareTo(Score s) //Allows us to compare scores by their calculated value when sorting our results list and filtering out results by the threshold.
         {
             if(this.cumulativeScore < s.cumulativeScore)
                 return 1;
             else if (this.cumulativeScore > s.cumulativeScore)
                 return -1;
             else
                 return 0;
         }
         public double getScore()
         {
             return cumulativeScore;
         }
         public int getDocID()
         {
             return docID;
         }
         public void setScore(double score)
         {
             cumulativeScore = score;
             
         }
         
         public void setDocID(int id)
         {
             docID = id;
         }
         
         public void updateScore(double score)
         {
             cumulativeScore +=score;
         }
     }
     
}
