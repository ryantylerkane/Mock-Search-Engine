
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class SearchEngine{

    /**
     *Use args[0] to pass the path to the "Corpus" directory as input for the program.
     */
    public static void main(String[] args) throws IOException {
       try
       {
        //Open BufferedWriter to write the results to the output file.
       FileWriter write = new FileWriter("results.txt");
       BufferedWriter out = new BufferedWriter(write);
       
       //Open a BufferedWriter to the output file so that we can display its contents to the user via the GUI.
       FileReader fileIn = new FileReader("results.txt");
       BufferedReader read = new BufferedReader(fileIn);
       
       InvertedIndex index = new InvertedIndex(); //Declare a new inverted index.
       String corpusDirectory = args[0]; //Obtain directory of corpus from command line.
       index.buildIndex(corpusDirectory);
       
           String query = "";
           while(query!=null) //Continue to process queries until the user clicks "Cancel" or "Exit" on the GUI.
           {
               //Display input window for user to enter the string.
               query = (String) JOptionPane.showInputDialog(new JFrame(), "Enter your query:", "IR Final Project - Simple Search Engine", JOptionPane.QUESTION_MESSAGE);
               if(!(query==null)) //If a query is provided, process it and write it to the file.
               {
                   index.getResults(query, out); //Score related documents and write the results to a file.
                   out.flush(); //Remove any contents that may still be in the buffer.
                   showFile(read, query); //Open a new window that displays the results of the submitted query.
               }
               else //Cancel was or "Exit" was pressed - user has no more queries.
               {
                   write.close(); //Close the output handler.
                   read.close(); //Close the initial BufferedReader so we open a new one at the top of the document.
                   BufferedReader finalRead = new BufferedReader(new FileReader("results.txt"));
                   showFile(finalRead, "All Searches"); //Open a new window that shows the entire contents of the file.
                   finalRead.close();
                   System.exit(0);
               }
           }
       }
       catch(IOException e)
       {
           System.err.println(e);
           System.exit(1);
       }
  }
    
     private static void showFile(BufferedReader read, String q) throws IOException{ //Function creates a JScrollPane that outputs the results for a single search or for all searches upon exiting.
       
        String input = "";
        String temp=null;
        
        while((temp=read.readLine()) != null) //Read each line in the between the current position and the end.
        {
            input +=temp + "\n"; //Concatenate all lines to a single string.
        }
        
        JTextArea contents = new JTextArea(input); //Create a JTextArea that will hold the concatenated string.
        JScrollPane scroll = new JScrollPane(contents); //Add the results to a scroll panel so that it is easier to view the contents.
        Dimension size = new Dimension(1000, 500);
        JFrame frame = new JFrame(); //Create new JFrame to avoid having the 
        frame.getContentPane().add(scroll);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); //Allows the program to keep running once the user clicks "OK" or Exits the results.
        scroll.setPreferredSize(size);
        JOptionPane.showMessageDialog(null, scroll,"Results for "+ q, JOptionPane.PLAIN_MESSAGE); //Display the constructed window on the screen.
}
  
}

    

